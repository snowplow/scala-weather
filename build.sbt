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

lazy val root = project
  .in(file("."))
  .enablePlugins(ScalaUnidocPlugin, GhpagesPlugin)
  .settings(
    name := "scala-weather",
    organization := "com.snowplowanalytics",
    version := "0.4.0",
    description := "High-performance Scala library for performing current and historical weather lookups",
    scalaVersion := "2.12.8",
    javacOptions := BuildSettings.javaCompilerOptions,
    shellPrompt := { _ =>
      name.value + "> "
    },
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
  )
  .settings(BuildSettings.publishSettings)
  .settings(BuildSettings.formatting)
  .settings(BuildSettings.docsSettings)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.Libraries.circeGeneric,
      Dependencies.Libraries.circeParser,
      Dependencies.Libraries.circeExtras,
      Dependencies.Libraries.hammockCore,
      Dependencies.Libraries.lruMap,
      Dependencies.Libraries.specs2,
      Dependencies.Libraries.specs2Mock,
      Dependencies.Libraries.specsScalaCheck
    )
  )
