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

// Java
import java.util.Date
import java.util.Calendar

// Scalaz
import scalaz.\/

// LRUCache
import com.twitter.util.SynchronizedLruMap

// This libraby
import Responses.OwmResponse

/**
 * Defines logic for store records (LRU) and obtaining bucket's key (day, place)
 *
 * @tparam W exact type of weather records to store.
 *           However now it could be used only only for History lookups
 */
trait WeatherCache[W <: OwmResponse] {

  val cacheSize: Int                 // Size of LRU cache
  val geoPrecision: Int              // nth part of one to round geo coordinates

  type Cache = SynchronizedLruMap[CacheKey, WeatherError \/ W]
  protected val cache: Cache = new SynchronizedLruMap[CacheKey, WeatherError \/ W](cacheSize)

  if (geoPrecision < 1) throw new IllegalArgumentException("OwmCacheClient geoPrecision must be greater than zero")

  /**
   * Round position and timestamp (event) to produce cache key
   *
   * @param timestamp timestamp in seconds
   * @param position latitude & longitude
   * @return cache key
   */
  def eventToCacheKey(timestamp: Timestamp, position: Position): CacheKey = {
    val timeFrame = getStartOfDay(timestamp)
    val roundPosition = Position(
      roundCoordinate(position.latitude),
      roundCoordinate(position.longitude))
    CacheKey(timeFrame, roundPosition)
  }

  /**
   * Get timestamp for beginning of the day
   *
   * @param timestamp event's timestamp
   * @return timestamp for beginning of day of this event
   */
  def getStartOfDay(timestamp: Timestamp): Day = {
    val calendar: Calendar = Calendar.getInstance()
    calendar.setTime(new Date(timestamp.toLong * 1000))
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    (calendar.getTimeInMillis / 1000).toInt
  }

  /**
   * Round coordinate by `geoPrecision`
   * Scale value to tenths to prevent values to be long like 1.333334
   *
   * @param coordinate latitude or longitude
   * @return rounded coordinate
   */
  def roundCoordinate(coordinate: Float): Float =
    BigDecimal(Math.round(coordinate * geoPrecision) / geoPrecision.toFloat)
      .setScale(1, BigDecimal.RoundingMode.HALF_UP).toFloat


}
