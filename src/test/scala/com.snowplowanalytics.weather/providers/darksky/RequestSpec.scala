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
package com.snowplowanalytics.weather.providers.darksky

import cats.data.NonEmptyList
import hammock._
import org.specs2.{ScalaCheck, Specification}

import requests.DarkSkyRequest
import BlockType._

class RequestSpec extends Specification with ScalaCheck {
  def is = s2"""

  Dark Sky API query-building tests

    Queries with no time, no params             $e1
    Queries with time, no params                $e2
    Exclude parameter should be comma separated $e3
    Extend parameter should have value hourly   $e4
    Should handle many parameters correctly     $e5

  """

  val baseUri = uri"http://example.com"
  val key     = "0123456789ABCDEF"

  def e1 = {
    val request = DarkSkyRequest(17.3f, -89.3f)
    request.constructQuery(baseUri, key) mustEqual baseUri / key / "17.3,-89.3"
  }

  def e2 = {
    val request = DarkSkyRequest(17.3f, -89.3f, Some(1234567))
    request.constructQuery(baseUri, key) mustEqual baseUri / key / "17.3,-89.3,1234567"
  }

  def e3 = {
    val request = DarkSkyRequest(17.3f, -89.3f, exclude = List(daily, minutely, hourly, flags))
    request.constructQuery(baseUri, key) mustEqual
      (baseUri / key / "17.3,-89.3") ? NonEmptyList.of("exclude" -> "daily,minutely,hourly,flags")
  }

  def e4 = {
    val request = DarkSkyRequest(17.3f, -89.3f, extend = true)
    request.constructQuery(baseUri, key) mustEqual
      (baseUri / key / "17.3,-89.3") ? NonEmptyList.of("extend" -> "hourly")
  }

  def e5 = {
    val request = DarkSkyRequest(17.3f,
                                 -89.3f,
                                 Some(1234567),
                                 exclude = List(daily),
                                 extend  = true,
                                 lang    = Some("pl"),
                                 units   = Some(Units.auto))
    request.constructQuery(baseUri, key) mustEqual (baseUri / key / "17.3,-89.3,1234567") ?
      NonEmptyList.of("exclude" -> "daily", "extend" -> "hourly", "lang" -> "pl", "units" -> "auto")
  }

}
