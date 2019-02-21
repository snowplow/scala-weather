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

import java.time._

import cats.effect.IO
import org.specs2.Specification
import org.specs2.mock.Mockito
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.specs2.concurrent.ExecutionEnv

import requests.OwmHistoryRequest
import responses.History
import Cache._

class TimeCacheSpec(implicit val ec: ExecutionEnv) extends Specification with Mockito {
  def is = s2"""

  Testing cache keys

  Start day
    Day of 1447945977 is 19 Nov 2015                        $e1
    Key for 2018-06-01, 42.832, 32.1101                     $e2
    dayStartEpoch produces correct time                     $e3
    Key from next day in other timezone extracted correctly $e4
                                                   """

  private val precision          = 1
  private val nov19time          = Instant.ofEpochSecond(1447945977).atZone(ZoneOffset.UTC).toLocalDate
  private val nov19begin         = 1447891200
  private val newDayInKranoyarsk = ZonedDateTime.parse("2015-12-14T00:12:44.000+07:00")
  private val sunsetInKranoyarsk = ZonedDateTime.parse("2018-07-13T21:28:44.000+07:00")

  def e1 = Cache.dayStartEpoch(nov19time) must beEqualTo(nov19begin)
  def e2 =
    Cache.eventToCacheKey(ZonedDateTime.parse("2018-06-02T02:23:12+05:00"), Position(42.832f, 32.1101f), precision) must beEqualTo(
      CacheKey(LocalDate.of(2018, 6, 1), Position(43.0f, 32.0f)))

  def e3 = {
    val dateTime =
      LocalDateTime.ofEpochSecond(Cache.dayStartEpoch(sunsetInKranoyarsk.toLocalDate), 0, ZoneOffset.ofHours(0))
    dateTime.getHour must beEqualTo(0)
    dateTime.getMinute must beEqualTo(0)
  }

  def e4 = {
    val emptyHistoryResponse = IO.pure(Right(History(BigInt(100), "0", List())))
    val expectedRequest = OwmHistoryRequest(
      "city",
      Map(
        "lat"   -> "4.44",
        "lon"   -> "3.33",
        "cnt"   -> "24",
        "start" -> "1449964800", // "2015-12-13T00:00:00.000+00:00"
        "end"   -> "1450051200" // "2015-12-14T00:00:00.000+00:00"
      )
    )
    val transport = mock[Transport[IO]].defaultReturn(emptyHistoryResponse)
    val action = for {
      client <- OpenWeatherMap.cacheClient(2, 1, transport)
      _      <- client.cachingHistoryByCoords(4.44f, 3.33f, newDayInKranoyarsk)
    } yield ()
    action.unsafeRunSync()
    there.was(1.times(transport).receive[History](eqTo(expectedRequest))(eqTo(implicitly)))
  }

}
