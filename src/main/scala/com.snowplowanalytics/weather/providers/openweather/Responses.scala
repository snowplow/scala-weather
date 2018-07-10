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

// circe
import io.circe.generic.JsonCodec

// This library
import Errors._

/**
 * Case classes used for extracting data from JSON
 */
object Responses {
  sealed trait OwmResponse extends WeatherResponse

  // RESPONSES

  // Very similar to Weather
  @JsonCodec
  final case class Current(main: MainInfo,
                           wind: Wind,
                           clouds: Clouds,
                           coord: Option[Coordinates],
                           visibility: Option[BigInt])
      extends OwmResponse

  @JsonCodec
  final case class Forecast(cnt: BigInt, cod: String, list: List[Weather]) extends OwmResponse {
    def pickCloseIn(timestamp: Int): Either[WeatherError, Weather] =
      pickClosestWeather(list, timestamp)
  }

  @JsonCodec
  final case class History(cnt: BigInt, cod: String, list: List[Weather]) extends OwmResponse {
    def pickCloseIn(timestamp: Int): Either[WeatherError, Weather] =
      pickClosestWeather(list, timestamp)
  }

  // DETAILS

  /**
   * Weather conditions at exact moment, past, future or current
   * Core data type
   */
  @JsonCodec
  final case class Weather(main: MainInfo,
                           wind: Wind,
                           clouds: Clouds,
                           rain: Option[Rain],
                           snow: Option[Snow],
                           weather: List[WeatherCondition],
                           dt: BigInt)
      extends OwmResponse

  /**
   * Common main information about weather
   */
  @JsonCodec
  final case class MainInfo(grnd_level: Option[BigDecimal],
                            humidity: BigDecimal,
                            pressure: BigDecimal,
                            sea_level: Option[BigDecimal],
                            temp: BigDecimal,
                            temp_min: BigDecimal,
                            temp_max: BigDecimal)

  /**
   * Textual description of the weather
   */
  @JsonCodec
  final case class WeatherCondition(main: String, description: String, id: Int, icon: String)

  @JsonCodec
  final case class Wind(speed: BigDecimal,
                        deg: BigDecimal,
                        gust: Option[BigDecimal],
                        var_end: Option[Int],
                        var_beg: Option[Int])
  @JsonCodec final case class Coordinates(lon: BigDecimal, lat: BigDecimal)
  @JsonCodec final case class Clouds(all: BigInt)
  @JsonCodec final case class Snow(`1h`: Option[BigDecimal], `3h`: Option[BigDecimal])
  @JsonCodec final case class Rain(`1h`: Option[BigDecimal], `3h`: Option[BigDecimal])

  /**
   * Pick an Integer from `list` which is close-in to `item`
   *
   * @param timestamp original integer
   * @return close neighbour
   */
  private[openweather] def pickClosestWeather(list: List[Weather], timestamp: Int): Either[WeatherError, Weather] =
    if (timestamp < 1) Left(InternalError("Timestamp should be greater than 0"))
    else
      pickClosest(list, timestamp, (st: Weather) => (st.dt.toInt, st))
        .map(Right(_))
        .getOrElse(Left(InternalError("Server response has no weather stamps")))

  /**
   * Helper function for taking closest value out of some list
   *
   * @param list list of objects (weather stamps)
   * @param index some index to derive position in ordering (timestamp)
   * @param transform function to derive index (timestamp) out of object (weather stamp)
   * @return closest object for some index
   */
  private[openweather] def pickClosest[A](list: List[A], index: Int, transform: A => (Int, A)): Option[A] =
    list
      .map(transform)
      .sortBy(x => Math.abs(index - x._1))
      .headOption
      .map(_._2)
}
