/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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

import scala.concurrent.duration._
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.specs2.{ScalaCheck, Specification}
import org.specs2.specification.core.{Env, OwnExecutionEnv}
import org.scalacheck.Prop
import Cache.Position
import errors._
import org.specs2.matcher.MatchResult
import providers.TestData
import responses._

object ServerSpec {
  val owmKey = sys.env.get("OWM_KEY")
}

import ServerSpec._

/**
  * Test case classes extraction from real server responses
  *
  * Define environment variable called OWM_KEY with OpenWeatherMap API Key in it,
  * otherwise these tests are skipped.
  */
class ServerSpec(val env: Env) extends Specification with ScalaCheck with OwnExecutionEnv with WeatherGenerator {
  def is = skipAllIf(owmKey.isEmpty) ^ s2"""

    Test server responses for current weather by coordinates (it can take several minutes)

      big cities                            $e1
      random cities                         $e2
      sane error message for unauthorized   $e3
      works with https                      $e4
  """

  val host          = "api.openweathermap.org"
  lazy val ioClient = CreateOWM[IO].create(host, owmKey.get, 1.seconds, true)
  val ioRun         = (a: IO[Either[WeatherError, Current]]) => a.unsafeRunSync()

  def testCities[F[_]](
    cities: Vector[(Float, Float)],
    client: OWMClient[F],
    f: F[Either[WeatherError, Current]] => Either[WeatherError, Current]
  ): Prop =
    Prop.forAll(genPredefinedPosition(cities)) { position: Position =>
      val current = client.currentByCoords(position.latitude, position.longitude)
      val result  = f(current)
      result must beRight
    }

  def e1 =
    testCities(TestData.bigAndAbnormalCities, ioClient, ioRun).set(maxSize = 5, minTestsOk = 5)

  def e2 =
    testCities(TestData.randomCities, ioClient, ioRun).set(maxSize = 10, minTestsOk = 10)

  def e3: MatchResult[Option[Either[WeatherError, Current]]] = {
    val ioClient = CreateOWM[IO].create(host, "INVALID-KEY", 1.seconds, true)
    val ioResult = ioClient.currentById(1).unsafeRunTimed(5.seconds)
    ioResult must beSome(Left[WeatherError, Current](AuthorizationError))
  }

  def e4 =
    testCities(TestData.randomCities, ioClient, ioRun).set(maxSize = 10, minTestsOk = 10)
}
