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

import io.circe.Decoder

import errors.WeatherError
import model._

trait Transport[F[_]] {

  /**
   * Main client logic for Request => Response function,
   * where Response is wrapped in tparam `F`
   *
   * @param request request built by client method
   * @tparam W type of weather response to extract
   * @return extracted either error or weather wrapped in `F`
   */
  def receive[W <: WeatherResponse: Decoder](request: WeatherRequest): F[Either[WeatherError, W]]

}
