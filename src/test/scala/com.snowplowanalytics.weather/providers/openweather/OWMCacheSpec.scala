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
package providers.openweather

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import cats.effect.{ContextShift, IO}
import cats.syntax.either._
import io.circe.Decoder
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.specs2.Specification
import org.specs2.mock.Mockito

import errors.{InvalidConfigurationError, TimeoutError}
import requests.OwmRequest
import responses.History

// Mock transport which returns predefined responses
class OWMCacheSpec extends Specification with Mockito {
  def is =
    s2"""

  Test cache specification

    do not bother server on identical requests $e1
    do not bother server on similar requests (example from README) $e4
    retry request on timeout error $e2
    check geoPrecision $e5
    make requests again after full cache $e3
    be left for invalid precision $e6
    be left for invalid cache size $e7
  """

  implicit val timer                = IO.timer(global)
  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  val ioEmptyHistoryResponse: IO[Either[TimeoutError, History]] =
    IO.pure(History(BigInt(100), "0", List()).asRight)
  val ioTimeoutErrorResponse: IO[Either[TimeoutError, History]] =
    IO.pure(TimeoutError("java.util.concurrent.TimeoutException: Futures timed out after [1 second]").asLeft)

  def e1 = {
    implicit val ioTransport = mock[Transport[IO]].defaultReturn(ioEmptyHistoryResponse)
    val ioAction = for {
      client <- CreateOWM[IO].create("host", "key", 1.seconds, true, 2, 1).map(_.toOption.get)
      _      <- client.cachingHistoryByCoords(4.44f, 3.33f, 100)
      _      <- client.cachingHistoryByCoords(4.44f, 3.33f, 100)
      _      <- client.cachingHistoryByCoords(4.44f, 3.33f, 100)
    } yield ()
    ioAction.unsafeRunSync()
    there.was(
      1.times(ioTransport)
        .receive[History](any[OwmRequest], eqTo("host"), eqTo("key"), eqTo(1.seconds), eqTo(true))(eqTo(implicitly))
    )

  }

  def e2 = {
    implicit val ioTransport = mock[Transport[IO]]
    ioTransport
      .receive[History](any[OwmRequest], any[String], any[String], any[FiniteDuration], any[Boolean])(
        any[Decoder[History]]
      )
      .returns(ioTimeoutErrorResponse)
      .thenReturn(ioEmptyHistoryResponse)
    val ioAction = for {
      client <- CreateOWM[IO].create("host", "key", 1.seconds, true, 2, 1).map(_.toOption.get)
      _      <- client.cachingHistoryByCoords(4.44f, 3.33f, 100)
      _      <- client.cachingHistoryByCoords(4.44f, 3.33f, 100)
    } yield ()
    ioAction.unsafeRunSync()
    there.was(
      2.times(ioTransport)
        .receive[History](any[OwmRequest], eqTo("host"), eqTo("key"), eqTo(1.seconds), eqTo(true))(eqTo(implicitly))
    )
  }

  def e3 = {
    implicit val ioTransport = mock[Transport[IO]].defaultReturn(ioEmptyHistoryResponse)
    val ioAction = for {
      client <- CreateOWM[IO].create("host", "key", 1.seconds, true, 2, 1).map(_.toOption.get)
      _      <- client.cachingHistoryByCoords(4.44f, 3.33f, 100)
      _      <- client.cachingHistoryByCoords(6.44f, 3.33f, 100)
      _      <- client.cachingHistoryByCoords(8.44f, 3.33f, 100)
      _      <- client.cachingHistoryByCoords(4.44f, 3.33f, 100)
    } yield ()
    ioAction.unsafeRunSync()
    there.was(
      4.times(ioTransport)
        .receive[History](any[OwmRequest], eqTo("host"), eqTo("key"), eqTo(1.seconds), eqTo(true))(eqTo(implicitly))
    )
  }

  def e4 = {
    implicit val ioTransport = mock[Transport[IO]].defaultReturn(ioEmptyHistoryResponse)
    val ioAction = for {
      client <- CreateOWM[IO].create("host", "key", 1.seconds, true, 10, 1).map(_.toOption.get)
      _      <- client.cachingHistoryByCoords(10.4f, 32.1f, 1447070440) // Nov 9 12:00:40 2015 GMT
      _      <- client.cachingHistoryByCoords(10.1f, 32.312f, 1447063607) // Nov 9 10:06:47 2015 GMT
      _      <- client.cachingHistoryByCoords(10.2f, 32.4f, 1447096857) // Nov 9 19:20:57 2015 GMT
    } yield ()
    ioAction.unsafeRunSync()
    there.was(
      1.times(ioTransport)
        .receive[History](any[OwmRequest], eqTo("host"), eqTo("key"), eqTo(1.seconds), eqTo(true))(eqTo(implicitly))
    )
  }

  def e5 = {
    implicit val ioTransport = mock[Transport[IO]].defaultReturn(ioEmptyHistoryResponse)
    val ioAction = for {
      client <- CreateOWM[IO].create("host", "key", 1.seconds, true, 10, 2).map(_.toOption.get)
      _      <- client.cachingHistoryByCoords(10.8f, 32.1f, 1447070440) // Nov 9 12:00:40 2015 GMT
      _      <- client.cachingHistoryByCoords(10.1f, 32.312f, 1447063607) // Nov 9 10:06:47 2015 GMT
      _      <- client.cachingHistoryByCoords(10.2f, 32.4f, 1447096857) // Nov 9 19:20:57 2015 GMT
    } yield ()
    ioAction.unsafeRunSync()
    there.was(
      2.times(ioTransport)
        .receive[History](any[OwmRequest], eqTo("host"), eqTo("key"), eqTo(1.seconds), eqTo(true))(eqTo(implicitly))
    )
  }

  def e6 =
    CreateOWM[IO].create("host", "KEY", 1.seconds, true, 10, geoPrecision = 0).unsafeRunSync() must beLeft.like {
      case InvalidConfigurationError(msg) => msg must be_==("geoPrecision must be greater than 0")
    }

  def e7 =
    CreateOWM[IO].create("host", "KEY", 1.seconds, true, cacheSize = 0, 10).unsafeRunSync() must beLeft.like {
      case InvalidConfigurationError(msg) => msg must be_==("cacheSize must be greater than 0")
    }

}
