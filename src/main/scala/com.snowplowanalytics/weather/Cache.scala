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

// Java
import java.time.{LocalDate, ZoneOffset, ZonedDateTime}

// cats
import cats.effect.Sync
import cats.syntax.flatMap._

// LruMap
import com.snowplowanalytics.lrumap.LruMap

// This library
import Errors.{TimeoutError, WeatherError}

class Cache[F[_]: Sync, W <: WeatherResponse] private[weather] (
  cache: LruMap[F, Cache.CacheKey, Either[WeatherError, W]],
  val geoPrecision: Int) {

  import Cache._

  def getCachedOrRequest(latitude: Float, longitude: Float, dateTime: ZonedDateTime)(
    f: (Float, Float, ZonedDateTime) => F[Either[WeatherError, W]]): F[Either[WeatherError, W]] = {

    val cacheKey = eventToCacheKey(dateTime, Position(latitude, longitude), geoPrecision)
    cache.get(cacheKey).flatMap {
      case Some(Right(cached)) =>
        Sync[F].delay(Right(cached)) // Cache hit
      case Some(Left(TimeoutError(_))) =>
        f(latitude, longitude, dateTime)
          .flatTap(cache.put(cacheKey, _))
      case Some(Left(error)) =>
        Sync[F].point(Left(error))
      case None =>
        f(latitude, longitude, dateTime)
          .flatTap(cache.put(cacheKey, _))
    }

  }

}

object Cache {

  /**
   * Cache key for obtaining record
   *
   * @param date timestamp for 0:00:00
   * @param center rounded geo coordinates
   */
  final case class CacheKey(date: LocalDate, center: Position)

  def dayStartEpoch(date: LocalDate): Timestamp = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC)
  def dayEndEpoch(date: LocalDate): Timestamp   = dayStartEpoch(date.plusDays(1))

  /**
   * Class to represent geographical coordinates
   *
   * @param latitude place's latitude
   * @param longitude places's longitude
   */
  final case class Position(latitude: Float, longitude: Float)

  /**
   * Round position and timestamp (event) to produce cache key
   *
   * @param dateTime zoned datetime
   * @param position latitude & longitude
   * @param geoPrecision nth part of 1 to which latitude and longitude will be rounded
   * @return cache key
   */
  def eventToCacheKey(dateTime: ZonedDateTime, position: Position, geoPrecision: Int): CacheKey = {
    val roundPosition =
      Position(roundCoordinate(position.latitude, geoPrecision), roundCoordinate(position.longitude, geoPrecision))
    CacheKey(dateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDate, roundPosition)
  }

  /**
   * Round coordinate by `geoPrecision`
   * Scale value to tenths to prevent values to be long like 1.333334
   *
   * @param coordinate latitude or longitude
   * @return rounded coordinate
   */
  def roundCoordinate(coordinate: Float, geoPrecision: Int): Float =
    BigDecimal
      .decimal(Math.round(coordinate * geoPrecision) / geoPrecision.toFloat)
      .setScale(1, BigDecimal.RoundingMode.HALF_UP)
      .toFloat
}
