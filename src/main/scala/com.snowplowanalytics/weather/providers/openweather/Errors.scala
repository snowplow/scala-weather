/*
 * Copyright (c) 2012-2016 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.weather.providers.openweather

object Errors {
  /**
   * Superclass for non-fatal exceptions that can be happen for weather fetching/processing
   */
  sealed trait WeatherError {
    val message: String
    override def toString = s"OpenWeatherMap ${this.getClass.getSimpleName} " + message
  }

  /**
   * Connecting/receiving/etc timeout errors
   */
  case class TimeoutError(message: String) extends java.util.concurrent.TimeoutException(message) with WeatherError

  /**
   * Invalid state/argument/response
   */
  case class InternalError(message: String) extends WeatherError

  /**
   * Response parsing error
   */
  case class ParseError(message: String) extends WeatherError

  /**
   * Common Auth error
   * We could also use [[ErrorResponse]], but for some unauth cases OWM returns HTML page
   */
  case object AuthorizationError extends WeatherError {
    val message = "Check your API key"
  }

  /**
   * Error returned from weather provider, which can be extracted from JSON
   */
  case class ErrorResponse(cod: Option[String], message: String) extends WeatherError
}
