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

/** Module for various predefined values in OWM specification */
object Api {

  /** Representation of measurement enums for history requests */
  sealed trait Measure {
    val value: String
  }

  final case object Tick extends Measure {
    val value = "tick"
  }

  final case object Hour extends Measure {
    val value = "hour"
  }

  final case object Day extends Measure {
    val value = "day"
  }

}
