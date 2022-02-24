/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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

import io.circe.generic.JsonCodec

object errors {

  /**
    * Superclass for exceptions that can happen for weather fetching/processing
    */
  sealed abstract class WeatherError(message: String) extends Exception(message)

  /**
    * Connecting/receiving/etc timeout errors
    */
  final case class TimeoutError(message: String) extends WeatherError(message)

  /**
    * Invalid state/argument/response
    */
  final case class InternalError(message: String) extends WeatherError(message)

  /**
    * Response parsing error
    */
  final case class ParseError(message: String) extends WeatherError(message)

  /**
    * Common Auth error
    * We could also use [[ErrorResponse]], but for some unauth cases OWM returns HTML page
    */
  final case object AuthorizationError extends WeatherError("Check your API key")

  /**
    * Error returned from weather provider, which can be extracted from JSON
    */
  @JsonCodec final case class ErrorResponse(cod: Option[String], message: String) extends WeatherError(message)

  /**
    * Error linked to http but not linked to auth
    */
  final case class HTTPError(message: String) extends WeatherError(message)

  /**
    * Error thrown when any argument provided to the clients is invalid
    */
  final case class InvalidConfigurationError(message: String) extends WeatherError(message)
}
