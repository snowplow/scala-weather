/*
 * Copyright (c) 2015-2019 Snowplow Analytics Ltd. All rights reserved.
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

import scala.concurrent.duration._

import cats.{Eval, Monad}
import cats.data.EitherT
import cats.effect.{Concurrent, Sync, Timer}
import cats.syntax.either._
import com.snowplowanalytics.lrumap.CreateLruMap

import Cache.CacheKey
import errors.{InvalidConfigurationError, WeatherError}
import responses.DarkSkyResponse

object DarkSky {

  /**
   * Create a `DarkSkyClient` with an underlying `Transport` instance
   * @param apiKey API key from Dark Sky
   * @param apiHost URL to the Dark Sky API endpoints
   * @return a Sync DarkSkyClient
   */
  def basicClient[F[_]: Sync](
    apiKey: String,
    apiHost: String = "api.darksky.net/forecast"
  ): DarkSkyClient[F] =
    basicClient(Transport.httpTransport[F](apiHost, apiKey, ssl = true))

  /**
   * Create an unsafe `DarkSkyClient` with an underlying `Transport` instance
   * @param apiKey API key from Dark Sky
   * @param apiHost URL to the Dark Sky API endpoints
   * @return an Eval DarkSkyClient
   */
  def unsafeBasicClient(
    apiKey: String,
    apiHost: String = "api.darksky.net/forecast"
  ): DarkSkyClient[Eval] =
    basicClient(Transport.unsafeHttpTransport(apiHost, apiKey, ssl = true))

  private[darksky] def basicClient[F[_]](transport: Transport[F]): DarkSkyClient[F] =
    new DarkSkyClient(transport)

  /**
   * Create a `DarkSkyCacheClient` with a `Transport` instance capable of timeouts
   * @param appId API key from Dark Sky
   * @param cacheSize amount of responses stored in the cache
   * @param geoPrecision nth part of 1 to which latitude and longitude will be rounded
   * stored in cache. e.g. coordinate 45.678 will be rounded to values 46.0, 45.5, 45.7, 45.78 by
   * geoPrecision 1,2,10,100 respectively. geoPrecision 1 will give ~60km accuracy in the worst
   * case; 2 ~30km etc
   * @param host URL to the Dark Sky API endpoints
   * @param timeout time after which active request will be considered failed
   * @return either an InvalidConfigurationError or a DarkSkyCacheClient in a Sync
   */
  def cacheClient[F[_]: Concurrent: Timer](
    appId: String,
    cacheSize: Int          = 5100,
    geoPrecision: Int       = 1,
    host: String            = "api.darksky.net/forecast",
    timeout: FiniteDuration = 5.seconds
  ): F[Either[InvalidConfigurationError, DarkSkyCacheClient[F]]] =
    cacheClient(
      cacheSize,
      geoPrecision,
      Transport.timeoutHttpTransport[F](host, appId, timeout, ssl = true)
    )

  /**
   * Create an unsafe `DarkSkyCacheClient` with a `Transport` instance capable of timeouts
   * @param appId API key from Dark Sky
   * @param cacheSize amount of responses stored in the cache
   * @param geoPrecision nth part of 1 to which latitude and longitude will be rounded
   * stored in cache. e.g. coordinate 45.678 will be rounded to values 46.0, 45.5, 45.7, 45.78 by
   * geoPrecision 1,2,10,100 respectively. geoPrecision 1 will give ~60km accuracy in the worst
   * case; 2 ~30km etc
   * @param host URL to the Dark Sky API endpoints
   * @param timeout time after which active request will be considered failed
   * @return either an InvalidConfigurationError or a DarkSkyCacheClient in an Eval
   */
  def unsafeCacheClient(
    appId: String,
    cacheSize: Int          = 5100,
    geoPrecision: Int       = 1,
    host: String            = "api.darksky.net/forecast",
    timeout: FiniteDuration = 5.seconds
  ): Eval[Either[InvalidConfigurationError, DarkSkyCacheClient[Eval]]] =
    cacheClient(
      cacheSize,
      geoPrecision,
      Transport.unsafeTimeoutHttpTransport(host, appId, timeout, ssl = true)
    )

  private[darksky] def cacheClient[F[_]: Monad](
    cacheSize: Int,
    geoPrecision: Int,
    transport: Transport[F]
  )(
    implicit CLM: CreateLruMap[F, CacheKey, Either[WeatherError, DarkSkyResponse]]
  ): F[Either[InvalidConfigurationError, DarkSkyCacheClient[F]]] =
    (for {
      _ <- EitherT.fromEither[F] {
        ().asRight
          .filterOrElse(
            _ => geoPrecision > 0,
            InvalidConfigurationError("geoPrecision must be greater than 0")
          )
          .filterOrElse(
            _ => cacheSize > 0,
            InvalidConfigurationError("cacheSize must be greater than 0")
          )
      }
      cache <- EitherT.right[InvalidConfigurationError](Cache.init(cacheSize, geoPrecision))
      client = new DarkSkyCacheClient[F](cache, transport)
    } yield client).value

}
