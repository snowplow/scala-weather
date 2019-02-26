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
package providers.openweather

import scala.concurrent.duration._

import cats.{Eval, Monad}
import cats.data.EitherT
import cats.effect.{Concurrent, Sync, Timer}
import cats.syntax.either._
import com.snowplowanalytics.lrumap.CreateLruMap

import Cache.CacheKey
import errors.{InvalidConfigurationError, WeatherError}
import responses.History

object OpenWeatherMap {

  /**
   * Create a `OwmClient` with an underlying `Transport` instance
   * @param appId API key from OpenWeatherMap
   * @param apiHost URL to the OpenWeatherMap API endpoints
   * @param ssl whether to use https
   * @return a Sync DarkSkyClient
   */
  def basicClient[F[_]: Sync](
    appId: String,
    apiHost: String = "api.openweathermap.org",
    ssl: Boolean
  ): OwmClient[F] = basicClient(Transport.httpTransport[F](apiHost, appId, ssl))

  /**
   * Create an unsafe `OwmClient` with an underlying `Transport` instance
   * @param appId API key from OpenWeatherMap
   * @param apiHost URL to the OpenWeatherMap API endpoints
   * @param ssl whether to use https
   * @return an Eval DarkSkyClient
   */
  def unsafeBasicClient(
    appId: String,
    apiHost: String = "api.openweathermap.org",
    ssl: Boolean
  ): OwmClient[Eval] = basicClient(Transport.unsafeHttpTransport(apiHost, appId, ssl))

  private[openweather] def basicClient[F[_]](transport: Transport[F]): OwmClient[F] =
    new OwmClient(transport)

  /**
   * Create a `OwmCacheClient` with a `Transport` instance capable of timeouts
   * @param appId API key from OpenWeatherMap
   * @param cacheSize amount of history requests storing in cache
   * it's better to store whole OWM packet (5000/50000/150000) plus some space for errors (~1%)
   * @param geoPrecision nth part of 1 to which latitude and longitude will be rounded
   * stored in cache. e.g. coordinate 45.678 will be rounded to values 46.0, 45.5, 45.7, 45.78 by
   * geoPrecision 1,2,10,100 respectively. geoPrecision 1 will give ~60km infelicity in the worst
   * case; 2 ~30km etc
   * @param host URL to the OpenWeatherMap API endpoints
   * @param timeout time after which active request will be considered failed
   * @param ssl whether to use https
   * @return either an InvalidConfigurationError or a OwmCacheClient in a Sync
   */
  def cacheClient[F[_]: Concurrent: Timer](
    appId: String,
    cacheSize: Int          = 5100,
    geoPrecision: Int       = 1,
    host: String            = "history.openweathermap.org",
    timeout: FiniteDuration = 5.seconds,
    ssl: Boolean            = true
  ): F[Either[InvalidConfigurationError, OwmCacheClient[F]]] =
    cacheClient(
      cacheSize,
      geoPrecision,
      Transport.timeoutHttpTransport[F](host, appId, timeout, ssl)
    )

  /**
   * Create an unsafe `OwmCacheClient` with a `Transport` instance capable of timeouts
   * @param appId API key from OpenWeatherMap
   * @param cacheSize amount of history requests storing in cache
   * it's better to store whole OWM packet (5000/50000/150000) plus some space for errors (~1%)
   * @param geoPrecision nth part of 1 to which latitude and longitude will be rounded
   * stored in cache. e.g. coordinate 45.678 will be rounded to values 46.0, 45.5, 45.7, 45.78 by
   * geoPrecision 1,2,10,100 respectively. geoPrecision 1 will give ~60km infelicity in the worst
   * case; 2 ~30km etc
   * @param host URL to the OpenWeatherMap API endpoints
   * @param timeout time after which active request will be considered failed
   * @param ssl whether to use https
   * @return either an InvalidConfigurationError or a OwmCacheClient in an Eval
   */
  def unsafeCacheClient(
    appId: String,
    cacheSize: Int          = 5100,
    geoPrecision: Int       = 1,
    host: String            = "history.openweathermap.org",
    timeout: FiniteDuration = 5.seconds,
    ssl: Boolean            = true
  ): Eval[Either[InvalidConfigurationError, OwmCacheClient[Eval]]] =
    cacheClient(
      cacheSize,
      geoPrecision,
      Transport.unsafeTimeoutHttpTransport(host, appId, timeout, ssl)
    )

  private[openweather] def cacheClient[F[_]: Monad](
    cacheSize: Int,
    geoPrecision: Int,
    transport: Transport[F]
  )(
    implicit CLM: CreateLruMap[F, CacheKey, Either[WeatherError, History]]
  ): F[Either[InvalidConfigurationError, OwmCacheClient[F]]] =
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
      client = new OwmCacheClient[F](cache, transport)
    } yield client).value

}
