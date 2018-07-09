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

// scala
import scala.concurrent.duration._

// cats
import cats.effect.IO
import cats.syntax.either._

// tests
import org.specs2.concurrent.ExecutionEnv
import org.specs2.Specification
import org.specs2.mock.Mockito
import org.specs2.matcher.DisjunctionMatchers

// This library
import Responses.History
import Errors.TimeoutError

// Mock transport which returns predefined responses
class CacheSpec(implicit val ec: ExecutionEnv) extends Specification with Mockito with DisjunctionMatchers {
  def is =
    s2"""

  Test cache specification

    do not bother server on identical requests $e1
    do not bother server on similar requests (example from README) $e4
    retry request on timeout error $e2
    check geoPrecision $e5
    make requests again after full cache $e3

  """

  val emptyHistoryResponse: IO[Either[TimeoutError, History]] = IO.pure(History(BigInt(100), "0", List()).asRight)
  val timeoutErrorResponse: IO[Either[TimeoutError, History]] =
    IO.pure(TimeoutError("java.util.concurrent.TimeoutException: Futures timed out after [1 second]").asLeft)

  def e1 = {
    val asyncClient = mock[OwmClient[IO]].defaultReturn(emptyHistoryResponse)
    val client      = OwmCacheClient(2, 1, asyncClient, 5.seconds)
    val action = for {
      _ <- client.getCachedOrRequest(4.44f, 3.33f, 100)
      _ <- client.getCachedOrRequest(4.44f, 3.33f, 100)
      _ <- client.getCachedOrRequest(4.44f, 3.33f, 100)
    } yield ()
    action.unsafeRunSync()
    there.was(1.times(asyncClient).receive(any())(any()))
  }

  def e2 = {
    val asyncClient = mock[OwmClient[IO]]
    asyncClient
      .receive[History](any())(any())
      .returns(timeoutErrorResponse)
      .thenReturn(emptyHistoryResponse)

    val client = OwmCacheClient(2, 1, asyncClient, 5.seconds)
    val action = for {
      _ <- client.getCachedOrRequest(4.44f, 3.33f, 100)
      _ <- client.getCachedOrRequest(4.44f, 3.33f, 100)
    } yield ()
    action.unsafeRunSync()
    there.was(2.times(asyncClient).receive(any())(any()))
  }

  def e3 = {
    val asyncClient = mock[OwmClient[IO]].defaultReturn(emptyHistoryResponse)
    val client      = OwmCacheClient(2, 1, asyncClient, 5.seconds)
    val action = for {
      _ <- client.getCachedOrRequest(4.44f, 3.33f, 100)
      _ <- client.getCachedOrRequest(6.44f, 3.33f, 100)
      _ <- client.getCachedOrRequest(8.44f, 3.33f, 100)
      _ <- client.getCachedOrRequest(4.44f, 3.33f, 100)
    } yield ()
    action.unsafeRunSync()
    there.was(4.times(asyncClient).receive(any())(any()))
  }

  def e4 = {
    val asyncClient = mock[OwmClient[IO]].defaultReturn(emptyHistoryResponse)
    val client      = OwmCacheClient(10, 1, asyncClient, 5.seconds)
    val action = for {
      _ <- client.getCachedOrRequest(10.4f, 32.1f, 1447070440)   // Nov 9 12:00:40 2015 GMT
      _ <- client.getCachedOrRequest(10.1f, 32.312f, 1447063607) // Nov 9 10:06:47 2015 GMT
      _ <- client.getCachedOrRequest(10.2f, 32.4f, 1447096857)   // Nov 9 19:20:57 2015 GMT
    } yield ()
    action.unsafeRunSync()
    there.was(1.times(asyncClient).receive(any())(any()))
  }

  def e5 = {
    val asyncClient = mock[OwmClient[IO]].defaultReturn(emptyHistoryResponse)
    val client      = OwmCacheClient(10, 2, asyncClient, 5.seconds)
    val action = for {
      _ <- client.getCachedOrRequest(10.8f, 32.1f, 1447070440)   // Nov 9 12:00:40 2015 GMT
      _ <- client.getCachedOrRequest(10.1f, 32.312f, 1447063607) // Nov 9 10:06:47 2015 GMT
      _ <- client.getCachedOrRequest(10.2f, 32.4f, 1447096857)   // Nov 9 19:20:57 2015 GMT
    } yield ()
    action.unsafeRunSync()
    there.was(2.times(asyncClient).receive(any())(any()))
  }
}
