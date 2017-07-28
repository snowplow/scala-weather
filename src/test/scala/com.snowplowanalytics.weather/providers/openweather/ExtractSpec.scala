/*
 * Copyright (c) 2015-2017 Snowplow Analytics Ltd. All rights reserved.
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

import org.json4s.jackson.parseJson

import org.specs2.Specification

import Errors._
import Responses._
import Requests.OwmRequest

class ExtractSpec extends Specification { def is = s2"""

  Extract case classes from OWM responses

  Extract history
    extract history from correct JSON              $e1
    extract history from empty correct batch       $e2
    fail extract from empty JSON                   $e5

  Extract other responses
    extract current from correct JSON              $e3
    extract forecast from correct JSON             $e4
    extract forecast from correct JSON             $e4
    extract history from error JSON                $e6
                                                   """

  type Id[+X] = X
  private val dummyClient = new Client[Id] {
    def receive[W <: OwmResponse: Manifest](owmRequest: OwmRequest) = ???
  }

  def e1 = {
    val json = parseJson(Source.fromURL(getClass.getResource("/history.json")).mkString)
    val weather = dummyClient.extractWeather[History](json)
    weather must beRight
  }

  def e2 = {
    val json = parseJson(Source.fromURL(getClass.getResource("/history-empty.json")).mkString)
    val weather = dummyClient.extractWeather[History](json)
    weather.right.map(_.list.length) must beRight(0)
  }

  def e3 = {
    val json = parseJson(Source.fromURL(getClass.getResource("/current.json")).mkString)
    val weather = dummyClient.extractWeather[Current](json)
    weather.right.map(_.main.humidity) must beRight(62)
  }

  def e4 = {
    val json = parseJson(Source.fromURL(getClass.getResource("/forecast.json")).mkString)
    val weather = dummyClient.extractWeather[Forecast](json)
    weather.right.map(_.cod) must beRight("200")
  }

  def e5 = {
    val json = parseJson(Source.fromURL(getClass.getResource("/empty.json")).mkString)
    val weather = dummyClient.extractWeather[History](json)
    weather.right.map(_.cod) must beLeft
  }

  def e6 = {
    val json = parseJson(Source.fromURL(getClass.getResource("/nodata.json")).mkString)
    val weather = dummyClient.extractWeather[History](json)
    weather.right.map(_.cod) must beLeft(ErrorResponse(Some("404"), "no data"))
  }
}
