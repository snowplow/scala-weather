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

import cats.Eval
import cats.effect.IO
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.specs2._
import org.specs2.mock.Mockito

import errors.WeatherError
import requests.DarkSkyRequest
import responses.DarkSkyResponse

class DarkSkyClientSpec extends Specification with Mockito {
  def is = s2"""

  DarkSky Client API test

    Client generates correct requests with DateTime $e1

  """

  val ioExampleResponse: IO[Either[WeatherError, DarkSkyResponse]] =
    IO.pure(Right(DarkSkyResponse(0f, 0f, "", None, None, None, None, None, None)))
  val evalExampleResponse: Eval[Either[WeatherError, DarkSkyResponse]] =
    Eval.now(Right(DarkSkyResponse(0f, 0f, "", None, None, None, None, None, None)))

  def e1 = {
    val expectedRequest = DarkSkyRequest(32.12f, 15.2f, Some(1449774761)) // "2015-12-10T19:12:41+00:00"

    val ioTransport: Transport[IO] = mock[Transport[IO]]
    ioTransport
      .receive[DarkSkyResponse](eqTo(DarkSkyRequest(0f, 0f)))(eqTo(implicitly))
      .returns(ioExampleResponse)
    val ioClient = DarkSky.basicClient[IO](ioTransport)
    ioClient.timeMachine(32.12f, 15.2f, ZonedDateTime.parse("2015-12-11T02:12:41.000+07:00"))
    there.was(1.times(ioClient.transport).receive[DarkSkyResponse](eqTo(expectedRequest))(eqTo(implicitly)))

    val evalTransport: Transport[Eval] = mock[Transport[Eval]]
    evalTransport
      .receive[DarkSkyResponse](eqTo(DarkSkyRequest(0f, 0f)))(eqTo(implicitly))
      .returns(evalExampleResponse)
    val evalClient = DarkSky.basicClient[Eval](evalTransport)
    evalClient.timeMachine(32.12f, 15.2f, ZonedDateTime.parse("2015-12-11T02:12:41.000+07:00"))
    there.was(
      1.times(evalClient.transport)
        .receive[DarkSkyResponse](eqTo(expectedRequest))(eqTo(implicitly)))
  }

}
