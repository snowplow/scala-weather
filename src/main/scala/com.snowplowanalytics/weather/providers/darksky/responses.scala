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

import io.circe.Decoder
import io.circe.generic.JsonCodec
import io.circe.generic.extras.semiauto.deriveUnwrappedDecoder

import model.WeatherResponse

object responses {

  @JsonCodec(decodeOnly = true)
  final case class DarkSkyResponse(
    latitude: Float,
    longitude: Float,
    timezone: String,
    currently: Option[DataPoint],
    minutely: Option[DataBlock],
    hourly: Option[DataBlock],
    daily: Option[DataBlock],
    alerts: Option[List[Alert]],
    flags: Option[Flags]
  ) extends WeatherResponse

  @JsonCodec(decodeOnly = true)
  final case class DataBlock(data: List[DataPoint], summary: Option[String], icon: Option[String])

  @JsonCodec(decodeOnly = true)
  final case class Alert(
    description: String,
    expires: Long,
    regions: List[String],
    severity: String,
    time: Long,
    title: String,
    uri: String
  )

  final case class Flags(darkSkyUnavailable: Option[String], sources: List[String], units: Units)
  implicit val unitsDecoder: Decoder[Units] = deriveUnwrappedDecoder[Units]
  implicit val flagsDecoder: Decoder[Flags] = Decoder.instance { c =>
    for {
      unavailable <- c.downField("darksky-unavailable").as[Option[String]]
      sources     <- c.downField("sources").as[List[String]]
      units       <- c.downField("units").as[Units]
    } yield Flags(unavailable, sources, units)
  }

  @JsonCodec(decodeOnly = true)
  final case class DataPoint(
    apparentTemperature: Option[Float],
    apparentTemperatureHigh: Option[Float],
    apparentTemperatureHighTime: Option[Long],
    apparentTemperatureLow: Option[Float],
    apparentTemperatureLowTime: Option[Long],
    cloudCover: Option[Float],
    dewPoint: Option[Float],
    humidity: Option[Float],
    icon: Option[String],
    moonPhase: Option[Float],
    nearestStormBearing: Option[Int],
    nearestStormDistance: Option[Int],
    ozone: Option[Float],
    precipAccumulation: Option[Float],
    precipIntensity: Option[Float],
    precipIntensityMax: Option[Float],
    precipIntensityMaxTime: Option[Long],
    precipProbability: Option[Float],
    precipType: Option[String],
    pressure: Option[Float],
    summary: Option[String],
    sunriseTime: Option[Long],
    sunsetTime: Option[Long],
    temperature: Option[Float],
    temperatureHigh: Option[Float],
    temperatureHighTime: Option[Long],
    temperatureLow: Option[Float],
    temperatureLowTime: Option[Long],
    temperatureMax: Option[Float],
    temperatureMaxTime: Option[Long],
    temperatureMin: Option[Float],
    temperatureMinTime: Option[Long],
    time: Long,
    uvIndex: Option[Int],
    uvIndexTime: Option[Long],
    visibility: Option[Float],
    windBearing: Option[Int],
    windGust: Option[Float],
    windSpeed: Option[Float]
  )

}
