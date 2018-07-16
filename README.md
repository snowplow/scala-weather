# Scala Weather

[![Build Status][travis-image]][travis] [![Maven Central][maven-badge]][maven-link] [![License][license-image]][license]
[![Join the chat at https://gitter.im/snowplow/scala-weather](https://badges.gitter.im/snowplow/scala-weather.svg)](https://gitter.im/snowplow/scala-weather?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![codecov](https://codecov.io/gh/snowplow/scala-weather/branch/master/graph/badge.svg)](https://codecov.io/gh/snowplow/scala-weather)

## Overview

High-performance, asynchronous and cache-aware Scala library for looking up the weather.

Used in **[Snowplow][snowplow-repo]** to power the **[Weather Enrichment][weather-enrichment]** for incoming events.

## Introduction

Scala Weather contains APIs to 2 weather providers: **[OpenWeatherMap][openweathermap]** and **[Dark Sky][darksky]**. 
It allows you to fetch the current weather, historical weather and weather forecasts for any city (OWM only) or geo coordinates.

We provide caching and basic clients for both providers - `OwmClient` and `OwmCacheClient` for OpenWeatherMap, `DarkSkyClient` and `DarkSkyCacheClient` for Dark Sky.

## Installation

The latest version of Scala Weather is 0.3.0, which is cross-built against Scala 2.11.x and 2.12.x.

If you're using SBT, add the following lines to your build file:

```scala
val scalaWeather = "com.snowplowanalytics" %% "scala-weather" % "0.3.0"
```

## Guide

**Note:** All of the clients take `F` as a type parameter. It can be any type that has an instance of `cats.effect.Sync` in case of basic clients and `cats.effect.Concurrent` in case of caching clients. In the following guide we will be using `cats.effect.IO`. 

### OpenWeatherMap

First **[sign up][owm-signup]** to OpenWeatherMap to get your API key.

Unfortunately, with the free plan you can only perform current weather and forecast lookups; for historical data access you need to subscribe to the **[history plan][history-plan]**. If you use the free plan all `historyBy...` methods will return failures.


#### Usage

Once you have your API key, you can create a client:

```scala
import com.snowplowanalytics.weather.providers.openweather.OpenWeatherMap
import cats.effect.IO
val client = OpenWeatherMap.basicClient[IO]("YOUR_KEY")
```

OpenWeatherMap provides several hosts for API with various benefits, which you can pass as the second argument:

+ `api.openweathermap.org` - free access, recommended, used in `OwmAsyncClient` by default
+ `history.openweathermap.org` - paid, history only, used in `OwmCacheClient` by default
+ `pro.openweathermap.org` - paid, faster, SSL-enabled

You can enable SSL through the third boolean argument:

```scala
val client = OpenWeatherMap.basicClient[IO]("YOUR_KEY", "history.openweathermap.org", ssl = true)
```

Both clients offer the same set of public methods:

+ `forecastById`
+ `forecastByName`
+ `forecastByCoords`
+ `currentById`
+ `currentByName`
+ `currentByCoords`
+ `historyById`
+ `historyByName`
+ `historyByCoords`

These methods were designed to follow OpenWeatherMap's own API calls as closely as possible. All of these calls receive similar arguments to those described in **[OpenWeatherMap API documentation][owm-api-docs]**. For example, to receive a response equivalent to this API call: ``api.openweathermap.org/data/2.5/weather?lat=35&lon=139&appid=YOURKEY``, run the following code:

```scala
import com.snowplowanalytics.weather.Errors.WeatherError
import com.snowplowanalytics.weather.providers.openweather.Responses.Current
val weatherInLondon: IO[Either[WeatherError, Current]] = client.currentByCoords(35.0f, 139.0f)
```

Notice that all temperature fields are in Kelvin, which is the OpenWeatherMap default (OWM only supports unit preference for the current weather).

Scala Weather doesn't try to validate your arguments (except of course their types), so invalid calls like this one:

```scala
// Count supposed to be positive
val forecast: IO[Either[WeatherError, Current]] = client.forecastById(3070325, cnt=-1)
```

will still be executed and OpenWeatherMap will decide how to handle it (in this case, it will ignore negative count).

The caching client is created like this:
```scala
val cachingClient = OpenWeatherMap.cacheClient[IO]("YOUR_KEY")
```
More about caching later.

### Dark Sky

Sign up **[here][darkskydev]** to receive the API key. Dark Sky currently allows for 1000 free requests per day.

#### Usage

Similar to OpenWeatherMap, to create a basic client you can use the factories in the `DarkSky` object:
```scala
import com.snowplowanalytics.weather.providers.darksky.DarkSky
import cats.effect.IO

val client = DarkSky.basicClient[IO]("YOUR_KEY")
```

Dark Sky API is much simpler than OWM, it consists only of two functions, namely `forecast` and `timeMachine`

- `forecast` returns the current weather and the forecast for next week
- `timeMachine` returns the observed or forecast weather for the specified date in the past or the future

Example:

```scala
import java.time.ZonedDateTime
import com.snowplowanalytics.weather.Errors.WeatherError
import com.snowplowanalytics.weather.providers.darksky.Responses.DarkSkyResponse
// Fetches weather a year ago in New York
val response: IO[Either[WeatherError, DarkSkyResponse]] = 
  client.timeMachine(40.71f, 74.0f, ZonedDateTime.now().minusYears(1))
```

Contrary to OpenWeatherMap, Dark Sky does not provide geocoding features,
meaning you must know the latitude and longitude of the location.


## Understanding the cache

### General information

Scala Weather provides a cache as part of the `weather.providers.openweather.OwmCacheClient` or `weather.providers.darksky.DarkSkyCacheClient`.
It uses an **[LRU cache][lru]** under the hood and only works for historical lookups. OpenWeatherMap restricts the number of history lookups you can make on the paid plans, so this cache helps to minimize requests, especially when run from a "big data" frameworks.

Note that the results of common methods like `historyById`, `currentByCoords` etc are not cached.
To employ the cache, you need to use:
 
 - for OWM `cachingHistoryByCoords`
 - for Dark Sky `cachingTimeMachine`

The following client factory arguments help to tune the cache:

* `cacheSize` determines how many daily weather reports for a location can be stored in the cache before entries start getting evicted
* `geoPrecision` determines how precise your cache will be from geospatial perspective

### Understanding the `geoPrecision` 

This essentially rounds the decimal places part of the geo coordinate to the specified part of 1. Some example settings:

* Setting to 1 will round both 32.05 and 32.4 to 32.0 (so 1/1)
* Setting to 2 will round 32.4 to 32.5, and 30.1 to 30.0 (so, 1/2)
* Setting to 5 will round 42.11 to 42.2 (so, 1/5)

In the worst precision case (`geoPrecision == 1`), our geo-locations for the weather will be up to ~60km out. But you need to note that the primary sources of weather inaccuracies are not usually distance, but things like urban/suburbian area, or rapid extreme weather changes, or the distance to the closest weather station (hundreds kilometers in some rare cases).

So, you can consider 1 as a good default value; it's strongly discouraged to set it higher than 10.

### How the cache works

`OwmCacheClient` and `DarkSkyCacheClient`'s internal cache stores responses in a map, which key is
computed roughly as:

`round(latitude), round(longitude), beginningOfDay` 

After the first request, it will try to fetch information for this exact place (round(latitude), round(longitude)) for the given day.
The OWM cache value usually consists of several (8-24) `Weather` objects, each with its own timestamps.

`cachingHistoryByCoords` in the OWM client will return the closest weather for the specified timestamp and leave the others in the cache.
For example, the following code will make only one request to OpenWeatherMap, but return 3 distinct results:

`cachingTimeMachine` in the Dark Sky client will return the whole `DarkSkyResponse` for the specified day.
The field `currently` will be omitted, so the user must rely on the `hourly` or `daily` fields.

The return type of both `OpenWeatherMap.cacheClient` and `DarkSky.cacheClient` is wrapped in the effect type, as
the creation of the underlying cache allocates mutable state.

```scala
import com.snowplowanalytics.weather.providers.openweather.OpenWeatherMap
import cats.effect.IO

val action = for {
  client <- OpenWeatherMap.cacheClient[IO]("YOUR_KEY", cacheSize = 10, geoPrecision = 1, timeout = 5.seconds)
  response1 <- client.cachingHistoryByCoords(10.4f, 32.1f, 1514765952)   // Jan 1 12:19:12 2018.
  response2 <- client.cachingHistoryByCoords(10.1f, 32.312f, 1514821267) // Jan 1 15:41:07 2018. From cache
  response3 <- client.cachingHistoryByCoords(10.2f, 32.4f, 1514776320)   // Jan 1 03:12:00 2018. From cache
} yield (response1, response2, response3)

action.unsafeRunSync()
```


## Copyright and license

Scala Weather is copyright 2015-2018 Snowplow Analytics Ltd.

Licensed under the **[Apache License, Version 2.0][license]**  (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[openweathermap]: http://openweathermap.org/
[owm-api-docs]: http://openweathermap.org/api
[darksky]: https://darksky.net
[darkskydev]: https://darksky.net/dev
[lru]: https://en.wikipedia.org/wiki/Cache_algorithms#LRU
[history-plan]: http://openweathermap.org/price
[owm-signup]: http://home.openweathermap.org/users/sign_up
[scalaz]: https://github.com/scalaz/scalaz
[scalaz-disjunction]: http://docs.typelevel.org/api/scalaz/stable/7.0.0/doc/scalaz/$bslash$div$minus.html

[snowplow]: http://snowplowanalytics.com
[snowplow-repo]: https://github.com/snowplow/snowplow
[weather-enrichment]: https://github.com/snowplow/snowplow/wiki/Weather-enrichment

[travis]: https://travis-ci.org/snowplow/scala-weather
[travis-image]: https://travis-ci.org/snowplow/scala-weather.png?branch=master

[maven-badge]: https://maven-badges.herokuapp.com/maven-central/com.snowplowanalytics/scala-weather_2.12/badge.svg
[maven-link]: https://maven-badges.herokuapp.com/maven-central/com.snowplowanalytics/scala-weather_2.12

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0
