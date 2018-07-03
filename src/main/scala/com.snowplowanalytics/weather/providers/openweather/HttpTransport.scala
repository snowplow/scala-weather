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

// json4s
import org.json4s.JValue
import org.json4s.jackson.JsonMethods.parseOpt

// hammock
import hammock._
import hammock.jvm.Interpreter

// This library
import Errors._
import Requests.WeatherRequest

/**
 * Hammock based transport
 * @param apiHost weather API server host
 */
class HttpTransport[F[_]: Sync](apiHost: String, ssl: Boolean = false) extends HttpAsyncTransport[F] {

  private implicit val interpreter = Interpreter[F]

  /**
   * Fetch data from the API
   *
   * @param request helper for generating correct URI
   * @param appId API key
   * @return either error or ready-to-process JSON, wrapped in effect type
   */
  def getData(request: WeatherRequest, appId: String): F[Either[WeatherError, JValue]] = {
    val uri    = request.constructQuery(appId)
    val scheme = if (ssl) "https://" else "http://"
    val url    = scheme + apiHost + uri

    Uri
      .fromString(url)
      .leftMap(InternalError)
      .traverse { uri: Uri =>
        Hammock
          .request(Method.GET, uri, Map())
          .map(processHttpResponse)
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
  private def processHttpResponse(response: HttpResponse): Either[WeatherError, JValue] =
    getResponseContent(response).right.flatMap(parseJson)

  /**
   * Wait for entity and convert it to string
   *
   * @param response full HTTP response
   * @return either entity content of HTTP response or throwable in case of timeout
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
   * @return disjunction of throwable and JValue
   */
  private def parseJson(content: String): Either[WeatherError, JValue] =
    parseOpt(content) match {
      case Some(json) => Right(json)
      case None       => Left(ParseError(s"OpenWeatherMap Error: string [$content] doesn't contain JSON"))
    }
}
