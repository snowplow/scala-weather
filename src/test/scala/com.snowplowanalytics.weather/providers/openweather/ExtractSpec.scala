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
import scala.io.Source

// cats
import cats.Id
import cats.syntax.either._

// specs2
import org.specs2.Specification

// circe
import io.circe.Decoder
import io.circe.parser.parse

// This library
import Errors._
import Responses._
import Requests.OwmRequest

class ExtractSpec extends Specification {
  def is = s2"""

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

  private val dummyClient = new Client[Id] {
    def receive[W <: OwmResponse: Decoder](owmRequest: OwmRequest) = ???
  }

  def e1 = {
    val weather = parse(Source.fromURL(getClass.getResource("/history.json")).mkString)
      .flatMap(dummyClient.extractWeather[History])
    weather must beRight
  }

  def e2 = {
    val weather = parse(Source.fromURL(getClass.getResource("/history-empty.json")).mkString)
      .flatMap(dummyClient.extractWeather[History])
    weather.map(_.list.length) must beRight(0)
  }

  def e3 = {
    val weather = parse(Source.fromURL(getClass.getResource("/current.json")).mkString)
      .flatMap(dummyClient.extractWeather[Current])
    weather.map(_.main.humidity) must beRight(62)
  }

  def e4 = {
    val weather = parse(Source.fromURL(getClass.getResource("/forecast.json")).mkString)
      .flatMap(dummyClient.extractWeather[Forecast])
    weather.map(_.cod) must beRight("200")
  }

  def e5 = {
    val weather = parse(Source.fromURL(getClass.getResource("/empty.json")).mkString)
      .flatMap(dummyClient.extractWeather[History])
    weather.map(_.cod) must beLeft
  }

  def e6 = {
    val weather = parse(Source.fromURL(getClass.getResource("/nodata.json")).mkString)
      .flatMap(dummyClient.extractWeather[History])
    weather.map(_.cod) must beLeft(ErrorResponse(Some("404"), "no data"))
  }
}
