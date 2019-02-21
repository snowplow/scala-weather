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

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import cats.effect.{Concurrent, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.monadError._
import com.snowplowanalytics.lrumap.LruMap

import Cache.CacheKey
import errors.{InvalidConfigurationError, WeatherError}
import responses.History

object OpenWeatherMap {

  /**
   * Create `OwmClient` with specified underlying `Transport`
   * @param transport instance of `Transport` which will do the actual sending of data
   */
  private[openweather] def basicClient[F[_]: Sync](transport: Transport[F]): OwmClient[F] =
    new OwmClient[F](transport)

  /**
   * Create `OwmClient` with `HttpTransport` instance
   * @param appId API key from OpenWeatherMap
   * @param apiHost URL to the OpenWeatherMap API endpoints
   * @param ssl whether to use https
   */
  def basicClient[F[_]: Sync](
    appId: String,
    apiHost: String = "api.openweathermap.org",
    ssl: Boolean    = true
  ): OwmClient[F] = basicClient(new HttpTransport[F](apiHost, appId, ssl))

  /**
   * Create `OwmCacheClient` with `TimeoutHttpTransport` instance
   * @param appId API key from OpenWeatherMap
   * @param cacheSize amount of history requests storing in cache
   *                  it's better to store whole OWM packet (5000/50000/150000)
   *                  plus some space for errors (~1%)
   * @param geoPrecision nth part of 1 to which latitude and longitude will be rounded
   *                     stored in cache. For eg. coordinate 45.678 will be rounded to
   *                     values 46.0, 45.5, 45.7, 45.78 by geoPrecision 1,2,10,100 respectively
   *                     geoPrecision 1 will give ~60km infelicity if worst case; 2 ~30km etc
   * @param host URL to the OpenWeatherMap API endpoints
   * @param timeout time after which active request will be considered failed
   * @param ssl whether to use https
   */
  def cacheClient[F[_]: Concurrent](
    appId: String,
    cacheSize: Int          = 5100,
    geoPrecision: Int       = 1,
    host: String            = "history.openweathermap.org",
    timeout: FiniteDuration = 5.seconds,
    ssl: Boolean            = true
  )(implicit executionContext: ExecutionContext): F[OwmCacheClient[F]] =
    cacheClient(cacheSize, geoPrecision, new TimeoutHttpTransport[F](host, appId, timeout, ssl))

  /**
   * Create `OwmCacheClient` with specified underlying `Transport`
   * @param cacheSize amount of history requests storing in cache
   *                  it's better to store whole OWM packet (5000/50000/150000)
   *                  plus some space for errors (~1%)
   * @param geoPrecision nth part of 1 to which latitude and longitude will be rounded
   *                     stored in cache. For eg. coordinate 45.678 will be rounded to
   *                     values 46.0, 45.5, 45.7, 45.78 by geoPrecision 1,2,10,100 respectively
   *                     geoPrecision 1 will give ~60km infelicity if worst case; 2 ~30km etc
   * @param transport instance of `Transport` which will do the actual sending of data
   */
  private[openweather] def cacheClient[F[_]: Concurrent](
    cacheSize: Int,
    geoPrecision: Int,
    transport: Transport[F]
  ): F[OwmCacheClient[F]] =
    Concurrent[F].unit
      .ensure(InvalidConfigurationError("geoPrecision must be greater than 0"))(_ => geoPrecision > 0)
      .ensure(InvalidConfigurationError("cacheSize must be greater than 0"))(_ => cacheSize > 0)
      .flatMap(_ => LruMap.create[F, CacheKey, Either[WeatherError, History]](cacheSize))
      .map(lruMap => new Cache(lruMap, geoPrecision))
      .map(cache => new OwmCacheClient(cache, transport))

}
