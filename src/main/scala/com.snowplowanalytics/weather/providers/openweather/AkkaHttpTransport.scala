/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
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

// Scalaz
import scalaz.{ \/, \/-, -\/ }

// Scala
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.control.NonFatal

// Akka
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Flow, Source, Sink }
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import com.typesafe.config.ConfigFactory

// json4s
import org.json4s.JValue
import org.json4s.jackson.JsonMethods.parseOpt

// This library
import Requests.WeatherRequest

/**
 * Akka Streams based transport
 *
 * @param apiHost weather API server host
 */
class AkkaHttpTransport(actorSystem: ActorSystem, apiHost: String) extends HttpAsyncTransport {
  implicit val system: ActorSystem = actorSystem
  implicit val context: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  // This will use request pool
  private val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
    Http().outgoingConnection(apiHost)

  def getData(request: WeatherRequest, appId: String): Future[WeatherError \/ JValue] = {
    val uri = request.constructQuery(appId)
    val response = Source.single(HttpRequest(uri = uri))
      .via(connectionFlow)
      .runWith(Sink.head)
    response.flatMap(processHttpResponse)
  }

  /**
   * Get JSON out of HTTP response body
   *
   * @param response full HTTP response
   * @return future with either server error or JSON
   */
  private def processHttpResponse(response: HttpResponse): Future[WeatherError \/ JValue] = {
    getResponseContent(response) match {
      case \/-(content) => content.map(parseJson)
      case -\/(failure) => Future.successful(\/.left(failure))
    }
  }

  /**
   * Wait 2 seconds for entity and convert it to string
   *
   * @param response full HTTP response
   * @return future entity content of HTTP response or throwable in case of timeout
   */
  private def getResponseContent(response: HttpResponse): WeatherError \/ Future[String] =
    try {
      \/.right(response.entity.toStrict(2.seconds).map(_.data.utf8String))
    } catch {
      case _: java.util.concurrent.TimeoutException =>
        \/.left(TimeoutError("OpenWeatherMap Error: HTTP entity didn't processed in 2 seconds"))
      case NonFatal(e) => \/.left(ParseError(s"OpenWeatherMap Error: Can't parse HTTP response. ${e.toString}"))
    }

  /**
   * Try to parse JSON
   *
   * @param content string containing JSON
   * @return disjunction of throwable and JValue
   */
  private def parseJson(content: String): WeatherError \/ JValue =
    parseOpt(content) match {
      case Some(json) => \/.right(json)
      case None => \/.left(ParseError(s"OpenWeatherMap Error: string [$content] doesn't contain JSON"))
    }
}

object AkkaHttpTransport {
  // If all transport will get injected actor systems, this one won't be initialized
  lazy val system: ActorSystem = ActorSystem("scala-weather-system",
    ConfigFactory.parseString("akka.daemonic=on"))

  /**
   * Create with built-in actor system
   *
   * @param host OWM API host
   * @return async transport
   */
  def apply(host: String): AkkaHttpTransport = {
    new AkkaHttpTransport(system, host)
  }

  /**
   * Create transport with custom actor system
   *
   * @param actorSystem custom actor system
   * @param host OWM API host
   * @return async transport
   */
  def apply(actorSystem: ActorSystem, host: String): AkkaHttpTransport = {
    new AkkaHttpTransport(actorSystem, host)
  }
}
