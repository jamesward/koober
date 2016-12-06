package controllers

import javax.inject._

import akka.stream.scaladsl.Flow
import org.apache.kafka.clients.producer.ProducerRecord
import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.mvc.{Action, Controller, WebSocket}
import services.Kafka


@Singleton
class HomeController @Inject() (kafka: Kafka, configuration: Configuration) extends Controller {

  lazy val mapboxAccessToken: String = configuration.getString("mapbox.access-token").get

  def driver = Action { implicit request =>
    Ok(views.html.driver(routes.HomeController.driverWs().webSocketURL(), mapboxAccessToken))
  }

  def rider = Action { implicit request =>
    Ok(views.html.rider(routes.HomeController.riderWs().webSocketURL(), mapboxAccessToken))
  }

  // sink is incoming driver messages
  // source is outgoing rider messages
  def driverWs = WebSocket.accept { request =>
    val sink = kafka.sink.contramap[JsValue](new ProducerRecord("driver", "", _))
    val source = kafka.source("rider").map(_.value())
    Flow.fromSinkAndSource(sink, source)
  }

  // sink is incoming rider messages
  // source is outgoing driver messages
  def riderWs = WebSocket.accept { request =>
    val sink = kafka.sink.contramap[JsValue](new ProducerRecord("rider", "", _))
    val source = kafka.source("driver").map(_.value())
    Flow.fromSinkAndSource(sink, source)
  }

}
