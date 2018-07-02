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

import org.specs2.{ScalaCheck, Specification}

import org.scalacheck.Prop.forAll
import org.scalacheck.Gen

import Errors._
import Responses._

class BatchGetSpec extends Specification with ScalaCheck with WeatherGenerator {
  def is = s2"""

  Pick neighbour item out of collection

  Take an integer
    take greater value                             $e1
    take much greater value                        $e2
    take lesser value                              $e3
    take last same value                           $e4
    take None from empty list                      $e5

  Take a weather
    take closest weather from history response     $e6
    always pick stamp from non-empty response      $e7
    never pick stamp from empty response           $e8
    fail on invalid timestamp                      $e9
                                                   """

  // Make a pair of same value
  private val pair = (i: Int) => (i, i)

  def e1 = pickClosest(List(100, 14, 1, 5, 10, 12), 13, pair) must beSome(14)
  def e2 = pickClosest(List(100, 14, 1, 5, 10, 12), 9000, pair) must beSome(100)
  def e3 = pickClosest(List(100, 14, 1, 5, 10, 12), 2, pair) must beSome(1)
  def e4 = pickClosest(List(100, 14, 1, 5, 10, 12), 12, pair) must beSome(12)
  def e5 = pickClosest(List(), 12, pair) must beNone
  def e6 =
    History(
      2,
      "200",
      List(
        Weather(
          main = MainInfo(Some(100),
                          50,
                          BigDecimal(3),
                          Some(BigDecimal(10)),
                          BigDecimal(12),
                          BigDecimal(10),
                          BigDecimal(15)),
          wind    = Wind(1, 5, None, None, None),
          clouds  = Clouds(100),
          rain    = None,
          snow    = None,
          weather = List(WeatherCondition("Clouds", "few clouds", 801, "02d")),
          dt      = 1447941977
        ),
        Weather(
          main = MainInfo(Some(150),
                          50,
                          BigDecimal(13),
                          Some(BigDecimal(0)),
                          BigDecimal(17),
                          BigDecimal(10),
                          BigDecimal(25)),
          wind    = Wind(1, 5, None, None, None),
          clouds  = Clouds(100),
          rain    = None,
          snow    = None,
          weather = List(WeatherCondition("Haze", "haze", 721, "50n")),
          dt      = 1447941171
        )
      )
    ).pickCloseIn(1447941101).right.map(_.dt) must beRight(1447941171)

  def e7 = forAll(genNonEmptyHistoryBatch, genTimestamp) { (h: History, t: Int) =>
    h.pickCloseIn(t) must beRight
  }
  def e8 = forAll(genEmptyHistoryBatch, genTimestamp) { (h: History, t: Int) =>
    h.pickCloseIn(t) must beLeft(InternalError("Server response has no weather stamps"))
  }
  def e9 = forAll(genNonEmptyHistoryBatch, Gen.oneOf(0, -1, -2)) { (h: History, t: Int) =>
    h.pickCloseIn(t) must beLeft(InternalError("Timestamp should be greater than 0"))
  }
}
