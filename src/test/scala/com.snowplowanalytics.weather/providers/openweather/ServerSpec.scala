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

import scala.concurrent.Await
import scala.concurrent.duration._

import org.specs2.{ ScalaCheck, Specification }
import org.specs2.concurrent.ExecutionEnv
import org.specs2.specification.ExecutionEnvironment

import org.scalacheck.Prop.forAll

import WeatherCache.Position
import Errors.WeatherError

object ServerSpec {
  val owmKey = sys.env.get("OWM_KEY")
}

import ServerSpec._
/**
 * Test case classes extraction from real server responses
 */
class ServerSpec
  extends Specification
    with ScalaCheck
    with ExecutionEnvironment
    with WeatherGenerator { def is(implicit ee: ExecutionEnv) = skipAllIf(owmKey.isEmpty) ^ s2"""

    Test server responses for history requests by coordinates (it can take several minutes)

      big cities                            $e1
      random cities                         $e2
      sane error message for unauthorized   $e3
      works with https                      $e4
  """

  val host = "history.openweathermap.org"
  val transportForCache = new HttpTransport(host)
  val client = OwmAsyncClient(owmKey.get, transportForCache)
  val sslClient = OwmAsyncClient(owmKey.get, new HttpTransport(host, ssl = true))

  def testCities(cities: Vector[Position], client: OwmAsyncClient) = {
    forAll(genPredefinedPosition(cities), genLastWeekTimeStamp) { (position: Position, timestamp: Timestamp) =>
      val history = client.historyByCoords(position.latitude, position.longitude, timestamp, timestamp + 80000)
      Await.result(history, 5 seconds) must beRight
    }
  }

  def e1 = testCities(TestData.bigAndAbnormalCities, client).set(maxSize = 5, minTestsOk = 5)

  def e2 = testCities(TestData.randomCities, client).set(maxSize = 15, minTestsOk = 15)

  def e3 = {
    val client = OwmAsyncClient("INVALID-KEY", transportForCache)
    val result = client.historyById(1)
    Await.result(result, 5 seconds) must beLeft.like {
      case e: WeatherError => e.toString must beEqualTo("OpenWeatherMap AuthorizationError$ Check your API key")
    }
  }

  def e4 = testCities(TestData.randomCities, sslClient).set(maxSize = 15, minTestsOk = 15)
}
