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

// cats
import cats.effect.IO

// Java
import java.util.{Calendar, Date, TimeZone}

// Joda
import org.joda.time.DateTime

// Specs2
import org.specs2.Specification
import org.specs2.mock.Mockito
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.specs2.concurrent.ExecutionEnv

// This library
import Requests.OwmHistoryRequest
import Responses.History
import Cache._

class TimeCacheSpec(implicit val ec: ExecutionEnv) extends Specification with Mockito {
  def is = s2"""

  Testing cache keys

  Start day
    Day of 1447945977 is 19 Nov 2015                        $e1
    Key for 1445591292, 42.832, 32.1101                     $e2
    getStartOfDay ends with 0                               $e3
    Still 86400 seconds in day                              $e4
    Key from next day in other timezone extracted correctly $e5
                                                   """

  private val precision          = 1
  private val nov19time          = 1447945977
  private val nov19begin         = 1447891200
  private val newDayInKranoyarsk = DateTime.parse("2015-12-14T00:12:44.000+07:00")

  def e1 = Cache.getStartOfDay(nov19time) must beEqualTo(nov19begin)
  def e2 =
    Cache.eventToCacheKey(1445591292, Position(42.832f, 32.1101f), precision) must beEqualTo(
      CacheKey(1445558400, Position(43.0f, 32.0f)))

  def e3 = {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
    calendar.setTime(new Date(Cache.getStartOfDay(1445151725).toLong * 1000))
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    hour must beEqualTo(0)
  }

  def e4 = {
    val calendarStart = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
    val calendarEnd   = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
    val cacheKey      = Cache.eventToCacheKey(1445151725, Position(0.0f, 0.0f), precision)
    calendarStart.setTime(new Date(cacheKey.day.toLong * 1000))
    calendarEnd.setTime(new Date(cacheKey.endOfDay.toLong * 1000))
    val delta = calendarEnd.get(Calendar.DAY_OF_MONTH) - calendarStart.get(Calendar.DAY_OF_MONTH)
    delta must beEqualTo(1)
  }

  def e5 = {
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
    there.was(1.times(transport).receive(eqTo(expectedRequest))(any()))
  }

}
