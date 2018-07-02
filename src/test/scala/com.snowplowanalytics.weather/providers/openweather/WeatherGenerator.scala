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
package providers.openweather

import org.scalacheck._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen._

import Responses._
import WeatherCache._

/**
 * Trait with methods for random weather generation
 */
trait WeatherGenerator {
  val someTenths = (0 until 300).by(10).map(Some(_))

  def genWind: Gen[Wind] =
    for {
      speed  <- arbitrary[BigDecimal]
      deg    <- arbitrary[BigDecimal]
      varBeg <- Gen.oneOf(someTenths)
      varEnd <- Gen.oneOf(someTenths)
      gust   <- arbitrary[Option[BigDecimal]]
    } yield Wind(speed, deg, gust, varBeg, varBeg.flatMap(x => varEnd.map(_ + x)))

  def genClouds: Gen[Clouds] =
    for {
      all <- arbitrary[BigInt]
    } yield Clouds(all)

  def genRain: Gen[Rain] =
    for {
      oneHour   <- arbitrary[Option[BigDecimal]]
      threeHour <- arbitrary[Option[BigDecimal]]
    } yield Rain(oneHour, threeHour)

  def genSnow: Gen[Snow] =
    for {
      oneHour   <- arbitrary[Option[BigDecimal]]
      threeHour <- arbitrary[Option[BigDecimal]]
    } yield Snow(oneHour, threeHour)

  def genMain: Gen[MainInfo] =
    for {
      grndLevel <- arbitrary[Option[BigDecimal]]
      humidity  <- arbitrary[BigDecimal]
      pressure  <- arbitrary[BigDecimal]
      seaLevel  <- arbitrary[Option[BigDecimal]]
      temp      <- arbitrary[BigDecimal]
      tempMin   <- arbitrary[BigDecimal]
      tempMax   <- arbitrary[BigDecimal]
    } yield MainInfo(grndLevel, humidity, pressure, seaLevel, temp, tempMin, tempMax)

  // 12 Feb 2015 - 01 Dec 2016
  def genTimestamp: Gen[Int] = Gen.choose(1423729852, 1480582792)

  def genWeatherDescription: Gen[List[WeatherCondition]] =
    for {
      main        <- Gen.oneOf(TestData.weatherConditions)
      description <- Gen.oneOf(TestData.weatherDescriptions.filter(_.contains(main.toLowerCase)))
      icon        <- Gen.oneOf(TestData.icons)
      id          <- Gen.oneOf(TestData.descriptionIds)
      size        <- Gen.oneOf(List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3))
      result      <- Gen.listOfN(size, WeatherCondition(main, description, id, icon))
    } yield result

  def genWeatherStamp: Gen[Weather] =
    for {
      main        <- genMain
      wind        <- genWind
      clouds      <- genClouds
      rain        <- arbitrary[Option[Rain]]
      snow        <- arbitrary[Option[Snow]]
      description <- genWeatherDescription
      dt          <- genTimestamp
    } yield Weather(main, wind, clouds, rain, snow, description, dt)

  def genEmptyHistoryBatch: Gen[History] =
    for {
      cnt   <- Gen.choose(1, 23)
      wind  <- genWind
      count <- cnt
    } yield History(cnt, "200", Nil)

  def genNonEmptyHistoryBatch: Gen[History] =
    for {
      cnt    <- Gen.choose(2, 20)
      wind   <- genWind
      seed   <- Gen.choose(-1, 3)
      count  <- cnt
      stamps <- arbitrary[Weather]
    } yield History(cnt, "200", List.fill(cnt + seed)(stamps))

  implicit def arbitraryRain: Arbitrary[Rain] = Arbitrary(genRain)
  implicit def arbitrarySnow: Arbitrary[Snow] = Arbitrary(genSnow)
  implicit def arbitraryWeatherStamp: Arbitrary[Weather] =
    Arbitrary(genWeatherStamp)

  /**
   * Pick random position predefined in `TestData` and distort it with <30km
   */
  def genPredefinedPosition(positions: Vector[Position]): Gen[Position] =
    for {
      seedLat <- Gen.choose(-0.4f, 0.4f) // Distort lat and lon little bit
      seedLon <- Gen.choose(-0.8f, 0.8f)
      pos     <- Gen.choose(0, positions.length - 1)
    } yield {
      val position = positions(pos)
      position.copy(latitude = position.latitude + seedLat, longitude = position.longitude + seedLon)
    }

  /**
   * Generate timestamp somewhere between two weeks ago and yesterday
   *
   * @return timestamp in seconds
   */
  def genLastWeekTimeStamp: Gen[Int] = {
    val now: Int = (System.currentTimeMillis() / 1000).toInt
    for {
      seed <- Gen.choose(86400 * 2, 1209600) // Between yesterday and two weeks back
    } yield now - seed
  }

  implicit def arbitraryPosition: Arbitrary[Position] =
    Arbitrary(genPredefinedPosition(TestData.randomCities))
}
