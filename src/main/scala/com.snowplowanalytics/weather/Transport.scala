/*
 * Copyright (c) 2015-2019 Snowplow Analytics Ltd. All rights reserved.
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
import cats.effect.{IO, Sync}
import cats.syntax.either._
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
   * @param request request built by client method
   * @param apiHost address of the API to interrogate
   * @param apiKey credentials to interrogate the API
   * @param requestTimeout duration after which the request will be timed out
   * @param ssl whether to use https or http
   * @tparam W type of weather response to extract
   * @return extracted either error or weather wrapped in `F`
   */
  def receive[W <: WeatherResponse: Decoder](
    request: WeatherRequest,
    apiHost: String,
    apiKey: String,
    requestTimeout: FiniteDuration,
    ssl: Boolean
  ): F[Either[WeatherError, W]]
}

object Transport {

  /** Http Transport leveraging cats-effect's Sync. */
  implicit def syncTransport[F[_]: Sync]: Transport[F] = new Transport[F] {
    implicit val interpreter = Interpreter[F]

    def receive[W <: WeatherResponse: Decoder](
      request: WeatherRequest,
      apiHost: String,
      apiKey: String,
      requestTimeout: FiniteDuration,
      ssl: Boolean
    ): F[Either[WeatherError, W]] =
      buildRequest(apiHost, apiKey, ssl, request).exec[F]
  }

  /** Eval http Transport in cases where you have to do side-effects (e.g. spark). */
  implicit def evalTransport: Transport[Eval] = new Transport[Eval] {
    implicit val interpreter = Interpreter[IO]

    def receive[W <: WeatherResponse: Decoder](
      request: WeatherRequest,
      apiHost: String,
      apiKey: String,
      requestTimeout: FiniteDuration,
      ssl: Boolean
    ): Eval[Either[WeatherError, W]] = Eval.later {
      buildRequest(apiHost, apiKey, ssl, request).exec[IO].unsafeRunSync()
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
