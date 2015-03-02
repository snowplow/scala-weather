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
    "Twitter Maven Repo" at "http://maven.twttr.com/",
    // For scalaz-7.0 & specs2
    "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
  )

  object V {
    // Java
    val jodaTime      = "2.7"

    // Scala
    val scalaz        = "7.0.6"
    val json4s        = "3.2.11"
    val akka          = "2.3.14"
    val akkaStreams   = "1.0"

    object collUtil {
      val _210      = "6.3.4"
      val _211      = "6.29.0"  // Should work on 2.10 too
    }

    // Tests
    val mockito     = "2.0.31-beta"
    val scalaCheck  = "1.12.5"
    val specs2      = s"3.6.1-scalaz-$scalaz" // $scalaz is for binary-compatible 3.x.x
  }

  object Libraries {
    // Java
    val jodaTime     = "joda-time"                 % "joda-time"                    % V.jodaTime

    // Scala
    val scalaz        = "org.scalaz"               %% "scalaz-core"                 % V.scalaz
    val json4s        = "org.json4s"               %% "json4s-core"                 % V.json4s
    val json4sJackson = "org.json4s"               %% "json4s-jackson"              % V.json4s
    val json4sScalaz  = "org.json4s"               %% "json4s-scalaz"               % V.json4s
    val akka          = "com.typesafe.akka"        %% "akka-actor"                  % V.akka
    val akkaStreams   = "com.typesafe.akka"        %% "akka-stream-experimental"    % V.akkaStreams
    val akkaHttpCore  = "com.typesafe.akka"        %% "akka-http-core-experimental" % V.akkaStreams
    val akkaHttp      = "com.typesafe.akka"        %% "akka-http-experimental"      % V.akkaStreams

    object collUtil {
      val _210      = "com.twitter"                %% "util-collection"             % V.collUtil._210
      val _211      = "com.twitter"                %% "util-collection"             % V.collUtil._211
    }

    // Tests
    val mockito         = "org.mockito"            % "mockito-core"        % V.mockito           % "test"
    val specs2          = "org.specs2"             %% "specs2-core"        % V.specs2            % "test"
    val specs2Mock      = "org.specs2"             %% "specs2-mock"        % V.specs2            % "test"
    val specsScalaCheck = "org.specs2"             %% "specs2-scalacheck"  % V.specs2            % "test"
    val scalaCheck      = "org.scalacheck"         %% "scalacheck"         % V.scalaCheck        % "test"
  }

  def onVersion[A](all: Seq[A] = Seq(), on210: => Seq[A] = Seq(), on211: => Seq[A] = Seq()) =
    scalaVersion(v => all ++ (if (v.contains("2.10.")) {
      on210
    } else {
      on211
    }))
}
