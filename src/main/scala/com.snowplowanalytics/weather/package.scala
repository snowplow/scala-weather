package com.snowplowanalytics

// Scala
import scala.concurrent.Future

// Scalaz
import scalaz.\/

package object weather {

  // Common classes

  /**
    * Class to represent geographical coordinates
    *
    * @param latitude place's latitude
    * @param longitude places's longitude
    */
  case class Position(latitude: Float, longitude: Float)

  /**
    * Cache key for obtaining record
    *
    * @param day timestamp for 0:00:00
    * @param center rounded geo coordinates
    */
  case class CacheKey(day: Day, center: Position) {
    def endOfDay = day + 86400
  }

  // Errors

  /**
   * Superclass for non-fatal exceptions that can be happen for weather fetching/processing
   */
  sealed trait WeatherError

  /**
   * Connecting/receiving/etc timeout errors
   */
  case class TimeoutError(message: String) extends java.util.concurrent.TimeoutException(message) with WeatherError

  /**
   * Invalid state/argument/response
   */
  case class InternalError(message: String) extends WeatherError

  /**
   * Response parsing error
   */
  case class ParseError(message: String) extends WeatherError

  // Type aliases/lambdas

  type ValidatedWeather[+A] = WeatherError \/ A
  type AsyncWeather[+A] = Future[WeatherError \/ A]

  type Timestamp = Int
  type Day = Int        // 0:00:00 timestamp of day
}
