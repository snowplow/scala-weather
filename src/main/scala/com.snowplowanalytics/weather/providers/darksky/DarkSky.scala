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

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import cats.effect.{Concurrent, Sync, Timer}
import cats.syntax.functor._
import cats.syntax.monadError._
import cats.syntax.flatMap._
import com.snowplowanalytics.lrumap.CreateLruMap

import Cache.CacheKey
import errors.{InvalidConfigurationError, WeatherError}
import responses.DarkSkyResponse

object DarkSky {

  private[darksky] def basicClient[F[_]: Sync](transport: Transport[F]): DarkSkyClient[F] =
    new DarkSkyClient(transport)

  /**
   * Create `DarkSkyClient` with underlying `HttpTransport` instance
   * @param apiKey API key from Dark Sky
   * @param apiHost URL to the Dark Sky API endpoints
   */
  def basicClient[F[_]: Sync](
    apiKey: String,
    apiHost: String = "api.darksky.net/forecast"
  ): DarkSkyClient[F] =
    basicClient(new HttpTransport[F](apiHost, apiKey, ssl = true))

  private[darksky] def cacheClient[F[_]: Concurrent](
    cacheSize: Int,
    geoPrecision: Int,
    transport: Transport[F]
  )(
    implicit CLM: CreateLruMap[F, CacheKey, Either[WeatherError, DarkSkyResponse]]
  ): F[DarkSkyCacheClient[F]] =
    Concurrent[F].unit
      .ensure(InvalidConfigurationError("geoPrecision must be greater than 0"))(_ => geoPrecision > 0)
      .ensure(InvalidConfigurationError("cacheSize must be greater than 0"))(_ => cacheSize > 0)
      .flatMap(_ => Cache.init(cacheSize, geoPrecision))
      .map(cache => new DarkSkyCacheClient(cache, transport))

  /**
   * Create `DarkSkyCacheClient` with `TimeoutHttpTransport` instance
   * @param appId API key from Dark Sky
   * @param cacheSize amount of responses stored in the cache
   * @param geoPrecision nth part of 1 to which latitude and longitude will be rounded
   *                     stored in cache. For eg. coordinate 45.678 will be rounded to
   *                     values 46.0, 45.5, 45.7, 45.78 by geoPrecision 1,2,10,100 respectively
   *                     geoPrecision 1 will give ~60km infelicity if worst case; 2 ~30km etc
   * @param host URL to the Dark Sky API endpoints
   * @param timeout time after which active request will be considered failed
   */
  def cacheClient[F[_]: Concurrent: Timer](
    appId: String,
    cacheSize: Int          = 5100,
    geoPrecision: Int       = 1,
    host: String            = "api.darksky.net/forecast",
    timeout: FiniteDuration = 5.seconds
  )(implicit executionContext: ExecutionContext): F[DarkSkyCacheClient[F]] =
    cacheClient(cacheSize, geoPrecision, new TimeoutHttpTransport[F](host, appId, timeout, ssl = true))

}
