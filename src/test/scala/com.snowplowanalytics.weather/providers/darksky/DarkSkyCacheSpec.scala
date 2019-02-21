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

import java.time.ZonedDateTime

import cats.effect.IO
import cats.syntax.either._
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.Specification
import org.specs2.mock.Mockito

import errors.{InvalidConfigurationError, TimeoutError}
import requests.DarkSkyRequest
import responses.DarkSkyResponse

// Mock transport which returns predefined responses
class DarkSkyCacheSpec(implicit val ec: ExecutionEnv) extends Specification with Mockito {
  def is =
    s2"""

  Test cache specification

    do not bother server on identical requests $e1
    do not bother server on similar requests $e4
    retry request on timeout error $e2
    check geoPrecision $e5
    make requests again after full cache $e3
    throw exception for invalid precision $e6
    throw exception for invalid cache size $e7
  """

  val exampleResponse: IO[Right[TimeoutError, DarkSkyResponse]] =
    IO.pure(Right(DarkSkyResponse(0f, 0f, "", None, None, None, None, None, None)))
  val timeoutErrorResponse: IO[Either[TimeoutError, DarkSkyResponse]] =
    IO.pure(TimeoutError("java.util.concurrent.TimeoutException: Futures timed out after [1 second]").asLeft)

  def e1 = {
    val transport = mock[Transport[IO]].defaultReturn(exampleResponse)
    val action = for {
      client <- DarkSky.cacheClient(1, 1, transport)
      _      <- client.cachingTimeMachine(4.44f, 3.33f, ZonedDateTime.now())
      _      <- client.cachingTimeMachine(4.44f, 3.33f, ZonedDateTime.now())
      _      <- client.cachingTimeMachine(4.44f, 3.33f, ZonedDateTime.now())
    } yield ()
    action.unsafeRunSync()
    there.was(1.times(transport).receive[DarkSkyResponse](any[DarkSkyRequest])(eqTo(implicitly)))
  }

  def e2 = {
    val transport = mock[TimeoutHttpTransport[IO]]
    transport
      .receive[DarkSkyResponse](any[DarkSkyRequest])(eqTo(implicitly))
      .returns(timeoutErrorResponse)
      .thenReturn(exampleResponse)

    val action = for {
      client <- DarkSky.cacheClient(2, 1, transport)
      _      <- client.cachingTimeMachine(4.44f, 3.33f, ZonedDateTime.now())
      _      <- client.cachingTimeMachine(4.44f, 3.33f, ZonedDateTime.now())
    } yield ()
    action.unsafeRunSync()
    there.was(2.times(transport).receive[DarkSkyResponse](any[DarkSkyRequest])(eqTo(implicitly)))
  }

  def e3 = {
    val transport = mock[Transport[IO]].defaultReturn(exampleResponse)
    val action = for {
      client <- DarkSky.cacheClient(2, 1, transport)
      _      <- client.cachingTimeMachine(4.44f, 3.33f, ZonedDateTime.now())
      _      <- client.cachingTimeMachine(6.44f, 3.33f, ZonedDateTime.now())
      _      <- client.cachingTimeMachine(8.44f, 3.33f, ZonedDateTime.now())
      _      <- client.cachingTimeMachine(4.44f, 3.33f, ZonedDateTime.now())
    } yield ()
    action.unsafeRunSync()
    there.was(4.times(transport).receive[DarkSkyResponse](any[DarkSkyRequest])(eqTo(implicitly)))
  }

  def e4 = {
    val transport = mock[Transport[IO]].defaultReturn(exampleResponse)
    val action = for {
      client <- DarkSky.cacheClient(1, 1, transport)
      _      <- client.cachingTimeMachine(10.4f, 32.1f, ZonedDateTime.parse("2015-11-09T12:00:40Z"))
      _      <- client.cachingTimeMachine(10.1f, 32.312f, ZonedDateTime.parse("2015-11-09T00:06:47Z"))
      _      <- client.cachingTimeMachine(10.2f, 32.4f, ZonedDateTime.parse("2015-11-09T23:06:47Z"))
    } yield ()
    action.unsafeRunSync()
    there.was(1.times(transport).receive[DarkSkyResponse](any[DarkSkyRequest])(eqTo(implicitly)))
  }

  def e5 = {
    val transport = mock[Transport[IO]].defaultReturn(exampleResponse)
    val action = for {
      client <- DarkSky.cacheClient(10, 2, transport)
      _      <- client.cachingTimeMachine(10.8f, 32.1f, ZonedDateTime.parse("2015-11-09T12:00:40Z"))
      _      <- client.cachingTimeMachine(10.1f, 32.312f, ZonedDateTime.parse("2015-11-09T00:06:47Z"))
      _      <- client.cachingTimeMachine(10.2f, 32.4f, ZonedDateTime.parse("2015-11-09T23:06:47Z"))
    } yield ()
    action.unsafeRunSync()
    there.was(2.times(transport).receive[DarkSkyResponse](any[DarkSkyRequest])(eqTo(implicitly)))
  }

  def e6 =
    DarkSky.cacheClient[IO]("KEY", geoPrecision = 0).unsafeRunSync() must throwA[InvalidConfigurationError]

  def e7 =
    DarkSky.cacheClient[IO]("KEY", cacheSize = 0).unsafeRunSync() must throwA[InvalidConfigurationError]

}
