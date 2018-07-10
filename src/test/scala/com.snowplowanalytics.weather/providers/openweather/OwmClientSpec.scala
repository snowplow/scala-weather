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

// joda
import org.joda.time.DateTime

// cats
import cats.effect.IO
import cats.syntax.either._

// circe
import io.circe.Decoder

// tests
import org.specs2.concurrent.ExecutionEnv
import org.specs2.Specification
import org.specs2.mock.Mockito
import org.mockito.ArgumentMatchers.{eq => eqTo}

// this library
import Errors.WeatherError
import Requests.OwmHistoryRequest
import Responses.History

class OwmClientSpec(implicit val ec: ExecutionEnv) extends Specification with Mockito {
  def is = s2"""

  OWM Client API test

    Implicits for DateTime work as expected (without imports)   $e1
  """

  val emptyHistoryResponse = IO.pure(History(BigInt(100), "0", List()).asRight[WeatherError])

  def e1 = {
    val transport = mock[Transport[IO]]
    transport.receive(any[WeatherRequest])(any[Decoder[WeatherResponse]]).returns(emptyHistoryResponse)
    val client = OpenWeatherMap.basicClient[IO](transport)
    val expectedRequest = OwmHistoryRequest(
      "city",
      Map(
        "lat"   -> "0.0",
        "lon"   -> "0.0",
        "start" -> "1449774761" // "2015-12-10T19:12:41+00:00"
      )
    )
    client.historyByCoords(0.00f, 0.00f, DateTime.parse("2015-12-11T02:12:41.000+07:00"))
    there.was(1.times(transport).receive(eqTo(expectedRequest))(any()))
  }

}
