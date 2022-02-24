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
package providers.darksky

import java.time.ZonedDateTime

import scala.concurrent.duration._

import cats.Monad

import responses.DarkSkyResponse
import errors.WeatherError

class DarkSkyCacheClient[F[_]] private[darksky] (
  cache: Cache[F, DarkSkyResponse],
  apiHost: String,
  apiKey: String,
  timeout: FiniteDuration,
  ssl: Boolean
)(implicit T: Transport[F])
    extends DarkSkyClient[F](apiHost, apiKey, timeout, ssl)(T) {

  /** nth part of 1 to which latitude and longitude will be rounded */
  val geoPrecision: Int = cache.geoPrecision

  /**
    * Gets DarkSkyResponse for the specified date (alignes time to midnight) from the cache,
    * if not found, then performs the request
    * == IMPORTANT ==
    * Returned DarkSkyResponse's `currently` field excluded, because the time is always aligned to midnight
    * @param latitude The latitude of a location. Positive is north, negative is south.
    * @param longitude The longitude of a location. Positive is east, negative is west.
    * @param dateTime zoned datetime, time is aligned to midnight
    * @return either weather error or dark sky response, wrapper in effect type F
    */
  def cachingTimeMachine(
    latitude: Float,
    longitude: Float,
    dateTime: ZonedDateTime,
    exclude: List[BlockType] = List.empty[BlockType],
    extend: Boolean = false,
    lang: Option[String] = None,
    units: Option[Units] = None
  )(implicit M: Monad[F]): F[Either[WeatherError, DarkSkyResponse]] =
    cache.getCachedOrRequest(latitude, longitude, dateTime)(doRequest(exclude, extend, lang, units))

  private def doRequest(
    exclude: List[BlockType],
    extend: Boolean,
    lang: Option[String],
    units: Option[Units]
  )(
    latitude: Float,
    longitude: Float,
    dateTime: ZonedDateTime
  ): F[Either[WeatherError, DarkSkyResponse]] =
    timeMachine(latitude, longitude, dateTime, (List(BlockType.currently) ++ exclude).distinct, extend, lang, units)
}
