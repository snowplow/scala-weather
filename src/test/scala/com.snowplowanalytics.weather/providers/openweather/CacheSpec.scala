/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
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

import scalaz.\/

import scala.concurrent.Future

import org.json4s.JsonDSL._

import org.specs2.concurrent.ExecutionEnv
import org.specs2.Specification
import org.specs2.mock.Mockito
import org.specs2.matcher.DisjunctionMatchers

import Requests.{ OwmHistoryRequest => HR }

// Mock transport which returns predefined responses
class CacheSpec(implicit val ec: ExecutionEnv) extends Specification with Mockito with DisjunctionMatchers { def is = s2"""

  Test cache specification

    do not bother server on identical requests $e1
    do not bother server on similar requests (example from README) $e4
    check geoPrecision $e5
    retry request on timeout error $e2
    make requests again after full cache $e3

  """

  val emptyHistoryResponse = \/.right(("cnt", 0) ~ ("cod", "200") ~ ("list", Nil))

  def e1 = {
    val transport = mock[AkkaHttpTransport].defaultReturn(Future.successful(emptyHistoryResponse))
    val client = OwmCacheClient("KEY", 2, 1, transport, 5)
    client.getCachedOrRequest(4.44f, 3.33f, 100)
    client.getCachedOrRequest(4.44f, 3.33f, 100)
    client.getCachedOrRequest(4.44f, 3.33f, 100)
    there.was(1.times(transport).getData(any[HR], anyString))
  }

  def e2 = {
    val transport = mock[AkkaHttpTransport]
    transport.getData(HR("city", Map("end" -> "86400", "lon" -> "3.33", "cnt" -> "24", "start" -> "0", "lat" -> "4.44")), "KEY")
      .returns(Future.successful(\/.left(TimeoutError("java.util.concurrent.TimeoutException: Futures timed out after [1 second]"))))
      .thenReturns(Future.successful(emptyHistoryResponse))
    val client = OwmCacheClient("KEY", 2, 1, transport, 5)
    client.getCachedOrRequest(4.44f, 3.33f, 100)
    client.getCachedOrRequest(4.44f, 3.33f, 100)
    there.was(2.times(transport).getData(any[HR], anyString))
  }

  def e3 = {
    val transport = mock[AkkaHttpTransport].defaultReturn(Future.successful(emptyHistoryResponse))
    val client = OwmCacheClient("KEY", 2, 1, transport, 5)
    client.getCachedOrRequest(4.44f, 3.33f, 100)
    client.getCachedOrRequest(6.44f, 3.33f, 100)
    client.getCachedOrRequest(8.44f, 3.33f, 100)
    client.getCachedOrRequest(4.44f, 3.33f, 100)
    there.was(4.times(transport).getData(any[HR], anyString))
  }

  def e4 = {
    val transport = mock[AkkaHttpTransport].defaultReturn(Future.successful(emptyHistoryResponse))
    val client = OwmCacheClient("KEY", 10, 1, transport, 5)
    client.getCachedOrRequest(10.4f, 32.1f, 1447070440)   // Nov 9 12:00:40 2015 GMT
    client.getCachedOrRequest(10.1f, 32.312f, 1447063607) // Nov 9 10:06:47 2015 GMT
    client.getCachedOrRequest(10.2f, 32.4f, 1447096857)   // Nov 9 19:20:57 2015 GMT
    there.was(1.times(transport).getData(any[HR], anyString))
  }

  def e5 = {
    val transport = mock[AkkaHttpTransport].defaultReturn(Future.successful(emptyHistoryResponse))
    val client = OwmCacheClient("KEY", 10, 2, transport, 5)
    client.getCachedOrRequest(10.8f, 32.1f, 1447070440)   // Nov 9 12:00:40 2015 GMT
    client.getCachedOrRequest(10.1f, 32.312f, 1447063607) // Nov 9 10:06:47 2015 GMT
    client.getCachedOrRequest(10.2f, 32.4f, 1447096857)   // Nov 9 19:20:57 2015 GMT
    there.was(2.times(transport).getData(any[HR], anyString))
  }
}
