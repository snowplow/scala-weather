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

// TODO: replace it with plain URIs
private[weather] object Requests {
  abstract sealed trait WeatherRequest {
    def constructQuery(appId: String): String
  }

  sealed trait OwmRequest extends WeatherRequest {
    val endpoint: Option[String]
    val resource: String
    val parameters: Map[String, String]

    /**
     * Construct URI for specific type of request and all other data
     *
     * @param appId API key
     * @return URI object ready to be sent
     */
    def constructQuery(appId: String): String = {
      val end         = endpoint.map(e => s"$e/$resource").getOrElse(s"$resource")
      val params      = parameters ++ Map("appid" -> appId)
      val queryString = params.map { case (a, b) => s"$a=$b" }.mkString("&")
      s"/data/2.5/$end?$queryString"
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
