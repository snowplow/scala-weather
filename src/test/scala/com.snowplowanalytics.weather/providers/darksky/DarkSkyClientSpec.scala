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

// java
import java.time.ZonedDateTime

// cats
import cats.effect.IO

// circe
import io.circe.Decoder

// tests
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.specs2._
import org.specs2.mock.Mockito

// This library
import com.snowplowanalytics.weather.Errors.WeatherError
import com.snowplowanalytics.weather.providers.darksky.Requests.DarkSkyRequest
import com.snowplowanalytics.weather.providers.darksky.Responses.DarkSkyResponse

class DarkSkyClientSpec extends Specification with Mockito {
  def is = s2"""

  DarkSky Client API test

    Client generates correct requests with DateTime $e1

  """

  val exampleResponse: IO[Right[WeatherError, DarkSkyResponse]] =
    IO.pure(Right(DarkSkyResponse(0f, 0f, "", None, None, None, None, None, None)))

  def e1 = {
    val transportMock =
      mock[Transport[IO]].receive(any[WeatherRequest])(any[Decoder[WeatherResponse]]).returns(exampleResponse)
    val client = DarkSky.basicClient[IO](transportMock)

    val expectedRequest = DarkSkyRequest(32.12f, 15.2f, Some(1449774761)) // "2015-12-10T19:12:41+00:00"
    client.timeMachine(32.12f, 15.2f, ZonedDateTime.parse("2015-12-11T02:12:41.000+07:00"))

    there.was(1.times(client.transport).receive(eqTo(expectedRequest))(any()))
  }

}
