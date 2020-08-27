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

import cats.{Id, Monad}
import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.either._
import com.snowplowanalytics.lrumap.CreateLruMap

import Cache.CacheKey
import errors.{InvalidConfigurationError, WeatherError}
import responses.DarkSkyResponse

sealed trait CreateDarkSky[F[_]] {

  /**
    * Create a `DarkSkyClient`
    * @param apiHost URL to the Dark Sky API endpoints
    * @param apiKey API key from Dark Sky
    * @return a DarkSkyClient
    */
  def create(apiHost: String, apiKey: String, timeout: FiniteDuration): DarkSkyClient[F]

  /**
    * Create a `DarkSkyCacheClient` capable of caching results
    * @param apiHost URL to the Dark Sky API endpoints
    * @param apiKey API key from Dark Sky
    * @param timeout time after which active request will be considered failed
    * @param cacheSize amount of responses stored in the cache
    * @param geoPrecision nth part of 1 to which latitude and longitude will be rounded
    * stored in cache. e.g. coordinate 45.678 will be rounded to values 46.0, 45.5, 45.7, 45.78 by
    * geoPrecision 1,2,10,100 respectively. geoPrecision 1 will give ~60km accuracy in the worst
    * case; 2 ~30km etc
    * @return either an InvalidConfigurationError or a DarkSkyCacheClient
    */
  def create(
    apiHost: String,
    apiKey: String,
    timeout: FiniteDuration,
    cacheSize: Int,
    geoPrecision: Int
  ): F[Either[InvalidConfigurationError, DarkSkyCacheClient[F]]]
}

object CreateDarkSky {
  def apply[F[_]](implicit ev: CreateDarkSky[F]): CreateDarkSky[F] = ev

  implicit def syncCreateDarkSky[F[_]: Sync: Transport](implicit
    CLM: CreateLruMap[F, CacheKey, Either[WeatherError, DarkSkyResponse]]
  ): CreateDarkSky[F] =
    new CreateDarkSky[F] {
      override def create(
        apiHost: String,
        apiKey: String,
        timeout: FiniteDuration
      ): DarkSkyClient[F] = new DarkSkyClient[F](apiHost, apiKey, timeout, ssl = true)
      override def create(
        apiHost: String,
        apiKey: String,
        timeout: FiniteDuration,
        cacheSize: Int,
        geoPrecision: Int
      ): F[Either[InvalidConfigurationError, DarkSkyCacheClient[F]]] =
        cacheClient[F](apiHost, apiKey, timeout, cacheSize, geoPrecision, ssl = true)
    }

  implicit def idCreateDarkSky(implicit T: Transport[Id]): CreateDarkSky[Id] =
    new CreateDarkSky[Id] {
      override def create(
        apiHost: String,
        apiKey: String,
        timeout: FiniteDuration
      ): DarkSkyClient[Id] = new DarkSkyClient[Id](apiHost, apiKey, timeout, ssl = true)
      override def create(
        apiHost: String,
        apiKey: String,
        timeout: FiniteDuration,
        cacheSize: Int,
        geoPrecision: Int
      ): Id[Either[InvalidConfigurationError, DarkSkyCacheClient[Id]]] =
        cacheClient[Id](apiHost, apiKey, timeout, cacheSize, geoPrecision, ssl = true)
    }

  private[darksky] def cacheClient[F[_]: Monad](
    apiHost: String,
    apiKey: String,
    timeout: FiniteDuration,
    cacheSize: Int,
    geoPrecision: Int,
    ssl: Boolean
  )(implicit
    CLM: CreateLruMap[F, CacheKey, Either[WeatherError, DarkSkyResponse]],
    T: Transport[F]
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
      client = new DarkSkyCacheClient[F](cache, apiHost, apiKey, timeout, ssl)
    } yield client).value
}
