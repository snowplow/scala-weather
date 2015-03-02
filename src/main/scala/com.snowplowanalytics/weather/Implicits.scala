/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
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

import scala.language.implicitConversions

object Implicits {
  /**
   * Options as optional arguments
   * See: http://stackoverflow.com/questions/4199393/are-options-and-named-default-arguments-like-oil-and-water-in-a-scala-api
   */
  class OptArg[T] private (val option: Option[T])
  object OptArg {
    implicit def any2opt[T](t: T): OptArg[T] = new OptArg(Option(t)) // NOT Some(t)
    implicit def option2opt[T](o: Option[T]): OptArg[T] = new OptArg(o)
    implicit def opt2option[T](o: OptArg[T]): Option[T] = o.option
  }

  /**
   * Construct Map out of (k,v) pair where v is optional
   *
   * @param pair tuple with optional second element
   * @return empty Map if v is None, Map(k -> v) otherwise
   */
  implicit def pairToMap(pair: (String, Option[String])): Map[String, String] =
    pair._2.map(v => Map(pair._1 -> v)).getOrElse(Map.empty)

  /**
   * Addition to `pairToMap` specified on Integers
   *
   * @param pair tuple with optional second element
   * @return empty Map if v is None, Map(k -> v) otherwise
   */
  implicit def pairIntToMap(pair: (String, OptArg[Int])): Map[String, String] =
    pair._2.map(v => Map(pair._1 -> v.toString)).getOrElse(Map.empty)
}
