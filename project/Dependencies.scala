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
import sbt._

object Dependencies {

  object V {
    val circe  = "0.11.1"
    val scalaj = "2.4.1"
    val lruMap = "0.3.0"

    // Tests
    val specs2 = "4.3.5"
  }

  object Libraries {
    val circeGeneric    = "io.circe"              %% "circe-generic"        % V.circe
    val circeParser     = "io.circe"              %% "circe-parser"         % V.circe
    val circeExtras     = "io.circe"              %% "circe-generic-extras" % V.circe
    val scalaj     = "org.scalaj"            %% "scalaj-http"          % V.scalaj
    val lruMap          = "com.snowplowanalytics" %% "scala-lru-map"        % V.lruMap
    // Tests
    val specs2          = "org.specs2"            %% "specs2-core"          % V.specs2 % Test
    val specs2Mock      = "org.specs2"            %% "specs2-mock"          % V.specs2 % Test
    val specsScalaCheck = "org.specs2"            %% "specs2-scalacheck"    % V.specs2 % Test
  }
}
