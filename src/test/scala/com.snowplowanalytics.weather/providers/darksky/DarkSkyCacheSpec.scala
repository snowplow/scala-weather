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
package com.snowplowanalytics.weather
package providers.darksky

import java.time.ZonedDateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import cats.effect.{ContextShift, IO}
import cats.syntax.either._
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.specs2.Specification
import org.specs2.mock.Mockito

import errors.{InvalidConfigurationError, TimeoutError}
import requests.DarkSkyRequest
import responses.DarkSkyResponse

class DarkSkyCacheSpec extends Specification with Mockito {
  def is =
    s2"""

  Test cache specification

    do not bother server on identical requests $e1
    do not bother server on similar requests $e4
    retry request on timeout error $e2
    check geoPrecision $e5
    make requests again after full cache $e3
    be left for invalid precision $e6
    be left for invalid cache size $e7
  """

  implicit val timer                = IO.timer(global)
  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  val ioExampleResponse: IO[Right[TimeoutError, DarkSkyResponse]] =
    IO.pure(Right(DarkSkyResponse(0f, 0f, "", None, None, None, None, None, None)))
  val ioTimeoutErrorResponse: IO[Either[TimeoutError, DarkSkyResponse]] =
    IO.pure(TimeoutError("java.util.concurrent.TimeoutException: Futures timed out after [1 second]").asLeft)

  def e1 = {
    implicit val ioTransport = mock[Transport[IO]].defaultReturn(ioExampleResponse)
    val ioAction = for {
      client <- CreateDarkSky[IO].create("host", "key", 1.seconds, 1, 1).map(_.toOption.get)
      _      <- client.cachingTimeMachine(4.44f, 3.33f, ZonedDateTime.now())
      _      <- client.cachingTimeMachine(4.44f, 3.33f, ZonedDateTime.now())
      _      <- client.cachingTimeMachine(4.44f, 3.33f, ZonedDateTime.now())
    } yield ()
    ioAction.unsafeRunSync()
    there.was(
      1.times(ioTransport)
        .receive[DarkSkyResponse](any[DarkSkyRequest], eqTo("host"), eqTo("key"), eqTo(1.seconds), eqTo(true))(
          eqTo(implicitly)))
  }

  def e2 = {
    implicit val ioTransport = mock[Transport[IO]]
    ioTransport
      .receive[DarkSkyResponse](any[DarkSkyRequest], any[String], any[String], any[FiniteDuration], any[Boolean])(
        eqTo(implicitly))
      .returns(ioTimeoutErrorResponse)
      .thenReturn(ioExampleResponse)

    val ioAction = for {
      client <- CreateDarkSky[IO].create("host", "key", 1.seconds, 2, 1).map(_.toOption.get)
      _      <- client.cachingTimeMachine(4.44f, 3.33f, ZonedDateTime.now())
      _      <- client.cachingTimeMachine(4.44f, 3.33f, ZonedDateTime.now())
    } yield ()
    ioAction.unsafeRunSync()
    there.was(
      2.times(ioTransport)
        .receive[DarkSkyResponse](any[DarkSkyRequest], eqTo("host"), eqTo("key"), eqTo(1.seconds), eqTo(true))(
          eqTo(implicitly)))
  }

  def e3 = {
    implicit val ioTransport = mock[Transport[IO]].defaultReturn(ioExampleResponse)
    val ioAction = for {
      client <- CreateDarkSky[IO].create("host", "key", 1.seconds, 2, 1).map(_.toOption.get)
      _      <- client.cachingTimeMachine(4.44f, 3.33f, ZonedDateTime.now())
      _      <- client.cachingTimeMachine(6.44f, 3.33f, ZonedDateTime.now())
      _      <- client.cachingTimeMachine(8.44f, 3.33f, ZonedDateTime.now())
      _      <- client.cachingTimeMachine(4.44f, 3.33f, ZonedDateTime.now())
    } yield ()
    ioAction.unsafeRunSync()
    there.was(
      4.times(ioTransport)
        .receive[DarkSkyResponse](any[DarkSkyRequest], eqTo("host"), eqTo("key"), eqTo(1.seconds), eqTo(true))(
          eqTo(implicitly)))
  }

  def e4 = {
    implicit val ioTransport = mock[Transport[IO]].defaultReturn(ioExampleResponse)
    val ioAction = for {
      client <- CreateDarkSky[IO].create("host", "key", 1.seconds, 1, 1).map(_.toOption.get)
      _      <- client.cachingTimeMachine(10.4f, 32.1f, ZonedDateTime.parse("2015-11-09T12:00:40Z"))
      _      <- client.cachingTimeMachine(10.1f, 32.312f, ZonedDateTime.parse("2015-11-09T00:06:47Z"))
      _      <- client.cachingTimeMachine(10.2f, 32.4f, ZonedDateTime.parse("2015-11-09T23:06:47Z"))
    } yield ()
    ioAction.unsafeRunSync()
    there.was(
      1.times(ioTransport)
        .receive[DarkSkyResponse](any[DarkSkyRequest], eqTo("host"), eqTo("key"), eqTo(1.seconds), eqTo(true))(
          eqTo(implicitly)))
  }

  def e5 = {
    implicit val ioTransport = mock[Transport[IO]].defaultReturn(ioExampleResponse)
    val ioAction = for {
      client <- CreateDarkSky[IO].create("host", "key", 1.seconds, 10, 2).map(_.toOption.get)
      _      <- client.cachingTimeMachine(10.8f, 32.1f, ZonedDateTime.parse("2015-11-09T12:00:40Z"))
      _      <- client.cachingTimeMachine(10.1f, 32.312f, ZonedDateTime.parse("2015-11-09T00:06:47Z"))
      _      <- client.cachingTimeMachine(10.2f, 32.4f, ZonedDateTime.parse("2015-11-09T23:06:47Z"))
    } yield ()
    ioAction.unsafeRunSync()
    there.was(
      2.times(ioTransport)
        .receive[DarkSkyResponse](any[DarkSkyRequest], eqTo("host"), eqTo("key"), eqTo(1.seconds), eqTo(true))(
          eqTo(implicitly)))
  }

  def e6 = {
    CreateDarkSky[IO].create("host", "KEY", 1.seconds, 10, geoPrecision = 0).unsafeRunSync() must beLeft.like {
      case InvalidConfigurationError(msg) => msg must be_==("geoPrecision must be greater than 0")
    }
  }

  def e7 = {
    CreateDarkSky[IO].create("host", "KEY", 1.seconds, cacheSize = 0, 10).unsafeRunSync() must beLeft.like {
      case InvalidConfigurationError(msg) => msg must be_==("cacheSize must be greater than 0")
    }
  }

}
