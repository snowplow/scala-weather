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
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.effect.Concurrent

// LruMap
import com.snowplowanalytics.lrumap.LruMap

// Joda
import org.joda.time.DateTime

// This library
import Errors._
import Responses._
import CacheUtils.{CacheKey, Position}

/**
 * Blocking OpenWeatherMap client with history (only) cache
 * Uses AsyncOwmClient under the hood, has the same method set, but also uses timeouts
 *
 * WARNING. Caching will not work with free OWM licenses - history plan is required
 */
class OwmCacheClient[F[_]: Concurrent] private[openweather] (cache: LruMap[F, CacheKey, Either[WeatherError, History]],
                                                             val geoPrecision: Int,
                                                             transport: Transport[F])
    extends OwmClient[F](transport) {

  /**
   * Search history in cache and if not found request and await it from server
   * and put to the cache. If timeout error was taken from cache, do request again
   *
   * @param latitude event's latitude
   * @param longitude event's longitude
   * @param timestamp event's timestamp
   * @return weather stamp immediately taken from cache or requested from server
   */
  def getCachedOrRequest(latitude: Float, longitude: Float, timestamp: Int): F[Either[WeatherError, Weather]] = {
    val cacheKey = CacheUtils.eventToCacheKey(timestamp, Position(latitude, longitude), geoPrecision)
    cache.get(cacheKey).flatMap {
      case Some(Right(cached)) =>
        Concurrent[F].delay(cached.pickCloseIn(timestamp)) // Cache hit
      case Some(Left(TimeoutError(_))) =>
        getAndCache(latitude, longitude, timestamp, cacheKey)
      case Some(Left(error)) =>
        Concurrent[F].point(Left(error))
      case None =>
        getAndCache(latitude, longitude, timestamp, cacheKey)
    }
  }

  /**
   * Overloaded `getCachedOrRequest` method with Joda DateTime instead of Unix epoch timestamp
   */
  def getCachedOrRequest(latitude: Float, longitude: Float, timestamp: DateTime): F[Either[WeatherError, Weather]] = {
    val unixTime: Int = (timestamp.getMillis / 1000).toInt
    getCachedOrRequest(latitude, longitude, unixTime)
  }

  /**
   * Retry request to the server, put whole batch result to client's GLOBAL cache
   * and pick near to realTimestamp weather stamp
   *
   * @param latitude real event's latitude
   * @param longitude real event's longitude
   * @param realTimestamp real event's timestamp
   * @param cacheKey cache key to use for bucket
   * @return near weather stamp
   */
  private def getAndCache(latitude: Float,
                          longitude: Float,
                          realTimestamp: Timestamp,
                          cacheKey: CacheKey): F[Either[WeatherError, Weather]] =
    historyByCoords(latitude, longitude, cacheKey.day, cacheKey.endOfDay, 24)
      .flatTap(response => cache.put(cacheKey, response))
      .map(response => getWeatherStamp(response, realTimestamp))

  /**
   * Get exact weather stamp from history batch-request (probably failed)
   * nearest to specified `timestamp`
   *
   * @param history history request with 0 or more weather stamps
   * @param timestamp timestamp
   * @return either error or weather stamp
   */
  private[openweather] def getWeatherStamp(history: Either[WeatherError, History],
                                           timestamp: Int): Either[WeatherError, Weather] =
    history.right.flatMap(_.pickCloseIn(timestamp))

}
