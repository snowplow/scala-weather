/*
 * Copyright (c) 2015-2017 Snowplow Analytics Ltd. All rights reserved.
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

import WeatherCache.Position

/**
 * Contains most-populated cities plus randomly picked city from every country,
 * plus some very distant or unpopulated cities,
 * plus cities with abnormal responses which I've found
 */
object TestData {
  // Pre-picked cities
  val bigAndAbnormalCities = Vector(
    Position(28.666668f, 77.21667f),
    Position(55.75222f, 37.615555f),
    Position(43.10562f, 131.87354f),
    Position(56.00972f, 92.79167f),
    Position(69.3535f, 88.2027f),
    Position(40.4165f, -3.70256f),
    Position(35.91979f, -88.75895f),
    Position(22.28552f, 114.15769f),
    Position(-23.5475f, -46.63611f),
    Position(-1.28333f, 36.81667f),
    Position(42.98339f, -81.23304f),
    Position(43.2f, -80.38333f),
    Position(35.6895f, 139.69171f),
    Position(37.085152f, 15.273000f), // Siracusa
    Position(39.213039f, -106.937820f), // Snowmass Village, US
    Position(42.869999f, 74.589996f), // Bishkek
    Position(50.000000f, 8.271110f) // Mainz, DE
  )

  // Random cities
  val randomCities = Vector(
    Position(33.59278f, -7.61916f),
    Position(14.35f, 108.0f),
    Position(26.866667f, 81.2f),
    Position(12.65f, -8.0f),
    Position(-3.945f, 122.49889f),
    Position(31.21564f, 29.95527f),
    Position(42.89427f, 24.71589f),
    Position(42.43333f, 23.81667f),
    Position(13.68935f, -89.18718f),
    Position(7.88481f, 98.40008f),
    Position(47.93595f, 13.48306f),
    Position(39.59611f, 27.02444f),
    Position(6.12104f, 100.36014f),
    Position(52.5396f, 31.9275f),
    Position(8.122222f, -63.54972f),
    Position(30.609444f, 34.80111f),
    Position(30.609444f, 34.80111f),
    Position(36.72564f, 9.18169f),
    Position(18.00191f, -66.10822f),
    Position(10.31672f, 123.89071f),
    Position(42.88052f, -8.54569f),
    Position(25.05f, 61.74167f),
    Position(-38.13874f, 176.24516f),
    Position(-22.68333f, 14.53333f),
    Position(4.58333f, 13.68333f),
    Position(5.47366f, 10.41786f),
    Position(38.71418f, -93.99133f),
    Position(46.55472f, 15.64667f),
    Position(46.55f, 26.95f),
    Position(-24.65451f, 25.90859f),
    Position(52.70389f, -8.86417f),
    Position(34.60712f, 43.67822f),
    Position(28.233334f, 83.98333f),
    Position(51.0159f, 4.20173f),
    Position(-33.83333f, 151.13333f),
    Position(-6.82349f, 39.26951f),
    Position(-34.83346f, -56.16735f),
    Position(21.51694f, 39.21917f),
    Position(-17.82935f, 31.05389f),
    Position(35.01361f, 69.17139f),
    Position(-20.16194f, 57.49889f),
    Position(47.39489f, 18.9136f),
    Position(-16.5f, -68.15f),
    Position(60.86667f, 26.7f),
    Position(67.25883f, 15.39181f),
    Position(50.63945f, 20.30454f),
    Position(46.91035f, 7.47096f),
    Position(9.17583f, 7.18083f),
    Position(35.1f, 33.41667f),
    Position(24.46667f, 54.36667f),
    Position(50.75667f, 78.54f),
    Position(-36.89272f, -60.32254f),
    Position(51.40606f, -0.4137f),
    Position(-4.21528f, -69.94056f),
    Position(35.88972f, 14.4425f),
    Position(35.53722f, 129.31667f),
    Position(48.666668f, 26.566668f),
    Position(-16.47083f, -54.63556f),
    Position(39.31009f, 16.3399f),
    Position(19.56378f, -70.87582f),
    Position(4.88447f, -1.75536f),
    Position(-29.61678f, 30.39278f),
    Position(54.09833f, 28.3325f),
    Position(-4.05466f, 39.66359f),
    Position(31.1f, -107.98333f),
    Position(48.73946f, 19.15349f),
    Position(35.69111f, -0.64167f),
    Position(25.286667f, 51.533333f),
    Position(55.86066f, 9.85034f),
    Position(9.90467f, -83.68352f)
  )

  val weatherConditions = Vector(
    "Clear",
    "Clouds",
    "Drizzle",
    "Fog",
    "Haze",
    "Mist",
    "Rain",
    "Snow",
    "Thunderstorm"
  )

  val weatherDescriptions = Vector(
    "thunderstorm with light rain",
    "light intensity drizzle rain",
    "Sky is Clear",
    "broken clouds",
    "drizzle",
    "few clouds",
    "fog",
    "haze",
    "heavy intensity rain",
    "light intensity drizzle",
    "light rain",
    "light shower snow",
    "light snow",
    "mist",
    "moderate rain",
    "overcast clouds",
    "proximity shower rain",
    "proximity thunderstorm",
    "rain and drizzle",
    "scattered clouds",
    "sky is clear",
    "thunderstorm with rain",
    "thunderstorm",
    "very heavy rain",
    "light intensity shower rain"
  )

  val icons = Vector("01d",
                     "01n",
                     "02d",
                     "02n",
                     "03d",
                     "03n",
                     "04d",
                     "04n",
                     "09d",
                     "09n",
                     "10d",
                     "10n",
                     "11d",
                     "11n",
                     "13d",
                     "13n",
                     "50d",
                     "50n")

  val descriptionIds =
    Vector(200, 211, 300, 301, 302, 310, 500, 501, 502, 520, 521, 600, 620, 701, 721, 741, 800, 801, 802, 803, 804)
}
