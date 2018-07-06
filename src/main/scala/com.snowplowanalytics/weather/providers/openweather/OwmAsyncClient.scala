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
package providers.openweather

// cats
import cats.effect.Sync
import cats.implicits._

// circe
import io.circe.parser.parse
import io.circe.{Decoder, Json}

// hammock
import hammock.{Hammock, HttpResponse, Method, Status, Uri}
import hammock.jvm.Interpreter

// This library
import Errors._
import Responses._
import Requests._

/**
 * Asynchronous OpenWeatherMap client
 *
 * @param appId API key
 */
case class OwmAsyncClient[F[_]: Sync](appId: String, apiHost: String = "api.openweathermap.org", ssl: Boolean = false)
    extends Client[F] {

  private implicit val interpreter = Interpreter[F]

  def receive[W <: OwmResponse: Decoder](request: OwmRequest): F[Either[WeatherError, W]] = {

    val scheme    = if (ssl) "https" else "http"
    val authority = Uri.Authority(None, Uri.Host.Other(apiHost), None)
    val baseUri   = Uri(Some(scheme), Some(authority))

    val uri = request.constructQuery(baseUri, appId)

    Hammock
      .request(Method.GET, uri, Map())
      .map(uri => processResponse(uri))
      .exec[F]
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
      case Status.OK           => Right(response.entity.content.toString)
      case Status.Unauthorized => Left(AuthorizationError)
      case _                   => Left(HTTPError(s"Request failed with status ${response.status.code}"))
    }

  private def parseJson(content: String): Either[ParseError, Json] =
    parse(content)
      .leftMap(e =>
        ParseError(
          s"OpenWeatherMap Error when trying to parse following json: \n$content\n\nMessage from the parser:\n ${e.message}"))

}
