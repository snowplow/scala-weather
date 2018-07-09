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

// Java
import java.util.{Calendar, Date, TimeZone}

object CacheUtils {

  /**
   * Cache key for obtaining record
   *
   * @param day timestamp for 0:00:00
   * @param center rounded geo coordinates
   */
  case class CacheKey(day: Day, center: Position) {
    def endOfDay = day + 86400
  }

  /**
   * Class to represent geographical coordinates
   *
   * @param latitude place's latitude
   * @param longitude places's longitude
   */
  case class Position(latitude: Float, longitude: Float)

  /**
   * Round position and timestamp (event) to produce cache key
   *
   * @param timestamp timestamp in seconds
   * @param position latitude & longitude
   * @param geoPrecision nth part of 1 to which latitude and longitude will be rounded
   * @return cache key
   */
  def eventToCacheKey(timestamp: Timestamp, position: Position, geoPrecision: Int): CacheKey = {
    val timeFrame = getStartOfDay(timestamp)
    val roundPosition =
      Position(roundCoordinate(position.latitude, geoPrecision), roundCoordinate(position.longitude, geoPrecision))
    CacheKey(timeFrame, roundPosition)
  }

  /**
   * Get timestamp for beginning of the day
   *
   * @param timestamp event's timestamp
   * @return timestamp for beginning of day of this event
   */
  def getStartOfDay(timestamp: Timestamp): Day = {
    val calendar: Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
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
  def roundCoordinate(coordinate: Float, geoPrecision: Int): Float =
    BigDecimal
      .decimal(Math.round(coordinate * geoPrecision) / geoPrecision.toFloat)
      .setScale(1, BigDecimal.RoundingMode.HALF_UP)
      .toFloat
}
