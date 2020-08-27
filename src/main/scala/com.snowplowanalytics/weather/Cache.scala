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

import java.time.{LocalDate, ZoneOffset, ZonedDateTime}

import cats.{Functor, Monad}
import cats.syntax.functor._
import cats.syntax.flatMap._
import com.snowplowanalytics.lrumap.{CreateLruMap, LruMap}

import errors.{TimeoutError, WeatherError}
import model.WeatherResponse

class Cache[F[_], W <: WeatherResponse] private (
  cache: LruMap[F, Cache.CacheKey, Either[WeatherError, W]],
  val geoPrecision: Int
) {

  import Cache._

  /**
    * Gets the response from cache; if not found, then calls the provided function `doRequest`,
    * returns its result and put it in the cache
    * @param latitude latitude of the event
    * @param longitude longitude of the event
    * @param dateTime datetime with zone
    * @param doRequest function which will be called after a cache miss
    * @return value stored in the cache or the result of the provided function
    */
  def getCachedOrRequest(latitude: Float, longitude: Float, dateTime: ZonedDateTime)(
    doRequest: (Float, Float, ZonedDateTime) => F[Either[WeatherError, W]]
  )(implicit
    M: Monad[F]
  ): F[Either[WeatherError, W]] = {
    val cacheKey = eventToCacheKey(dateTime, Position(latitude, longitude), geoPrecision)
    cache.get(cacheKey).flatMap {
      case Some(Right(cached)) =>
        M.pure(Right(cached)) // Cache hit
      case Some(Left(TimeoutError(_))) =>
        doRequest(latitude, longitude, dateTime).flatTap(cache.put(cacheKey, _))
      case Some(Left(error)) =>
        M.pure(Left(error))
      case None =>
        doRequest(latitude, longitude, dateTime).flatTap(cache.put(cacheKey, _))
    }
  }
}

object Cache {

  /**
    * Factory method to create a cache.
    * @param size resulting cache's size
    * @param geoPrecision nth part of 1 to which latitude and longitude will be rounded when making
    * requests to the cache
    * @return a cache of WeatherResponse wrapped in a F
    */
  def init[F[_]: Functor, W <: WeatherResponse](
    size: Int,
    geoPrecision: Int
  )(implicit CLM: CreateLruMap[F, CacheKey, Either[WeatherError, W]]): F[Cache[F, W]] =
    for {
      lru <- CreateLruMap[F, CacheKey, Either[WeatherError, W]].create(size)
      cache = new Cache[F, W](lru, geoPrecision)
    } yield cache

  /**
    * Cache key for obtaining record
    *
    * @param date local date for UTC
    * @param center rounded geo coordinates
    */
  final case class CacheKey(date: LocalDate, center: Position)

  /** @param date local date for UTC
    * @return unix timestamp of the provided day's start
    */
  def dayStartEpoch(date: LocalDate): Timestamp = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC)

  /** @param date local date for UTC
    * @return unix timestamp of the provided day's end
    */
  def dayEndEpoch(date: LocalDate): Timestamp = dayStartEpoch(date.plusDays(1))

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
    val lat           = roundCoordinate(position.latitude, geoPrecision)
    val lng           = roundCoordinate(position.longitude, geoPrecision)
    val roundPosition = Position(lat, lng)
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
