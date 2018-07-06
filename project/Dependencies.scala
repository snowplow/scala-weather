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
import sbt._
import Keys._

object Dependencies {

  val resolutionRepos = Seq(
    // For Twitter's LRU cache
    "Twitter Maven Repo" at "http://maven.twttr.com/"
  )

  object V {
    // Java
    val jodaTime    = "2.10"
    val jodaConvert = "2.1"
    // Scala
    val circe    = "0.9.3"
    val collUtil = "6.39.0"
    val hammock  = "0.8.5"
    // Tests
    val specs2   = "3.9.4"
  }

  object Libraries {
    // Java
    val jodaTime        = "joda-time"   %  "joda-time"         % V.jodaTime
    val jodaConvert     = "org.joda"    %  "joda-convert"      % V.jodaConvert
    // Scala
    val circeGeneric    = "io.circe"    %% "circe-generic"     % V.circe
    val circeParser     = "io.circe"    %% "circe-parser"      % V.circe
    val collUtil        = "com.twitter" %% "util-collection"   % V.collUtil
    val hammockCore     = "com.pepegar" %% "hammock-core"      % V.hammock
    // Tests
    val specs2          = "org.specs2"  %% "specs2-core"       % V.specs2 % "test"
    val specs2Mock      = "org.specs2"  %% "specs2-mock"       % V.specs2 % "test"
    val specsScalaCheck = "org.specs2"  %% "specs2-scalacheck" % V.specs2 % "test"
  }
}
