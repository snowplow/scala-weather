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
package com.snowplowanalytics.weather.providers.openweather

import org.specs2.{ Specification, ScalaCheck }
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll

class GeoPrecisionSpec extends Specification with ScalaCheck { def is = s2"""

  Float coordinates rounder specification

  Whole number
    round 1.321 to 1.0                             $e1
    round 1.921 to 2.0                             $e2

  Half number
    round 1.321 to 1.5                             $e3
    round 2.6 to 2.5                               $e4
    round 2.85321 to 3.0                           $e5

  1/5 number
    round 7.312 to 7.4                             $e6
    round 7.8001 to 7.8                            $e7

  Test on arbitrary floats
    test 1                                         $e8
    test 1/2                                       $e9
    test 1/5                                       $e10

  Illegal state
    throw exception for zero geoPrecision          $e11
                                                   """


  // We use here it because of Scala constructor order
  private[openweather] class CacheWithUnknownSize(
      val geoPrecision: Int,
      val cacheSize: Int = 1 // Must be > 0
  ) extends WeatherCache[Responses.History]

  val rounderChecker1 = new CacheWithUnknownSize(1)
  val rounderChecker2 = new CacheWithUnknownSize(2)
  val rounderChecker5 = new CacheWithUnknownSize(5)

  def e1 = rounderChecker1.roundCoordinate(1.321f) must beEqualTo(1.0f)
  def e2 = rounderChecker1.roundCoordinate(1.921f) must beEqualTo(2.0f)
  def e3 = rounderChecker2.roundCoordinate(1.321f) must beEqualTo(1.5f)
  def e4 = rounderChecker2.roundCoordinate(2.6f) must beEqualTo(2.5f)
  def e5 = rounderChecker2.roundCoordinate(2.85321f) must beEqualTo(3.0f)
  def e6 = rounderChecker5.roundCoordinate(7.312f) must beEqualTo(7.4f)
  def e7 = rounderChecker5.roundCoordinate(7.8001f) must beEqualTo(7.8f)
  def e11 = new CacheWithUnknownSize(0) must throwA[IllegalArgumentException]

  // Rounding arbitrary floats
  val sensibleFloat =         // we want omit big exponents
    Arbitrary.arbitrary[Float] suchThat (f => (f > -180.0) && (f < 180.0) )

  def e8 = forAll(sensibleFloat) { f: Float =>
    rounderChecker1.roundCoordinate(f).toString must endWith(".0")
  }
  def e9 = forAll(sensibleFloat) { f: Float =>
    rounderChecker2.roundCoordinate(f).toString must endWith(".0") or endWith(".5")
  }
  def e10 = forAll(sensibleFloat) { f: Float =>
    rounderChecker5.roundCoordinate(f).toString must endWith(".0") or endWith(".2") or endWith(".4") or endWith(".6") or endWith(".8")
  }
}
