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

import errors._
import implicits._
import responses._
import requests._

/**
 * Non-caching OpenWeatherMap client
 *
 * @tparam F effect type
 */
class OwmClient[F[_]] private[openweather] (transport: Transport[F]) {

  /**
   * Get historical data by city id
   * Docs: http://bugs.openweathermap.org/projects/api/wiki/Api_2_5_history#By-city-id
   *
   * @param id id of the city
   * @param start start (unix time, UTC)
   * @param end end (unix time, UTC)
   * @param cnt count of returned data
   * @param measure one of predefined `Api.Measures` to constrain accuracy
   * @return either error or history wrapped in `F`
   */
  def historyById(
    id: Int,
    start: OptArg[Long]                 = None,
    end: OptArg[Long]                   = None,
    cnt: OptArg[Int]                    = None,
    measure: OptArg[Api.Measures.Value] = None
  ): F[Either[WeatherError, History]] = {
    val request = OwmHistoryRequest(
      "city",
      Map("id" -> id.toString)
        ++ ("start" -> start)
        ++ ("end"   -> end)
        ++ ("cnt"   -> cnt)
        ++ ("type"  -> measure.map(_.toString))
    )
    transport.receive(request)
  }

  /**
   * Get historical data by city name
   * Docs: http://bugs.openweathermap.org/projects/api/wiki/Api_2_5_history#By-city-name
   *
   * @param name name of the city
   * @param country optional two-letter code
   * @param start start (unix time, UTC)
   * @param end end (unix time, UTC)
   * @param cnt count of returned data
   * @param measure one of predefined `Api.Measures` to constrain accuracy
   * @return either error or history wrapped in `F`
   */
  def historyByName(
    name: String,
    country: OptArg[String]             = None,
    start: OptArg[Long]                 = None,
    end: OptArg[Long]                   = None,
    cnt: OptArg[Int]                    = None,
    measure: OptArg[Api.Measures.Value] = None
  ): F[Either[WeatherError, History]] = {
    val query = name + country.map("," + _).getOrElse("")
    val request = OwmHistoryRequest(
      "city",
      Map("q" -> query)
        ++ ("start" -> start)
        ++ ("end"   -> end)
        ++ ("cnt"   -> cnt)
        ++ ("type"  -> measure.map(_.toString))
    )
    transport.receive(request)
  }

  /**
   * Get historical data by city name
   * Docs: http://bugs.openweathermap.org/projects/api/wiki/Api_2_5_history#By-city-name
   *
   * @param lat lattitude
   * @param lon longitude
   * @param start start (unix time, UTC)
   * @param end end (unix time, UTC)
   * @param cnt count of returned data
   * @param measure one of predefined `Api.Measures` to constrain accuracy
   * @return either error or history wrapped in `F`
   */
  def historyByCoords(
    lat: Float,
    lon: Float,
    start: OptArg[Long]                 = None,
    end: OptArg[Long]                   = None,
    cnt: OptArg[Int]                    = None,
    measure: OptArg[Api.Measures.Value] = None
  ): F[Either[WeatherError, History]] = {
    val request = OwmHistoryRequest("city",
                                    Map("lat" -> lat.toString, "lon" -> lon.toString)
                                      ++ ("start" -> start)
                                      ++ ("end"   -> end)
                                      ++ ("cnt"   -> cnt)
                                      ++ ("type"  -> measure.map(_.toString)))
    transport.receive(request)
  }

  /**
   * Get forecast data by city id
   * Docs: http://bugs.openweathermap.org/projects/api/wiki/Api_2_5_forecast#Get-forecast-by-city-id
   *
   * @param id id of the city
   * @return either error or forecast wrapped in `F`
   */
  def forecastById(id: Int, cnt: OptArg[Int] = None): F[Either[WeatherError, Forecast]] =
    transport.receive(OwmForecastRequest("city", Map("id" -> id.toString, "cnt" -> cnt.toString)))

  /**
   * Get 5 day/3 hour forecast data by city name
   * Docs: http://openweathermap.org/forecast#5days
   *
   * @param name name of the city
   * @param country optional two-letter code
   * @param cnt count of returned data
   * @return either error or forecast wrapped in `F`
   */
  def forecastByName(
    name: String,
    country: OptArg[String],
    cnt: OptArg[Int] = None
  ): F[Either[WeatherError, Forecast]] = {
    val query = name + country.map("," + _).getOrElse("")
    transport.receive(OwmForecastRequest("city", Map("q" -> query, "cnt" -> cnt.toString)))
  }

  /**
   * Get forecast data for coordinates
   *
   * @param lat latitude
   * @param lon longitude
   * @return either error or forecast wrapped in `F`
   */
  def forecastByCoords(
    lat: Float,
    lon: Float,
    cnt: OptArg[Int] = None
  ): F[Either[WeatherError, Weather]] =
    transport.receive(
      OwmForecastRequest("weather", Map("lat" -> lat.toString, "lon" -> lon.toString, "cnt" -> cnt.toString)))

  /**
   * Get current weather data by city id
   * Docs: http://bugs.openweathermap.org/projects/api/wiki/Api_2_5_weather#3-By-city-ID
   *
   * @param id id of the city
   * @return either error or current weather wrapped in `F`
   */
  def currentById(id: Int): F[Either[WeatherError, Current]] =
    transport.receive(OwmCurrentRequest("weather", Map("id" -> id.toString)))

  /**
   * Get 5 day/3 hour forecast data by city name
   * Docs: http://openweathermap.org/forecast#5days
   *
   * @param name name of the city
   * @param country optional two-letter code
   * @param cnt count of returned data
   * @return either error or forecast wrapped in `F`
   */
  def currentByName(
    name: String,
    country: OptArg[String],
    cnt: OptArg[Int] = None
  ): F[Either[WeatherError, Current]] = {
    val query = name + country.map("," + _).getOrElse("")
    transport.receive(OwmCurrentRequest("weather", Map("q" -> query, "cnt" -> cnt.toString)))
  }

  /**
   * Get current weather data by city coordinates
   * Docs: http://bugs.openweathermap.org/projects/api/wiki/Api_2_5_weather#2-By-geographic-coordinate
   *
   * @param lat latitude
   * @param lon longitude
   * @return either error or current weather wrapped in `F`
   */
  def currentByCoords(lat: Float, lon: Float): F[Either[WeatherError, Current]] =
    transport.receive(OwmCurrentRequest("weather", Map("lat" -> lat.toString, "lon" -> lon.toString)))

}
