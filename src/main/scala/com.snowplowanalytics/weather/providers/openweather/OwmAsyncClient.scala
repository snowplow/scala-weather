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
import cats._

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

  def receive[W <: OwmResponse](request: OwmRequest)(implicit wDecoder: Decoder[W]): F[Either[WeatherError, W]] = {

    val uri    = request.constructQuery(appId)
    val scheme = if (ssl) "https://" else "http://"
    val url    = scheme + apiHost + uri

    Uri
      .fromString(url)
      .leftMap(InternalError)
      .traverse { uri: Uri =>
        Hammock
          .request(Method.GET, uri, Map())
          .map(response => processResponse(response))
          .exec[F]
      }
      .map(x => x.joinRight)
  }

  /**
   * Get JSON out of HTTP response body
   *
   * @param response full HTTP response
   * @return either server error or JSON
   */
  private def processResponse[A: Decoder](response: HttpResponse): Either[WeatherError, A] =
    getResponseContent(response)
      .flatMap(parseJson)
      .flatMap(json => extractWeather(json))

  /**
   * Convert the response to string
   *
   * @param response full HTTP response
   * @return either entity content of HTTP response or WeatherError
   */
  private def getResponseContent(response: HttpResponse): Either[WeatherError, String] =
    response.status match {
      case Status.OK           => Right(response.entity.content.toString)
      case Status.Unauthorized => Left(AuthorizationError)
      case _                   => Left(HTTPError(s"Request failed with status ${response.status.code}"))
    }

  /**
   * Try to parse JSON
   *
   * @param content string containing JSON
   * @return either WeatherError or expected value
   */
  private def parseJson(content: String): Either[WeatherError, Json] =
    parse(content)
      .leftMap(e =>
        ParseError(
          s"OpenWeatherMap Error when trying to parse following json: \n$content\n\nMessage from the parser:\n ${e.message}"))

}
