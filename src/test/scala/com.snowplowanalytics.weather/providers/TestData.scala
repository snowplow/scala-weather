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
package com.snowplowanalytics.weather.providers

/**
  * Contains most-populated cities plus randomly picked city from every country,
  * plus some very distant or unpopulated cities,
  * plus cities with abnormal responses which I've found
  */
object TestData {
  // Pre-picked cities
  val bigAndAbnormalCities = Vector(
    (28.666668f, 77.21667f),
    (55.75222f, 37.615555f),
    (43.10562f, 131.87354f),
    (56.00972f, 92.79167f),
    (69.3535f, 88.2027f),
    (40.4165f, -3.70256f),
    (35.91979f, -88.75895f),
    (22.28552f, 114.15769f),
    (-23.5475f, -46.63611f),
    (-1.28333f, 36.81667f),
    (42.98339f, -81.23304f),
    (43.2f, -80.38333f),
    (35.6895f, 139.69171f),
    (37.085152f, 15.273000f),   // Siracusa
    (39.213039f, -106.937820f), // Snowmass Village, US
    (42.869999f, 74.589996f),   // Bishkek
    (50.000000f, 8.271110f)     // Mainz, DE
  )

  // Random cities
  val randomCities = Vector(
    (33.59278f, -7.61916f),
    (14.35f, 108.0f),
    (26.866667f, 81.2f),
    (12.65f, -8.0f),
    (-3.945f, 122.49889f),
    (31.21564f, 29.95527f),
    (42.89427f, 24.71589f),
    (42.43333f, 23.81667f),
    (13.68935f, -89.18718f),
    (7.88481f, 98.40008f),
    (47.93595f, 13.48306f),
    (39.59611f, 27.02444f),
    (6.12104f, 100.36014f),
    (52.5396f, 31.9275f),
    (8.122222f, -63.54972f),
    (30.609444f, 34.80111f),
    (30.609444f, 34.80111f),
    (36.72564f, 9.18169f),
    (18.00191f, -66.10822f),
    (10.31672f, 123.89071f),
    (42.88052f, -8.54569f),
    (25.05f, 61.74167f),
    (-38.13874f, 176.24516f),
    (-22.68333f, 14.53333f),
    (4.58333f, 13.68333f),
    (5.47366f, 10.41786f),
    (38.71418f, -93.99133f),
    (46.55472f, 15.64667f),
    (46.55f, 26.95f),
    (-24.65451f, 25.90859f),
    (52.70389f, -8.86417f),
    (34.60712f, 43.67822f),
    (28.233334f, 83.98333f),
    (51.0159f, 4.20173f),
    (-33.83333f, 151.13333f),
    (-6.82349f, 39.26951f),
    (-34.83346f, -56.16735f),
    (21.51694f, 39.21917f),
    (-17.82935f, 31.05389f),
    (35.01361f, 69.17139f),
    (-20.16194f, 57.49889f),
    (47.39489f, 18.9136f),
    (-16.5f, -68.15f),
    (60.86667f, 26.7f),
    (67.25883f, 15.39181f),
    (50.63945f, 20.30454f),
    (46.91035f, 7.47096f),
    (9.17583f, 7.18083f),
    (35.1f, 33.41667f),
    (24.46667f, 54.36667f),
    (50.75667f, 78.54f),
    (-36.89272f, -60.32254f),
    (51.40606f, -0.4137f),
    (-4.21528f, -69.94056f),
    (35.88972f, 14.4425f),
    (35.53722f, 129.31667f),
    (48.666668f, 26.566668f),
    (-16.47083f, -54.63556f),
    (39.31009f, 16.3399f),
    (19.56378f, -70.87582f),
    (4.88447f, -1.75536f),
    (-29.61678f, 30.39278f),
    (54.09833f, 28.3325f),
    (-4.05466f, 39.66359f),
    (31.1f, -107.98333f),
    (48.73946f, 19.15349f),
    (35.69111f, -0.64167f),
    (25.286667f, 51.533333f),
    (55.86066f, 9.85034f),
    (9.90467f, -83.68352f)
  )

  val owmWeatherConditions = Vector(
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

  val owmWeatherDescriptions = Vector(
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

  val owmIcons = Vector(
    "01d",
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
    "50n"
  )

  val owmDescriptionIds =
    Vector(200, 211, 300, 301, 302, 310, 500, 501, 502, 520, 521, 600, 620, 701, 721, 741, 800, 801, 802, 803, 804)
}
