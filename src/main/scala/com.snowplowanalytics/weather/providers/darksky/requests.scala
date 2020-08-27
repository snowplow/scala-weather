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

import java.text.DecimalFormat

import scalaj.http._

import model.WeatherRequest

final case class BlockType private (name: String) extends AnyVal

object BlockType {
  val currently = BlockType("currently")
  val minutely  = BlockType("minutely")
  val hourly    = BlockType("hourly")
  val daily     = BlockType("daily")
  val alerts    = BlockType("alerts")
  val flags     = BlockType("flags")
}

final case class Units private (name: String) extends AnyVal

object Units {
  val auto = Units("auto")
  val ca   = Units("ca")
  val uk2  = Units("uk2")
  val us   = Units("us")
  val si   = Units("si")
}

object requests {

  final case class DarkSkyRequest(
    latitude: Float,
    longitude: Float,
    time: Option[Long] = None,
    exclude: List[BlockType] = List.empty[BlockType],
    extend: Boolean = false,
    lang: Option[String] = None,
    units: Option[Units] = None
  ) extends WeatherRequest {

    override def constructRequest(baseUri: String, apiKey: String): HttpRequest = {
      val pathParams = List(latitude, longitude).map(floatToString) ++ time.map(_.toString).toList
      val uri        = s"$baseUri/$apiKey/${pathParams.mkString(",")}"

      val queryParams = List(
        exclude.map(_.name).reduceOption(_ + "," + _).map("exclude" -> _),
        Some(extend).collect { case true => "extend" -> "hourly" },
        lang.map("lang"   -> _),
        units.map("units" -> _.name)
      ).flatten

      Http(uri).params(queryParams)
    }
  }

  /** Dark Sky seems to not consume numbers in scientific notation,
    * which are sometimes produced by toString
    */
  private def floatToString(value: Float): String = {
    val decimalFormat = new DecimalFormat("0.0000")
    decimalFormat.setMinimumFractionDigits(0)
    decimalFormat.format(value.toDouble)
  }

}
