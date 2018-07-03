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

// json4s
import org.json4s.JValue

// This library
import Errors.WeatherError
import Requests.WeatherRequest

/**
 * Basic trait responsible for receiving data via HTTP
 * Also supposed to be responsible for establishing HTTP connection
 */
trait HttpAsyncTransport[F[_]] {

  /**
   * Build request and send it to the server and get response wrapped in effect type
   *
   * @param request helper for generating correct URI
   * @param appId API key
   * @return either service-error or ready-to-process JSON, wrapped in effect type
   */
  def getData(request: WeatherRequest, appId: String): F[Either[WeatherError, JValue]]
}
