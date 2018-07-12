package com.snowplowanalytics.weather.providers.darksky

// cats
import cats.data.NonEmptyList

// hammock
import hammock._

// tests
import org.specs2.{ScalaCheck, Specification}

// This library
import Requests.DarkSkyRequest
import BlockType._

class RequestSpec extends Specification with ScalaCheck {
  def is = s2"""

  Dark Sky API query-building tests

    Queries with no time, no params             $e1
    Queries with time, no params                $e2
    Exclude parameter should be comma separated $e3
    Extend parameter should have value hourly   $e4
    Should handle many parameters correctly     $e5

  """

  val baseUri = uri"http://example.com"
  val key     = "0123456789ABCDEF"

  def e1 = {
    val request = DarkSkyRequest(17.3f, -89.3f)
    request.constructQuery(baseUri, key) mustEqual baseUri / key / "17.3,-89.3"
  }

  def e2 = {
    val request = DarkSkyRequest(17.3f, -89.3f, Some(1234567))
    request.constructQuery(baseUri, key) mustEqual baseUri / key / "17.3,-89.3,1234567"
  }

  def e3 = {
    val request = DarkSkyRequest(17.3f, -89.3f, exclude = List(daily, minutely, hourly, flags))
    request.constructQuery(baseUri, key) mustEqual
      (baseUri / key / "17.3,-89.3") ? NonEmptyList.of("exclude" -> "daily,minutely,hourly,flags")
  }

  def e4 = {
    val request = DarkSkyRequest(17.3f, -89.3f, extend = true)
    request.constructQuery(baseUri, key) mustEqual
      (baseUri / key / "17.3,-89.3") ? NonEmptyList.of("extend" -> "hourly")
  }

  def e5 = {
    val request = DarkSkyRequest(17.3f,
                                 -89.3f,
                                 Some(1234567),
                                 exclude = List(daily),
                                 extend  = true,
                                 lang    = Some("pl"),
                                 units   = Some(Units.auto))
    request.constructQuery(baseUri, key) mustEqual (baseUri / key / "17.3,-89.3,1234567") ?
      NonEmptyList.of("exclude" -> "daily", "extend" -> "hourly", "lang" -> "pl", "units" -> "auto")
  }

}
