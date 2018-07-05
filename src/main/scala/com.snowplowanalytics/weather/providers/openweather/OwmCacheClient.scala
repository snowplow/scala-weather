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
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

// cats
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.effect.{Concurrent, Timer}

// circe
import io.circe.Decoder

// Joda
import org.joda.time.DateTime

// This library
import Errors._
import Requests._
import Responses._
import WeatherCache.{CacheKey, Position}

/**
 * Blocking OpenWeatherMap client with history (only) cache
 * Uses AsyncOwmClient under the hood, have same method set, but also uses timeouts
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
 * @param requestTimeout timeout after which active request will be considered failed
 */
class OwmCacheClient[F[_]: Concurrent](
  val cacheSize: Int,
  val geoPrecision: Int,
  asyncClient: OwmAsyncClient[F],
  val requestTimeout: FiniteDuration)(implicit val executionContext: ExecutionContext)
    extends Client[F]
    with WeatherCache[History] {

  private val timer: Timer[F] = Timer.derive[F]

  def receive[W <: OwmResponse: Decoder](request: OwmRequest): F[Either[WeatherError, W]] =
    timeout(asyncClient.receive(request), requestTimeout)

  /**
   * Search history in cache and if not found request and await it from server
   * and put to the cache. If timeout error was taken from cache, do request again
   *
   * @param latitude event's latitude
   * @param longitude event's longitude
   * @param timestamp event's timestamp
   * @return weather stamp immediately taken from cache or requested from server
   */
  def getCachedOrRequest(latitude: Float, longitude: Float, timestamp: Int): F[Either[WeatherError, Weather]] = {
    val cacheKey = eventToCacheKey(timestamp, Position(latitude, longitude))
    Concurrent[F].delay(cache.get(cacheKey)).flatMap {
      case Some(Right(cached)) =>
        Concurrent[F].delay(cached.pickCloseIn(timestamp)) // Cache hit
      case Some(Left(TimeoutError(_))) =>
        getAndCache(latitude, longitude, timestamp, cacheKey)
      case Some(Left(error)) =>
        Concurrent[F].point(Left(error))
      case None =>
        getAndCache(latitude, longitude, timestamp, cacheKey)
    }
  }

  /**
   * Overloaded `getCachedOrRequest` method with Joda DateTime instead of Unix epoch timestamp
   */
  def getCachedOrRequest(latitude: Float, longitude: Float, timestamp: DateTime): F[Either[WeatherError, Weather]] = {
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
                          cacheKey: CacheKey): F[Either[WeatherError, Weather]] =
    historyByCoords(latitude, longitude, cacheKey.day, cacheKey.endOfDay, 24)
      .map { response =>
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
   * Apply timeout to the `operation` parameter. To be replaced by Concurrent[F].timeout in cats-effect 1.0.0
   *
   * @param operation The operation we want to run with a timeout
   * @param duration Duration to timeout after
   * @return either Left(TimeoutError) or a result of the operation, wrapped in F
   */
  private def timeout[W](operation: F[Either[WeatherError, W]], duration: FiniteDuration): F[Either[WeatherError, W]] =
    Concurrent[F]
      .race(operation, timer.sleep(duration))
      .map {
        case Left(value) => value
        case Right(_)    => Left(TimeoutError(s"OpenWeatherMap request timed out after ${duration.toSeconds} seconds"))
      }
}

/**
 * Companion object for OwmClient with default transport based on akka http
 */
object OwmCacheClient {

  /**
   * Create OwmCacheClient with singleton-placed (in-scala-weather) akka system
   */
  def apply[F[_]: Concurrent](
    appId: String,
    cacheSize: Int          = 5100,
    geoPrecision: Int       = 1,
    host: String            = "pro.openweathermap.org",
    timeout: FiniteDuration = 5.seconds)(implicit executionContext: ExecutionContext): OwmCacheClient[F] =
    new OwmCacheClient(cacheSize, geoPrecision, new OwmAsyncClient(appId, host), timeout)

  /**
   * Create OwmCacheClient with underlying async client
   */
  def apply[F[_]: Concurrent](cacheSize: Int,
                              geoPrecision: Int,
                              asyncClient: OwmAsyncClient[F],
                              timeout: FiniteDuration)(implicit executionContext: ExecutionContext): OwmCacheClient[F] =
    new OwmCacheClient(cacheSize, geoPrecision, asyncClient, timeout)
}
