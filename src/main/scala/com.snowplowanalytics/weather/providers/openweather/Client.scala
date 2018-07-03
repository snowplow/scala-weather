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

// Scala
import scala.language.higherKinds

// Json4s
import org.json4s.JValue
import org.json4s.DefaultFormats
import org.json4s.jackson.compactJson

// This library
import Errors._
import Implicits._
import Responses._
import Requests._

/**
 * Base client trait with defined client methods such as `historyById`, `historyByName`
 * common for subclasses
 *
 * @tparam F response wrapper for `Client` subclass, such as cats `IO`
 *                  all `receive` logic should be wrapped in it
 */
trait Client[F[_]] {

  private implicit val formats = DefaultFormats

  /**
   * Main client logic for Request => Response function,
   * where Response is wrappeed in tparam `F`
   *
   * @param owmRequest request built by client method
   * @tparam W type of weather response to extract
   * @return extracted either error or weather wrapped in `F`
   */
  def receive[W <: OwmResponse: Manifest](owmRequest: OwmRequest): F[Either[WeatherError, W]]

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
  def historyById(id: Int,
                  start: OptArg[Int]                  = None,
                  end: OptArg[Int]                    = None,
                  cnt: OptArg[Int]                    = None,
                  measure: OptArg[Api.Measures.Value] = None): F[Either[WeatherError, History]] = {
    val request = OwmHistoryRequest("city",
                                    Map("id" -> id.toString)
                                      ++ ("start" -> start)
                                      ++ ("end"   -> end)
                                      ++ ("cnt"   -> cnt)
                                      ++ ("type"  -> measure.map(_.toString)))
    receive(request)
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
  def historyByName(name: String,
                    country: OptArg[String]             = None,
                    start: OptArg[Int]                  = None,
                    end: OptArg[Int]                    = None,
                    cnt: OptArg[Int]                    = None,
                    measure: OptArg[Api.Measures.Value] = None): F[Either[WeatherError, History]] = {
    val query = name + country.map("," + _).getOrElse("")
    val request = OwmHistoryRequest("city",
                                    Map("q" -> query)
                                      ++ ("start" -> start)
                                      ++ ("end"   -> end)
                                      ++ ("cnt"   -> cnt)
                                      ++ ("type"  -> measure.map(_.toString)))
    receive(request)
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
  def historyByCoords(lat: Float,
                      lon: Float,
                      start: OptArg[Int]                  = None,
                      end: OptArg[Int]                    = None,
                      cnt: OptArg[Int]                    = None,
                      measure: OptArg[Api.Measures.Value] = None): F[Either[WeatherError, History]] = {
    val request = OwmHistoryRequest("city",
                                    Map("lat" -> lat.toString, "lon" -> lon.toString)
                                      ++ ("start" -> start)
                                      ++ ("end"   -> end)
                                      ++ ("cnt"   -> cnt)
                                      ++ ("type"  -> measure.map(_.toString)))
    receive(request)
  }

  /**
   * Get forecast data by city id
   * Docs: http://bugs.openweathermap.org/projects/api/wiki/Api_2_5_forecast#Get-forecast-by-city-id
   *
   * @param id id of the city
   * @return either error or forecast wrapped in `F`
   */
  def forecastById(id: Int, cnt: OptArg[Int] = None): F[Either[WeatherError, Forecast]] =
    receive(OwmForecastRequest("city", Map("id" -> id.toString, "cnt" -> cnt.toString)))

  /**
   * Get 5 day/3 hour forecast data by city name
   * Docs: http://openweathermap.org/forecast#5days
   *
   * @param name name of the city
   * @param country optional two-letter code
   * @param cnt count of returned data
   * @return either error or forecast wrapped in `F`
   */
  def forecastByName(name: String,
                     country: OptArg[String],
                     cnt: OptArg[Int] = None): F[Either[WeatherError, Forecast]] = {
    val query = name + country.map("," + _).getOrElse("")
    receive(OwmForecastRequest("city", Map("q" -> query, "cnt" -> cnt.toString)))
  }

  /**
   * Get forecast data for coordinates
   *
   * @param lat latitude
   * @param lon longitude
   * @return either error or forecast wrapped in `F`
   */
  def forecastByCoords(lat: Float, lon: Float, cnt: OptArg[Int] = None): F[Either[WeatherError, Weather]] =
    receive(OwmForecastRequest("weather", Map("lat" -> lat.toString, "lon" -> lon.toString, "cnt" -> cnt.toString)))

  /**
   * Get current weather data by city id
   * Docs: http://bugs.openweathermap.org/projects/api/wiki/Api_2_5_weather#3-By-city-ID
   *
   * @param id id of the city
   * @return either error or current weather wrapped in `F`
   */
  def currentById(id: Int): F[Either[WeatherError, Current]] =
    receive(OwmCurrentRequest("weather", Map("id" -> id.toString)))

  /**
   * Get 5 day/3 hour forecast data by city name
   * Docs: http://openweathermap.org/forecast#5days
   *
   * @param name name of the city
   * @param country optional two-letter code
   * @param cnt count of returned data
   * @return either error or forecast wrapped in `F`
   */
  def currentByName(name: String,
                    country: OptArg[String],
                    cnt: OptArg[Int] = None): F[Either[WeatherError, Current]] = {
    val query = name + country.map("," + _).getOrElse("")
    receive(OwmCurrentRequest("weather", Map("q" -> query, "cnt" -> cnt.toString)))
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
    receive(OwmCurrentRequest("weather", Map("lat" -> lat.toString, "lon" -> lon.toString)))

  /**
   * Transform JSON into parseable format and try to extract specified response
   *
   * @param response either of previous or JSON
   * @tparam W specific response case class from
   *           `com.snowplowanalytics.weather.providers.openweather.Responses`
   * @return either error string or response case class
   */
  protected[openweather] def extractWeather[W: Manifest](response: JValue): Either[WeatherError, W] =
    response.extractOpt[W] match {
      case Some(weather) => Right(weather)
      case None =>
        response.extractOpt[ErrorResponse] match {
          case Some(error) => Left(error)
          case None        => Left(ParseError(s"Could not extract ${manifest[W]} from ${compactJson(response)}"))
        }
    }
}
