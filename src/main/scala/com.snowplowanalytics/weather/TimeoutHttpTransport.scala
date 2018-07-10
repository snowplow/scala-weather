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

// Scala
import scala.concurrent.ExecutionContext

// cats
import cats.effect.{Concurrent, Timer}
import cats.syntax.functor._

// circe
import io.circe.Decoder

// This library
import Errors.{TimeoutError, WeatherError}

import scala.concurrent.duration.FiniteDuration

class TimeoutHttpTransport[F[_]: Concurrent](apiHost: String,
                                             apiKey: String,
                                             requestTimeout: FiniteDuration,
                                             ssl: Boolean = true)(implicit val executionContext: ExecutionContext)
    extends HttpTransport[F](apiHost, apiKey, ssl) {

  import TimeoutHttpTransport._

  private val timer: Timer[F] = Timer.derive[F]

  override def receive[W <: WeatherResponse: Decoder](request: WeatherRequest): F[Either[Errors.WeatherError, W]] =
    timeout(super.receive(request), requestTimeout, timer)

}

object TimeoutHttpTransport {

  /**
   * Apply timeout to the `operation` parameter. To be replaced by Concurrent[F].timeout in cats-effect 1.0.0
   *
   * @param operation The operation we want to run with a timeout
   * @param duration Duration to timeout after
   * @return either Left(TimeoutError) or a result of the operation, wrapped in F
   */
  private def timeout[F[_]: Concurrent, W](operation: F[Either[WeatherError, W]],
                                           duration: FiniteDuration,
                                           timer: Timer[F]): F[Either[WeatherError, W]] =
    Concurrent[F]
      .race(operation, timer.sleep(duration))
      .map {
        case Left(value) => value
        case Right(_)    => Left(TimeoutError(s"OpenWeatherMap request timed out after ${duration.toSeconds} seconds"))
      }
}
