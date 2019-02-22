/*
 * Copyright (c) 2015-2018 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.weather

import scala.concurrent.duration.FiniteDuration

import cats.Eval
import cats.free.Free
import cats.effect.{Concurrent, ContextShift, IO, Sync, Timer}
import cats.syntax.either._
import cats.syntax.functor._
import hammock._
import hammock.jvm.Interpreter
import io.circe.{Decoder, Json}
import io.circe.parser.parse

import errors._
import model._

trait Transport[F[_]] {

  /**
   * Main client logic for Request => Response function,
   * where Response is wrapped in tparam `F`
   *
   * @param request request built by client method
   * @tparam W type of weather response to extract
   * @return extracted either error or weather wrapped in `F`
   */
  def receive[W <: WeatherResponse: Decoder](request: WeatherRequest): F[Either[WeatherError, W]]

}

object Transport {

  /**
   * Http Transport leveraging cats-effect's Sync.
   * @param apiHost address of the API to interrogate
   * @param apiKey credentials to interrogate the API
   * @param ssl whether to use https or http
   * @return a Sync Transport
   */
  def httpTransport[F[_]: Sync](
    apiHost: String,
    apiKey: String,
    ssl: Boolean = true
  ): Transport[F] = new Transport[F] {

    implicit val interpreter = Interpreter[F]

    def receive[W <: WeatherResponse: Decoder](
      request: WeatherRequest
    ): F[Either[WeatherError, W]] =
      buildRequest(apiHost, apiKey, ssl, request)
        .exec[F]
  }

  /**
   * Unsafe http Transport to use in cases where you have to do side-effects (e.g. spark or beam).
   * @param apiHost address of the API to interrogate
   * @param apiKey credentials to interrogate the API
   * @param ssl whether to use https or http
   * @return an Eval Transport
   */
  def unsafeHttpTransport(
    apiHost: String,
    apiKey: String,
    ssl: Boolean = true
  ): Transport[Eval] = new Transport[Eval] {

    implicit val interpreter = Interpreter[IO]

    def receive[W <: WeatherResponse: Decoder](
      request: WeatherRequest
    ): Eval[Either[WeatherError, W]] =
      Eval.later {
        buildRequest(apiHost, apiKey, ssl, request)
          .exec[IO]
          .unsafeRunSync()
      }
  }

  /**
   * Http Transport which is able to timeout requests.
   * @param apiHost address of the API to interrogate
   * @param apiKey credentials to interrogate the API
   * @param requestTimeout duration after which the request will be timed out
   * @param ssl whether to use https or http
   * @return a Transport with Concurrent and Timer constraints
   */
  def timeoutHttpTransport[F[_]: Concurrent: Timer](
    apiHost: String,
    apiKey: String,
    requestTimeout: FiniteDuration,
    ssl: Boolean = true
  ): Transport[F] = new Transport[F] {

    implicit val interpreter = Interpreter[F]

    def receive[W <: WeatherResponse: Decoder](
      request: WeatherRequest
    ): F[Either[WeatherError, W]] = {
      val operation = buildRequest(apiHost, apiKey, ssl, request).exec[F]
      timeout(operation, requestTimeout)
    }
  }

  /**
   * Unsafe http Transport which is able to timeout requests.
   * @param apiHost address of the API to interrogate
   * @param apiKey credentials to interrogate the API
   * @param requestTimeout duration after which the request will be timed out
   * @param ssl whether to use https or http
   * @return an Eval Transport
   */
  def unsafeTimeoutHttpTransport(
    apiHost: String,
    apiKey: String,
    requestTimeout: FiniteDuration,
    ssl: Boolean = true
  ): Transport[Eval] = new Transport[Eval] {

    implicit val interpreter = Interpreter[IO]
    implicit val timer       = IO.timer(scala.concurrent.ExecutionContext.Implicits.global)
    implicit val cs: ContextShift[IO] =
      IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)

    def receive[W <: WeatherResponse: Decoder](
      request: WeatherRequest
    ): Eval[Either[WeatherError, W]] = Eval.later {
      val operation = buildRequest(apiHost, apiKey, ssl, request).exec[IO]
      timeout(operation, requestTimeout).unsafeRunSync()
    }
  }

  private def buildRequest[W <: WeatherResponse: Decoder](
    apiHost: String,
    apiKey: String,
    ssl: Boolean,
    request: WeatherRequest
  ): Free[HttpF, Either[WeatherError, W]] = {
    val scheme    = if (ssl) "https" else "http"
    val authority = Uri.Authority(None, Uri.Host.Other(apiHost), None)
    val baseUri   = Uri(Some(scheme), Some(authority))

    val uri = request.constructQuery(baseUri, apiKey)

    Hammock
      .request(Method.GET, uri, Map())
      .map(uri => processResponse(uri))
  }

  /**
   * Apply timeout to the `operation` parameter. To be replaced by Concurrent[F].timeout in cats-effect 1.0.0
   *
   * @param operation The operation we want to run with a timeout
   * @param duration Duration to timeout after
   * @return either Left(TimeoutError) or a result of the operation, wrapped in F
   */
  private def timeout[F[_]: Concurrent: Timer, W](
    operation: F[Either[WeatherError, W]],
    duration: FiniteDuration,
  ): F[Either[WeatherError, W]] =
    Concurrent[F]
      .race(operation, Timer[F].sleep(duration))
      .map {
        case Left(value) => value
        case Right(_)    => Left(TimeoutError(s"OpenWeatherMap request timed out after ${duration.toSeconds} seconds"))
      }

  /**
   * Decode response case class from HttpResponse body
   *
   * @param response full HTTP response
   * @return either error or decoded case class
   */
  private def processResponse[A: Decoder](response: HttpResponse): Either[WeatherError, A] =
    getResponseContent(response)
      .flatMap(parseJson)
      .flatMap(json => extractWeather(json))

  /**
   * Convert the response to string
   *
   * @param response full HTTP response
   * @return either entity content of HTTP response or WeatherError (AuthorizationError / HTTPError)
   */
  private def getResponseContent(response: HttpResponse): Either[WeatherError, String] =
    response.status match {
      case Status.OK => Right(response.entity.content.toString)
      case Status.Unauthorized | Status.Forbidden => Left(AuthorizationError)
      case _ => Left(HTTPError(s"Request failed with status ${response.status.code}"))
    }

  private def parseJson(content: String): Either[ParseError, Json] =
    parse(content)
      .leftMap(e =>
        ParseError(
          s"OpenWeatherMap Error when trying to parse following json: \n$content\n\nMessage from the parser:\n ${e.message}"))

  /**
   * Transform JSON into parseable format and try to extract specified response
   *
   * @param response response json
   * @tparam W provider-specific response case class
   * @return either weather error or response case class
   */
  private[weather] def extractWeather[W: Decoder](response: Json): Either[WeatherError, W] =
    response.as[W].leftFlatMap { _ =>
      response.as[ErrorResponse] match {
        case Right(error) => Left(error)
        case Left(_)      => Left(ParseError(s"Could not extract ${Decoder[W].toString} from ${response.toString}"))
      }
    }
}
