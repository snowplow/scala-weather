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

import scala.io.Source

import io.circe.parser.parse
import org.specs2.Specification

import errors._
import responses._

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

  def e1 = {
    val weather = parse(Source.fromURL(getClass.getResource("/history.json")).mkString)
      .flatMap(json => HttpTransport.extractWeather[History](json))
    weather must beRight
  }

  def e2 = {
    val weather = parse(Source.fromURL(getClass.getResource("/history-empty.json")).mkString)
      .flatMap(json => HttpTransport.extractWeather[History](json))
    weather.map(_.list.length) must beRight(0)
  }

  def e3 = {
    val weather = parse(Source.fromURL(getClass.getResource("/current.json")).mkString)
      .flatMap(json => HttpTransport.extractWeather[Current](json))
    weather.map(_.main.humidity) must beRight(62)
  }

  def e4 = {
    val weather = parse(Source.fromURL(getClass.getResource("/forecast.json")).mkString)
      .flatMap(json => HttpTransport.extractWeather[Forecast](json))
    weather.map(_.cod) must beRight("200")
  }

  def e5 = {
    val weather = parse(Source.fromURL(getClass.getResource("/empty.json")).mkString)
      .flatMap(json => HttpTransport.extractWeather[History](json))
    weather.map(_.cod) must beLeft
  }

  def e6 = {
    val weather = parse(Source.fromURL(getClass.getResource("/nodata.json")).mkString)
      .flatMap(json => HttpTransport.extractWeather[History](json))
    weather.map(_.cod) must beLeft(ErrorResponse(Some("404"), "no data"))
  }
}
