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
package com.snowplowanalytics.weather.providers.openweather

import cats.data.NonEmptyList
import hammock.Uri

// This library
import com.snowplowanalytics.weather.WeatherRequest

private[weather] object Requests {

  sealed trait OwmRequest extends WeatherRequest {
    val endpoint: Option[String]
    val resource: String
    val parameters: Map[String, String]

    def constructQuery(baseUri: Uri, apiKey: String): Uri = {
      val versionedBaseUri = baseUri / "data" / "2.5"
      val uriWithPath      = endpoint.map(e => versionedBaseUri / e / resource).getOrElse(versionedBaseUri / resource)
      val params           = NonEmptyList.of("appid" -> apiKey) ++ parameters.toList

      uriWithPath ? params
    }

  }

  final case class OwmHistoryRequest(resource: String, parameters: Map[String, String]) extends OwmRequest {
    val endpoint = Some("history")
  }

  final case class OwmForecastRequest(resource: String, parameters: Map[String, String]) extends OwmRequest {
    val endpoint = Some("forecast")
  }

  final case class OwmCurrentRequest(resource: String, parameters: Map[String, String]) extends OwmRequest {
    val endpoint = None
  }
}
