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
package providers.openweather

import java.time.{Instant, ZoneOffset, ZonedDateTime}

import cats.Monad
import cats.syntax.functor._

import errors._
import responses._

/**
 * Blocking OpenWeatherMap client with history (only) cache
 * Extends `OwmClient`, but uses timeouts for requests
 *
 * WARNING. Caching will not work with free OWM licenses - history plan is required
 */
class OwmCacheClient[F[_]] private[openweather] (
  cache: Cache[F, History],
  transport: Transport[F]
) extends OwmClient[F](transport) {

  /** nth part of 1 to which latitude and longitude will be rounded */
  val geoPrecision: Int = cache.geoPrecision

  /**
   * Search history in cache and if not found request and await it from server
   * and put to the cache. If timeout error was taken from cache, do request again
   *
   * @param latitude event's latitude
   * @param longitude event's longitude
   * @param timestamp event's timestamp
   * @return weather stamp immediately taken from cache or requested from server
   */
  def cachingHistoryByCoords(
    latitude: Float,
    longitude: Float,
    timestamp: Timestamp
  )(implicit M: Monad[F]): F[Either[WeatherError, Weather]] =
    cachingHistoryByCoords(latitude,
                           longitude,
                           ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC))

  /**
   * Overloaded `cachingHistoryByCoords` method with `ZonedDateTime` instead of Unix epoch timestamp
   */
  def cachingHistoryByCoords(
    latitude: Float,
    longitude: Float,
    dateTime: ZonedDateTime
  )(implicit M: Monad[F]): F[Either[WeatherError, Weather]] =
    cache
      .getCachedOrRequest(latitude, longitude, dateTime)(doRequest)
      .map(historyResult => getWeatherStamp(historyResult, dateTime))

  /**
   * Retry request to the server, put whole batch result to client's cache
   * and pick near to `timestamp` weather stamp
   *
   * @param latitude real event's latitude
   * @param longitude real event's longitude
   * @param dateTime real event's zoned datetime
   * @return near weather stamp
   */
  private def doRequest(
    latitude: Float,
    longitude: Float,
    dateTime: ZonedDateTime
  ): F[Either[WeatherError, History]] =
    historyByCoords(
      latitude,
      longitude,
      Cache.dayStartEpoch(dateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDate),
      Cache.dayEndEpoch(dateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDate),
      24
    )

  /**
   * Get exact weather stamp from history batch-request (probably failed)
   * nearest to specified `timestamp`
   *
   * @param history history request with 0 or more weather stamps
   * @param dateTime zoned date time
   * @return either error or weather stamp
   */
  private[openweather] def getWeatherStamp(
    history: Either[WeatherError, History],
    dateTime: ZonedDateTime
  ): Either[WeatherError, Weather] =
    history.right.flatMap(_.pickCloseIn(dateTime))

}
