/*
 * Copyright (c) 2013-2015 Snowplow Analytics Ltd. All rights reserved.
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
    "Sonatype" at "https://oss.sonatype.org/content/repositories/releases",
    // For scala-util
    "Snowplow Analytics" at "http://maven.snplow.com/releases/",
    // For Twitter's LRU cache
    "Twitter Maven Repo" at "http://maven.twttr.com/"
  )

  object V {
    // Java
    val jodaTime    = "2.9.9"
    val jodaConvert = "1.8.2"
    // Scala
    val json4s   = "3.2.11"
    val scalaj   = "2.3.0"
    val collUtil = "6.34.0"
    // Tests
    val specs2   = "3.9.4"
  }

  object Libraries {
    // Java
    val jodaTime        = "joda-time"   %  "joda-time"         % V.jodaTime
    val jodaConvert     = "org.joda"    %  "joda-convert"      % V.jodaConvert
    // Scala
    val json4s          = "org.json4s"  %% "json4s-core"       % V.json4s
    val json4sJackson   = "org.json4s"  %% "json4s-jackson"    % V.json4s
    val scalaj          = "org.scalaj"  %% "scalaj-http"       % V.scalaj
    val collUtil        = "com.twitter" %% "util-collection"   % V.collUtil
    // Tests
    val specs2          = "org.specs2"  %% "specs2-core"       % V.specs2 % "test"
    val specs2Mock      = "org.specs2"  %% "specs2-mock"       % V.specs2 % "test"
    val specsScalaCheck = "org.specs2"  %% "specs2-scalacheck" % V.specs2 % "test"
  }
}
