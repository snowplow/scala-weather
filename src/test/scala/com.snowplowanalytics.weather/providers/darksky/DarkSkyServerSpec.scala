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
package providers
package darksky

import java.time.{Instant, ZoneOffset, ZonedDateTime}

import scala.concurrent.duration._

import cats.Eval
import cats.effect.IO
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.specs2.specification.core.{Env, OwnExecutionEnv}
import org.specs2.{ScalaCheck, Specification}

import errors._
import responses._

/**
 * Test case classes extraction from real server responses
 *
 * Define environment variable called DARKSKY_KEY with DarkSky API Key in it,
 * otherwise these tests are skipped.
 */
class DarkSkyServerSpec(val env: Env) extends Specification with ScalaCheck with OwnExecutionEnv {
  private val key = sys.env.get("DARKSKY_KEY")

  def is = skipAllIf(key.isEmpty) ^ s2"""

    Test server responses (it can take several minutes)

      big cities - forecast                 $e1
      random cities - forecast              $e2
      big cities - history                  $e3
      random cities - history               $e4
      sane error message for unauthorized   $e5
  """

  def zonedDateTimeGenerator: Gen[ZonedDateTime] = {
    val rangeStart = ZonedDateTime.now().minusYears(1).toEpochSecond
    val rangeEnd   = ZonedDateTime.now().plusDays(13).toEpochSecond

    Gen.choose(rangeStart, rangeEnd).map { timestamp =>
      val instant = Instant.ofEpochSecond(timestamp)
      ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
    }
  }

  def testCitiesForecast[F[_]](
    cities: Vector[(Float, Float)],
    client: DarkSkyClient[F],
    f: F[Either[WeatherError, DarkSkyResponse]] => Either[WeatherError, DarkSkyResponse]
  ) =
    forAll(Gen.oneOf(cities)) {
      case (latitude, longitude) =>
        val forecast = client.forecast(latitude, longitude)
        val result   = f(forecast)
        result must beRight
    }

  def testCitiesHistory[F[_]](
    cities: Vector[(Float, Float)],
    client: DarkSkyClient[F],
    f: F[Either[WeatherError, DarkSkyResponse]] => Either[WeatherError, DarkSkyResponse]
  ) = {
    val gen = for {
      city <- Gen.oneOf(cities)
      date <- zonedDateTimeGenerator
    } yield (city, date)
    forAll(gen) {
      case ((latitude, longitude), dateTime) =>
        val timeMachine = client.timeMachine(latitude, longitude, dateTime)
        val result      = f(timeMachine)
        result must beRight
    }
  }

  private lazy val ioClient   = DarkSky.basicClient[IO](key.get)
  val ioRun                   = (a: IO[Either[WeatherError, DarkSkyResponse]]) => a.unsafeRunSync()
  private lazy val evalClient = DarkSky.unsafeBasicClient(key.get)
  val evalRun                 = (a: Eval[Either[WeatherError, DarkSkyResponse]]) => a.value

  def e1 = {
    testCitiesForecast(TestData.bigAndAbnormalCities, ioClient, ioRun)
      .set(maxSize = 10, minTestsOk = 10)
    testCitiesForecast(TestData.bigAndAbnormalCities, evalClient, evalRun)
      .set(maxSize = 10, minTestsOk = 10)
  }

  def e2 = {
    testCitiesForecast(TestData.randomCities, ioClient, ioRun)
      .set(maxSize = 10, minTestsOk = 10)
    testCitiesForecast(TestData.randomCities, evalClient, evalRun)
      .set(maxSize = 10, minTestsOk = 10)
  }

  def e3 = {
    testCitiesHistory(TestData.bigAndAbnormalCities, ioClient, ioRun)
      .set(maxSize = 10, minTestsOk = 10)
    testCitiesHistory(TestData.bigAndAbnormalCities, evalClient, evalRun)
      .set(maxSize = 10, minTestsOk = 10)
  }

  def e4 = {
    testCitiesHistory(TestData.randomCities, ioClient, ioRun).set(maxSize     = 10, minTestsOk = 10)
    testCitiesHistory(TestData.randomCities, evalClient, evalRun).set(maxSize = 10, minTestsOk = 10)
  }

  def e5 = {
    val ioClient = DarkSky.basicClient[IO]("INVALID-KEY")
    val ioResult = ioClient.forecast(0f, 0f).unsafeRunTimed(5.seconds)
    ioResult must beSome
    ioResult.get must beLeft(AuthorizationError)

    val evalClient = DarkSky.unsafeBasicClient("INVALID-KEY")
    val evalResult = evalClient.forecast(0f, 0f).value
    evalResult must beLeft(AuthorizationError)
  }

}
