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
package com.snowplowanalytics.weather.providers.openweather

import org.specs2.{ScalaCheck, Specification}
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll

class GeoPrecisionSpec extends Specification with ScalaCheck {
  def is = s2"""

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
                                                   """

  def e1 = CacheUtils.roundCoordinate(1.321f, 1) must beEqualTo(1.0f)
  def e2 = CacheUtils.roundCoordinate(1.921f, 1) must beEqualTo(2.0f)
  def e3 = CacheUtils.roundCoordinate(1.321f, 2) must beEqualTo(1.5f)
  def e4 = CacheUtils.roundCoordinate(2.6f, 2) must beEqualTo(2.5f)
  def e5 = CacheUtils.roundCoordinate(2.85321f, 2) must beEqualTo(3.0f)
  def e6 = CacheUtils.roundCoordinate(7.312f, 5) must beEqualTo(7.4f)
  def e7 = CacheUtils.roundCoordinate(7.8001f, 5) must beEqualTo(7.8f)

  // Rounding arbitrary floats
  val sensibleFloat = // we want omit big exponents
    Arbitrary.arbitrary[Float] suchThat (f => (f > -180.0) && (f < 180.0))

  def e8 = forAll(sensibleFloat) { f: Float =>
    CacheUtils.roundCoordinate(f, 1).toString must endWith(".0")
  }
  def e9 = forAll(sensibleFloat) { f: Float =>
    CacheUtils.roundCoordinate(f, 2).toString must endWith(".0") or endWith(".5")
  }
  def e10 = forAll(sensibleFloat) { f: Float =>
    CacheUtils
      .roundCoordinate(f, 5)
      .toString must endWith(".0") or endWith(".2") or endWith(".4") or endWith(".6") or endWith(".8")
  }
}
