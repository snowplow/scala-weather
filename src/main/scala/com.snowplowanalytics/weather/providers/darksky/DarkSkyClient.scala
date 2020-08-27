/*
 * Copyright (c) 2015-2019 Snowplow Analytics Ltd. All rights reserved.
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
package providers.darksky

import java.time.ZonedDateTime

import scala.concurrent.duration._

import errors.WeatherError
import responses.DarkSkyResponse
import requests.DarkSkyRequest

/**
  * Non-caching Dark Sky client
  * Allows forecast, current, and historical data requests
  * This API does not support Geocoding - coordinates only
  * For more detailed description go to: https://darksky.net/dev/docs
  * @param apiHost address of the API to interrogate
  * @param apiKey credentials to interrogate the API
  * @param timeout duration after which the request will be timed out
  * @param ssl whether to use https or http
  * @tparam F effect type
  */
class DarkSkyClient[F[_]] private[darksky] (
  apiHost: String,
  apiKey: String,
  timeout: FiniteDuration,
  ssl: Boolean
)(implicit T: Transport[F]) {

  /** Forecast request that returns the current weather condition,
    * a minute-by-minute forecast (where available), an hour-by-hour forecast
    * for the next 48 hours, and a day-by-day forecast for the next week
    *
    * @param latitude The latitude of a location. Positive is north, negative is south.
    * @param longitude The longitude of a location. Positive is east, negative is west.
    * @param exclude blocks to exclude from the response
    * @param extend if true, `hourly` will be extended to 168 hours instead of 48
    * @param lang returns `summary` field in the specified language (use codes like `de`, `ru`)
    *            full list at Dark Sky docs
    * @param units return weather conditions in the specified units
    * @return either error or response, wrapped in effect type `F`
    */
  def forecast(
    latitude: Float,
    longitude: Float,
    exclude: List[BlockType] = List.empty[BlockType],
    extend: Boolean = false,
    lang: Option[String] = None,
    units: Option[Units] = None
  ): F[Either[WeatherError, DarkSkyResponse]] = {
    val request = DarkSkyRequest(latitude, longitude, None, exclude, extend, lang, units)
    T.receive(request, apiHost, apiKey, timeout, ssl)
  }

  /** "Time Machine Request" - returns the observed (in the past) or forecasted (in the future)
    * hour-by-hour weather and daily weather conditions for the provided date
    *
    * @param latitude The latitude of a location. Positive is north, negative is south
    * @param longitude The longitude of a location. Positive is east, negative is west
    * @param dateTime Datetime that will be converted to the Unix timestamp for the actual request
    * @param exclude blocks to exclude from the response
    * @param extend if true, `hourly` will be extended to 168 hours instead of 48
    * @param lang returns `summary` field in the specified language (use codes like `de`, `ru`)
    *            full list at Dark Sky docs
    * @param units return weather conditions in the specified units
    * @return either error or response, wrapped in effect type `F`
    */
  def timeMachine(
    latitude: Float,
    longitude: Float,
    dateTime: ZonedDateTime,
    exclude: List[BlockType] = List.empty[BlockType],
    extend: Boolean = false,
    lang: Option[String] = None,
    units: Option[Units] = None
  ): F[Either[WeatherError, DarkSkyResponse]] = {
    val request = DarkSkyRequest(latitude, longitude, Some(dateTime.toEpochSecond), exclude, extend, lang, units)
    T.receive(request, apiHost, apiKey, timeout, ssl)
  }
}
