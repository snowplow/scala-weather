/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
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
class OwmAsyncClient(appId: String, transport: HttpAsyncTransport) extends Client[AsyncWeather]  {
  def receive[W <: OwmResponse: Manifest](request: OwmRequest): Future[Either[WeatherError, W]] = {
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
  def apply(
    appId: String,
    host: String = "history.openweathermap.org",
    ssl: Boolean = false
  ): OwmAsyncClient =
    new OwmAsyncClient(appId, new HttpTransport(host, ssl))

  /**
   * Create async client with key and optionally non-standard (mock) HTTP transport
   */
  def apply(appId: String, transport: HttpAsyncTransport): OwmAsyncClient =
    new OwmAsyncClient(appId, transport)
}
