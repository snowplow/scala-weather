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

// Scala
import scala.concurrent.duration._

// cats
import cats.Id
import cats.effect.IO

// Joda
import org.joda.time.DateTime

// This library
import Errors._
import Requests._
import Responses._
import WeatherCache.{CacheKey, Position}

/**
 * Blocking OpenWeatherMap client with history (only) cache
 * Uses AsyncOwmClient under the hood, have same method set, which
 * return Weather instead of Future[Weather]
 *
 * WARNING. This client uses pro.openweathermap.org for data access,
 * It will not work with free OWM licenses.
 *
 * @param cacheSize amount of history requests storing in cache
 *                  it's better to store whole OWM packet (5000/50000/150000)
 *                  plus some space for errors (~1%)
 * @param geoPrecision nth part of 1 to which latitude and longitude will be rounded
 *                     stored in cache. For eg. coordinate 45.678 will be rounded to
 *                     values 46.0, 45.5, 45.7, 45.78 by geoPrecision 1,2,10,100 respectively
 *                     geoPrecision 1 will give ~60km infelicity if worst case; 2 ~30km etc
 * @param asyncClient instance of `OwmAsyncClient` which will do all underlying work
 * @param timeout timeout in seconds after which active request will be considered failed
 */
class OwmCacheClient(val cacheSize: Int, val geoPrecision: Int, asyncClient: OwmAsyncClient[IO], val timeout: Int)
    extends Client[Id]
    with WeatherCache[History] {

  private val requestTimeout = timeout.seconds

  def receive[W <: OwmResponse: Manifest](request: OwmRequest): Either[WeatherError, W] =
    await(request)

  /**
   * Search history in cache and if not found request and await it from server
   * and put to the cache. If timeout error was taken from cache, do request again
   *
   * @param latitude event's latitude
   * @param longitude event's longitude
   * @param timestamp event's timestamp
   * @return weather stamp immediately taken from cache or requested from server
   */
  def getCachedOrRequest(latitude: Float, longitude: Float, timestamp: Int): Either[WeatherError, Weather] = {
    val cacheKey = eventToCacheKey(timestamp, Position(latitude, longitude))
    cache.get(cacheKey) match {
      case Some(Right(cached)) =>
        cached.pickCloseIn(timestamp) // Cache hit
      case Some(Left(TimeoutError(_))) =>
        getAndCache(latitude, longitude, timestamp, cacheKey) // Retry if timeout
      case Some(Left(error)) =>
        Left(error) // Return error
      case None =>
        getAndCache(latitude, longitude, timestamp, cacheKey) // Make request
    }
  }

  /**
   * Overloaded `getCachedOrRequest` method with Joda DateTime instead of Unix epoch timestamp
   */
  def getCachedOrRequest(latitude: Float, longitude: Float, timestamp: DateTime): Either[WeatherError, Weather] = {
    val unixTime: Int = (timestamp.getMillis / 1000).toInt
    getCachedOrRequest(latitude, longitude, unixTime)
  }

  /**
   * Retry request to the server, put whole batch result to client's GLOBAL cache
   * and pick near to realTimestamp weather stamp
   *
   * @param latitude real event's latitude
   * @param longitude real event's longitude
   * @param realTimestamp real event's timestamp
   * @param cacheKey cache key to use for bucket
   * @return near weather stamp
   */
  private def getAndCache(latitude: Float,
                          longitude: Float,
                          realTimestamp: Timestamp,
                          cacheKey: CacheKey): Either[WeatherError, Weather] = {
    val response = historyByCoords(latitude, longitude, cacheKey.day, cacheKey.endOfDay, 24)
    cache.put(cacheKey, response)
    getWeatherStamp(response, realTimestamp)
  }

  /**
   * Get exact weather stamp from history batch-request (probably failed)
   * nearest to specified `timestamp`
   *
   * @param history history request with 0 or more weather stamps
   * @param timestamp timestamp
   * @return either error or weather stamp
   */
  private[openweather] def getWeatherStamp(history: Either[WeatherError, History],
                                           timestamp: Int): Either[WeatherError, Weather] =
    history.right.flatMap(_.pickCloseIn(timestamp))

  /**
   * Await for Future completed
   * Probably Await.result isn't the best way to get Future's result,
   * but it's fine for event enrichment, since it's sequental operation
   *
   * @param request request built by client method
   * @tparam W type of Weather request
   * @return either response or error in case of timeout
   */
  private def await[W <: OwmResponse: Manifest](request: OwmRequest): Either[WeatherError, W] =
    asyncClient
      .receive(request)
      .unsafeRunTimed(requestTimeout)
      .getOrElse(
        Left(TimeoutError(s"OpenWeatherMap Error: server didn't responded in $timeout seconds. Timeout"))
      )
}

/**
 * Companion object for OwmClient with default transport based on akka http
 */
object OwmCacheClient {

  /**
   * Create OwmCacheClient with singleton-placed (in-scala-weather) akka system
   */
  def apply(appId: String,
            cacheSize: Int    = 5100,
            geoPrecision: Int = 1,
            host: String      = "pro.openweathermap.org",
            timeout: Int      = 5): OwmCacheClient =
    new OwmCacheClient(cacheSize, geoPrecision, OwmAsyncClient(appId, new HttpTransport[IO](host)), timeout)

  /**
   * Create OwmCacheClient with underlying async client
   */
  // TODO: think about hardcoding `IO` here
  def apply(cacheSize: Int, geoPrecision: Int, asyncClient: OwmAsyncClient[IO], timeout: Int): OwmCacheClient =
    new OwmCacheClient(cacheSize, geoPrecision, asyncClient, timeout)

  /**
   * Create OwmCacheClient with underlying transport (probably another actor system)
   */
  // TODO: think about hardcoding `IO` here
  def apply(appId: String,
            cacheSize: Int,
            geoPrecision: Int,
            transport: HttpAsyncTransport[IO],
            timeout: Int): OwmCacheClient =
    new OwmCacheClient(cacheSize, geoPrecision, OwmAsyncClient(appId, transport), timeout)
}
