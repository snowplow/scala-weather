# Scala Weather

[ ![Build Status] [travis-image] ] [travis] [ ![Release] [release-image] ] [releases] [ ![License] [license-image] ] [license]

## Overview

High-performance, asynchronous and cache-aware Scala library for looking up the weather.

Used in **[Snowplow] [snowplow-repo]** to power the **[Weather Enrichment] [weather-enrichment]** for incoming events.

## Introduction

Currently Scala Weather works with only one weather provider: **[OpenWeatherMap] [openweathermap]**. It allows you to fetch the current weather, historical weather and weather forecasts for any city or geo coordinate.

Scala Weather currently provides two clients `OwmAsyncClient` and `OwmCacheClient`:

1. `OwmAsyncClient` works in a fully asynchronous way, using akka-http under the hood and returning `Future[WeatherError \/ WeatherResponse]`, where `WeatherResponse` is one of the three OpenWeatherMap responses (`Current`, `History`, `Forecast`) as appropriate
2. `OwmCacheClient` works in a blocking way, contains an LRU cache inside and returns `WeatherError \/ WeatherResponse`

`\/` is a **[scalaz disjunction] [scalaz-disjunction]**, which is isomorphic with Scala's native `Either`.

Although you will typically want to use the `OwmAsyncClient`, note that Scala Weather was written to support the **[Snowplow] [snowplow]** enrichment process, which uses `OwmCacheClient`.

## Setup

### OpenWeatherMap sign up

First **[sign up] [owm-signup]** to OpenWeatherMap to get your API key.

Unfortunately, with free plan you can only perform current weather and forecast lookups; for historical data access you need to subscribe **[history plan] [history-plan]**. If you use the free plan all `historyBy...` methods will return failures.

### Installation

The latest version of Scala Weather is 0.1.0, which is cross-built against Scala 2.10.x and 2.11.x.

If you're using SBT, add the following lines to your build file:

```scala
// Resolvers
val snowplowRepo = "Snowplow Analytics" at "http://maven.snplow.com/releases/"
val oldTwitterRepo  = "Twitter Maven Repo" at "http://maven.twttr.com/"
val newTwitterRepo = "Sonatype" at "https://oss.sonatype.org/content/repositories/releases"

// Dependency
val scalaWeather = "com.snowplowanalytics" %% "scala-weather"  % "0.1.0"
```

Note the double percent (`%%`) between the group and artifactId.
That'll ensure you get the right package for your Scala version.

## Usage

Once you have your API key, you can create a client:

```scala
import com.snowplowanalytics.weather.providers.openweather.OwmAsyncClient
val client = OwmAsyncClient(YOURKEY)
```

OpenWeatherMap provides several hosts for API with various benefits, which you can pass as the second argument:

+ `api.openweathermap.org` - free access, recommended, used in `OwmAsyncClient` by default
+ `history.openweathermap.org` - paid, history only, used in `OwmCacheClient` by default
+ `pro.openweathermap.org` - paid, faster, SSL-enabled

Both clients offer the same set of public methods:

+ `forecastById`
+ `forecastByCoords`
+ `currentById`
+ `currentByCoords`
+ `historyById`
+ `historyByName`
+ `historyByCoords

These methods were designed to follow OpenWeatherMap's own API calls as closely as possible. All of these calls receive similar arguments to those described in **[OpenWeatherMap API documentation] [owm-api-docs]**. For example, to receive a response equivalent to this API call: ``api.openweathermap.org/data/2.5/weather?lat=35&lon=139&appid=YOURKEY``, run the following code:

```scala
val weatherInLondon: Future[WeatherError \/ Current] = asyncClient.currentByCoords(35, 139)
```

Notice that all temperature fields are in Kelvin, which is the OpenWeatherMap default (OWM only supports unit preference for the current weather).

Scala Weather doesn't try to validate your arguments (except of course their types), so invalid calls like this one:

```scala
// Count supposed to be positive
val forecast: Future[WeatherError \/ Current] = client.forecastById(3070325, cnt=-1)
```

will still be executed and OpenWeatherMap will decide how to handle it (in this case, it will ignore negative count).

## Understanding the cache

### General information

Scala Weather provides a cache as part of the `weather.providers.openweather.OwmCacheClient`. It uses an **[LRU cache] [lru]** under the hood and only works for historical lookups. OpenWeatherMap restricts the number of history lookups you can make on the paid plans, so this cache helps to minimize requests, especially when run from a "big data" runtime such as Hadoop, Storm or Spark.

Note that the results of common methods like `historyById`, `currentByCoords` etc are not cached. You need to use `getCachedOrRequest(latitude: Float, longitude: Float, timestamp: Int): WeatherError \/ Weather` to employ the cache.

The following client constructor arguments help to tune the cache:

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

`OwmCacheClient`'s internal cache follows this structure:

|  Key                                                  |  Value            |
| ----------------------------------------------------- | ----------------- |
|  `round(latitude), round(longitude), beginningOfDay`  |  `List[Weather]`  |

After the first request, it will try to fetch information for this exact place (round(latitude), round(longitude)) for the given day.
This cache value usually consist of several (8-24) `Weather` objects, each with its own timestamps. `getCachedOrRequest` will return closest weather for specified timestamp and leave other in cache.

For example, the following code will make only one request to OpenWeatherMap, but return 3 distinct results:

```scala
val client = OwmCacheClient(MYKEY, 10, 1, 5)
client.getCachedOrRequest(10.4f, 32.1f, 1447070440)   // Nov 9 12:00:40 2015.
client.getCachedOrRequest(10.1f, 32.312f, 1447063607) // Nov 9 10:06:47 2015. From cache
client.getCachedOrRequest(10.2f, 32.4f, 1447096857)   // Nov 9 19:20:57 2015. From cache
```

## Copyright and license

Scala Weather is copyright 2015 Snowplow Analytics Ltd.

Licensed under the **[Apache License, Version 2.0] [license]**  (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[openweathermap]: http://openweathermap.org/
[owm-api-docs]: http://openweathermap.org/api
[lru]: https://en.wikipedia.org/wiki/Cache_algorithms#LRU
[history-plan]: http://openweathermap.org/price
[owm-signup]: http://home.openweathermap.org/users/sign_up
[scalaz]: https://github.com/scalaz/scalaz
[scalaz-disjunction]: http://docs.typelevel.org/api/scalaz/stable/7.0.0/doc/scalaz/$bslash$div$minus.html

[snowplow]: http://snowplowanalytics.com
[snowplow-repo]: https://github.com/snowplow/snowplow
[weather-enrichment]: https://github.com/snowplow/snowplow/wiki/Weather-enrichment

[vagrant-install]: http://docs.vagrantup.com/v2/installation/index.html
[virtualbox-install]: https://www.virtualbox.org/wiki/Downloads

[travis]: https://travis-ci.org/snowplow/scala-weather
[travis-image]: https://travis-ci.org/snowplow/scala-weather.png?branch=master

[release-image]: http://img.shields.io/badge/release-0.1.0-blue.svg?style=flat
[releases]: https://github.com/snowplow/scala-weather/releases

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0
