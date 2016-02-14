package com.snowplowanalytics

// Scalaz
import com.snowplowanalytics.weather.providers.openweather.Errors

import scalaz.\/

// Scala
import scala.concurrent.Future

// This library
import Errors._

package object weather {

  type Timestamp = Int
  type Day = Int        // 0:00:00 timestamp of day

  // Type aliases/lambdas
  type ValidatedWeather[+A] = WeatherError \/ A
  type AsyncWeather[+A] = Future[WeatherError \/ A]
}
