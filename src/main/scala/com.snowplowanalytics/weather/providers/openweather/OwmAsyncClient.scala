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
import cats._
import cats.implicits._

// This library
import Errors.WeatherError
import Responses._
import Requests._

/**
 * Asynchronous OpenWeatherMap client
 *
 * @param appId API key
 * @param transport HTTP client for send requests, receive responses
 */
class OwmAsyncClient[F[_]: FlatMap](appId: String, transport: HttpAsyncTransport[F]) extends Client[F] {
  def receive[W <: OwmResponse: Manifest](request: OwmRequest): F[Either[WeatherError, W]] = {
    val processedResponse = transport.getData(request, appId)
    processedResponse.map(_.right.flatMap(extractWeather[W]))
  }
}

/**
 * Companion object for async client
 */
object OwmAsyncClient {

  /**
   * Create async client with key and optionally different API host
   */
  def apply[F[_]: Sync](
    appId: String,
    host: String = "history.openweathermap.org",
    ssl: Boolean = false
  ): OwmAsyncClient[F] =
    new OwmAsyncClient[F](appId, new HttpTransport[F](host, ssl))

  /**
   * Create async client with key and optionally non-standard (mock) HTTP transport
   */
  def apply[F[_]: Sync](appId: String, transport: HttpAsyncTransport[F]): OwmAsyncClient[F] =
    new OwmAsyncClient[F](appId, transport)
}
