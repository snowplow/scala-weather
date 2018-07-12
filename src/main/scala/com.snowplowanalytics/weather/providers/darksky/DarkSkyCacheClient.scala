package com.snowplowanalytics.weather
package providers.darksky

// Java
import java.time.ZonedDateTime

// This library
import Responses.DarkSkyResponse
import Errors.WeatherError

class DarkSkyCacheClient[F[_]] private[darksky] (cache: Cache[F, DarkSkyResponse], transport: Transport[F])
    extends DarkSkyClient[F](transport) {

  val geoPrecision: Int = cache.geoPrecision

  /**
   * Gets DarkSkyResponse for the specified date (alignes time to midnight) from the cache,
   * if not found, then performs the request
   * == IMPORTANT ==
   * Returned DarkSkyResponse's `currently` field excluded, because the time is always aligned to midnight
   *
   * @param latitude The latitude of a location. Positive is north, negative is south.
   * @param longitude The longitude of a location. Positive is east, negative is west.
   * @param dateTime zoned datetime, time is aligned to midnight
   * @return either weather error or dark sky response, wrapper in effect type F
   */
  def cachingTimeMachine(latitude: Float,
                         longitude: Float,
                         dateTime: ZonedDateTime,
                         exclude: List[BlockType] = List.empty[BlockType],
                         extend: Boolean          = false,
                         lang: Option[String]     = None,
                         units: Option[Units]     = None): F[Either[WeatherError, DarkSkyResponse]] =
    cache.getCachedOrRequest(latitude, longitude, dateTime)(doRequest(exclude, extend, lang, units))

  private def doRequest(exclude: List[BlockType], extend: Boolean, lang: Option[String], units: Option[Units])(
    latitude: Float,
    longitude: Float,
    dateTime: ZonedDateTime): F[Either[WeatherError, DarkSkyResponse]] =
    timeMachine(latitude, longitude, dateTime, (List(BlockType.currently) ++ exclude).distinct, extend, lang, units)

}
