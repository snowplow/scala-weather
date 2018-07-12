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

import cats.effect.Sync

object DarkSky {

  private[darksky] def basicClient[F[_]: Sync](transport: Transport[F]): DarkSkyClient[F] =
    new DarkSkyClient(transport)

  /**
   * Create `DarkSkyClient` with underlying `HttpTransport` instance
   * @param apiKey API key from Dark Sky
   * @param apiHost URL to the Dark Sky API endpoints
   */
  def basicClient[F[_]: Sync](apiKey: String, apiHost: String = "api.darksky.net/forecast"): DarkSkyClient[F] =
    basicClient(new HttpTransport[F](apiHost, apiKey, ssl = true))

}
