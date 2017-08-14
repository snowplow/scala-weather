/*
 * Copyright (c) 2015-2017 Snowplow Analytics Ltd. All rights reserved.
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

// Scala
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

// json4s
import org.json4s.JValue
import org.json4s.jackson.JsonMethods.parseOpt

// scalaj
import scalaj.http._

// This library
import Errors._
import Requests.WeatherRequest

/**
 * Scalaj based transport
 * @param apiHost weather API server host
 */
class HttpTransport(apiHost: String, ssl: Boolean = false) extends HttpAsyncTransport {

  def getData(request: WeatherRequest, appId: String): Future[Either[WeatherError, JValue]] =
    Future {
      val uri = request.constructQuery(appId)
      val scheme = if (ssl) "https://" else "http://"
      val url = scheme + apiHost + uri
      Http(url).asString
    }.map(processHttpResponse)

  /**
   * Get JSON out of HTTP response body
   *
   * @param response full HTTP response
   * @return future with either server error or JSON
   */
  private def processHttpResponse(response: HttpResponse[String]): Either[WeatherError, JValue] =
    getResponseContent(response).right.flatMap(parseJson)

  /**
   * Wait for entity and convert it to string
   *
   * @param response full HTTP response
   * @return future entity content of HTTP response or throwable in case of timeout
   */
  private def getResponseContent(response: HttpResponse[String]): Either[WeatherError, String] =
    if (response.isSuccess) {
      Right(response.body)
    } else {
      if (response.code == 401) Left(AuthorizationError)
      else Left(HTTPError(s"Request failed with status ${response.code}"))
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
      case None => Left(ParseError(s"OpenWeatherMap Error: string [$content] doesn't contain JSON"))
    }
}
