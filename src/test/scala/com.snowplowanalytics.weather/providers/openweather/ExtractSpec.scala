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

import scala.io.Source

import scalaz.\/
import scalaz.Id.Id

import org.json4s.JValue
import org.json4s.jackson.parseJson

import org.specs2.matcher.DisjunctionMatchers
import org.specs2.Specification

import Responses._
import Requests.OwmRequest

class ExtractSpec extends Specification with DisjunctionMatchers { def is = s2"""

  Extract case classes from OWM responses

  Extract history
    extract history from correct JSON              $e1
    extract history from empty correct batch       $e2
    fail extract from empty JSON                   $e5

  Extract other responses
    extract current from correct JSON              $e3
    extract forecast from correct JSON             $e4
                                                   """

  private val dummyClient = new Client[Id] {
    def receive[W <: OwmResponse: Manifest](owmRequest: OwmRequest) = ???
  }

  def e1 = {
    val json = \/.right[WeatherError, JValue](parseJson(Source.fromURL(getClass.getResource("/history.json")).mkString))
    val weather = dummyClient.extractWeather[History](json)
    weather must be_\/-
  }

  def e2 = {
    val json = \/.right[WeatherError, JValue](parseJson(Source.fromURL(getClass.getResource("/history-empty.json")).mkString))
    val weather = dummyClient.extractWeather[History](json)
    weather.map(_.list.length) must be_\/-(0)
  }

  def e3 = {
    val json = \/.right[WeatherError, JValue](parseJson(Source.fromURL(getClass.getResource("/current.json")).mkString))
    val weather = dummyClient.extractWeather[Current](json)
    weather.map(_.main.humidity) must be_\/-(62)
  }

  def e4 = {
    val json = \/.right[WeatherError, JValue](parseJson(Source.fromURL(getClass.getResource("/forecast.json")).mkString))
    val weather = dummyClient.extractWeather[Forecast](json)
    weather.map(_.cod) must be_\/-("200")
  }

  def e5 = {
    val json = \/.right[WeatherError, JValue](parseJson(Source.fromURL(getClass.getResource("/empty.json")).mkString))
    val weather = dummyClient.extractWeather[History](json)
    weather.map(_.cod) must be_-\/
  }
}
