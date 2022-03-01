/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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
  .settings(
    name := "scala-weather",
    organization := "com.snowplowanalytics",
    description := "High-performance Scala library for performing current and historical weather lookups",
    scalaVersion := "2.13.8",
    crossScalaVersions := Seq("2.12.15", "2.13.8"),
    javacOptions := BuildSettings.javaCompilerOptions,
    shellPrompt := { _ =>
      name.value + "> "
    },
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n <= 12 =>
          List(compilerPlugin(("org.scalamacros" % "paradise" % "2.1.1").cross(CrossVersion.full)))
        case _ => Nil
      }
    },
    Compile / scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n <= 12 => Nil
        case _                       => List("-Ymacro-annotations")
      }
    }
  )
  .settings(BuildSettings.publishSettings)
  .settings(BuildSettings.docsSettings)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.Libraries.circeGeneric,
      Dependencies.Libraries.circeParser,
      Dependencies.Libraries.circeExtras,
      Dependencies.Libraries.scalaj,
      Dependencies.Libraries.lruMap,
      Dependencies.Libraries.specs2,
      Dependencies.Libraries.specs2Mock,
      Dependencies.Libraries.specsScalaCheck
    )
  )
  .enablePlugins(SiteScaladocPlugin, PreprocessPlugin)
