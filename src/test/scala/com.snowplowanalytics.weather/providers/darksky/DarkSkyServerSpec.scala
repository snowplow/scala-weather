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
package providers.darksky

// Java
import java.time.{Instant, ZoneOffset, ZonedDateTime}

// Scala
import scala.concurrent.duration._

// cats
import cats.effect.IO

// tests
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.specs2.concurrent.ExecutionEnv
import org.specs2.specification.ExecutionEnvironment
import org.specs2.{ScalaCheck, Specification}

// This library
import com.snowplowanalytics.weather.Errors.AuthorizationError
import com.snowplowanalytics.weather.providers.TestData

/**
 * Test case classes extraction from real server responses
 */
class DarkSkyServerSpec extends Specification with ScalaCheck with ExecutionEnvironment {
  def is(implicit ee: ExecutionEnv) = skipAllIf(key.isEmpty) ^ s2"""

    Test server responses (it can take several minutes)

      big cities - forecast                 $e1
      random cities - forecast              $e2
      big cities - history                  $e3
      random cities - history               $e4
      sane error message for unauthorized   $e5
  """

  private val key = sys.env.get("DARKSKY_KEY")
  if (key.isEmpty) {
    throw new RuntimeException("Define environment variable called DARKSKY_KEY with DarkSky API Key in it")
  }

  def zonedDateTimeGenerator: Gen[ZonedDateTime] = {
    val rangeStart = ZonedDateTime.now().minusYears(1).toEpochSecond
    val rangeEnd   = ZonedDateTime.now().plusDays(13).toEpochSecond

    Gen.choose(rangeStart, rangeEnd).map { timestamp =>
      val instant = Instant.ofEpochSecond(timestamp)
      ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
    }
  }

  val client = DarkSky.basicClient[IO](key.get)

  def testCitiesForecast(cities: Vector[(Float, Float)]) =
    forAll(Gen.oneOf(cities)) {
      case (latitude, longitude) =>
        val result = client.forecast(latitude, longitude).unsafeRunTimed(5.seconds)
        result must beSome
        result.get must beRight
    }

  def testCitiesHistory(cities: Vector[(Float, Float)]) = {
    val gen = for {
      city <- Gen.oneOf(cities)
      date <- zonedDateTimeGenerator
    } yield (city, date)
    forAll(gen) {
      case ((latitude, longitude), dateTime) =>
        val result = client.timeMachine(latitude, longitude, dateTime).unsafeRunTimed(5.seconds)
        result must beSome
        result.get must beRight
    }
  }

  def e1 = testCitiesForecast(TestData.bigAndAbnormalCities).set(maxSize = 10, minTestsOk = 10)

  def e2 = testCitiesForecast(TestData.randomCities).set(maxSize = 10, minTestsOk = 10)

  def e3 = testCitiesHistory(TestData.bigAndAbnormalCities).set(maxSize = 10, minTestsOk = 10)

  def e4 = testCitiesHistory(TestData.randomCities).set(maxSize = 10, minTestsOk = 10)

  def e5 = {
    val client = DarkSky.basicClient[IO]("INVALID-KEY")
    val result = client.forecast(0f, 0f).unsafeRunTimed(5.seconds)
    result must beSome
    result.get must beLeft(AuthorizationError)
  }

}
