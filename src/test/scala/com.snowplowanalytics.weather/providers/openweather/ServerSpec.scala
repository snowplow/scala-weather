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
import scala.concurrent.duration._

// cats
import cats.effect.IO

// tests
import org.specs2.{ScalaCheck, Specification}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.specification.ExecutionEnvironment

import org.scalacheck.Prop.forAll

// This library
import Errors.AuthorizationError
import CacheUtils.Position

object ServerSpec {
  val owmKey = sys.env.get("OWM_KEY")
}

import ServerSpec._

/**
 * Test case classes extraction from real server responses
 */
class ServerSpec extends Specification with ScalaCheck with ExecutionEnvironment with WeatherGenerator {
  def is(implicit ee: ExecutionEnv) = skipAllIf(owmKey.isEmpty) ^ s2"""

    Test server responses for history requests by coordinates (it can take several minutes)

      big cities                            $e1
      random cities                         $e2
      sane error message for unauthorized   $e3
      works with https                      $e4
  """

  val host      = "history.openweathermap.org"
  val client    = OpenWeatherMap.basicClient[IO](owmKey.get, host)
  val sslClient = OpenWeatherMap.basicClient[IO](owmKey.get, host, ssl = true)

  def testCities(cities: Vector[Position], client: OwmClient[IO]) =
    forAll(genPredefinedPosition(cities), genLastWeekTimeStamp) { (position: Position, timestamp: Timestamp) =>
      val history = client.historyByCoords(position.latitude, position.longitude, timestamp, timestamp + 80000)
      val result  = history.unsafeRunTimed(5.seconds)
      result must beSome
      result.get must beRight
    }

  def e1 = testCities(TestData.bigAndAbnormalCities, client).set(maxSize = 5, minTestsOk = 5)

  def e2 = testCities(TestData.randomCities, client).set(maxSize = 15, minTestsOk = 15)

  def e3 = {
    val client = OpenWeatherMap.basicClient[IO]("INVALID-KEY", host)
    val result = client.historyById(1).unsafeRunTimed(5.seconds)
    result must beSome
    result.get must beLeft(AuthorizationError)
  }

  def e4 = testCities(TestData.randomCities, sslClient).set(maxSize = 15, minTestsOk = 15)
}
