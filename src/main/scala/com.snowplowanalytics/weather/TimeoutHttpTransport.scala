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

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

import cats.effect.{Concurrent, Timer}
import cats.syntax.functor._
import io.circe.Decoder

import errors.{TimeoutError, WeatherError}
import model._

class TimeoutHttpTransport[F[_]: Concurrent: Timer](
  apiHost: String,
  apiKey: String,
  requestTimeout: FiniteDuration,
  ssl: Boolean = true
)(implicit val executionContext: ExecutionContext)
    extends HttpTransport[F](apiHost, apiKey, ssl) {

  import TimeoutHttpTransport._

  override def receive[W <: WeatherResponse: Decoder](
    request: WeatherRequest
  ): F[Either[WeatherError, W]] =
    timeout(super.receive(request), requestTimeout)

}

object TimeoutHttpTransport {

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
}
