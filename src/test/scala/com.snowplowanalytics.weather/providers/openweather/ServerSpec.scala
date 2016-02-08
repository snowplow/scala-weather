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

import com.snowplowanalytics.weather.providers

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration._

import org.specs2.{ ScalaCheck, Specification }
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.DisjunctionMatchers
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
    with DisjunctionMatchers
    with ExecutionEnvironment
    with WeatherGenerator { def is(implicit ee: ExecutionEnv) = skipAllIf(owmKey.isEmpty) ^ s2"""

    Test server responses for history requests by coordinates (it can take several minutes)

      big cities                            $e1
      random cities                         $e2
      sane error message for unauthorized   $e3
      sane error message for not found city $e4
  """

  val conf = ConfigFactory.parseString("akka.log-dead-letters = 0, akka.daemonic = on")

  lazy val system = ActorSystem("test-actor-system", conf)
  val transportForCache = AkkaHttpTransport(system, "pro.openweathermap.org")

  def testCities(cities: Vector[Position]) = {
    val client = OwmAsyncClient(owmKey.get, transportForCache)
    forAll(genPredefinedPosition(cities), genLastWeekTimeStamp) { (position: Position, timestamp: Timestamp) =>
      val history = client.historyByCoords(position.latitude, position.longitude, timestamp, timestamp + 80000)
      Await.result(history, 5 seconds) must be_\/-
    }
  }

  def e1 = testCities(TestData.bigAndAbnormalCities).set(maxSize = 5, minTestsOk = 5)

  def e2 = testCities(TestData.randomCities).set(maxSize = 15, minTestsOk = 15)

  def e3 = {
    val client = OwmAsyncClient("INVALID-KEY", transportForCache)
    val result = client.historyById(1)
    Await.result(result, 5 seconds) must be_-\/.like {
      case e: WeatherError => e.toString must beEqualTo("OpenWeatherMap AuthorizationError$ Check your API key")
    }
  }

  def e4 = {
    val client = OwmAsyncClient(owmKey.get, transportForCache)
    val result = client.historyById(0, start=1015606302, end=1015609910)
    Await.result(result, 5 seconds) must be_-\/.like {
      case e: WeatherError => e.toString must beEqualTo("OpenWeatherMap ErrorResponse no data")
    }
  }
}
