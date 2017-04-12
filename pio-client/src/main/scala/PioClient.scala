import java.util.UUID

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import helpers.KafkaHelper
import org.apache.kafka.common.serialization.StringDeserializer
import org.joda.time.DateTime
import play.api.http.Status
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object PioClient extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val keyDeserializer = new StringDeserializer()

  val consumerSettings = ConsumerSettings(system, keyDeserializer, KafkaHelper.jsValueDeserializer)
    .withBootstrapServers(KafkaHelper.kafkaUrl(system.settings.config))
    .withGroupId("pio-client")

  val subscriptions = Subscriptions.topics("rider")
  val source = Consumer.plainSource(consumerSettings, subscriptions)

  val accessKey: String = sys.env("PIO_ACCESS_KEY")

  source.map(_.value()).via(PioFlow(accessKey)).runWith(Sink.ignore)
}

object PioFlow {

  val pioUrl: String = "http://localhost:7070/events.json"

  def apply(accessKey: String)(implicit materializer: ActorMaterializer): Flow[JsValue, JsValue, NotUsed] = {
    val wsClient = AhcWSClient()

    materializer.system.registerOnTermination(wsClient.close())

    Flow[JsValue].mapAsync(50) { kafkaJson =>
      val status = (kafkaJson \ "status").as[String]
      val dateTime = (kafkaJson \ "datetime").as[DateTime]
      val lngLat = (kafkaJson \ "lngLat").as[JsObject]

      val pioJson = Json.obj(
        "event" -> status,
        "entityId" -> UUID.randomUUID(),
        "entityType" -> "location",
        "properties" -> lngLat,
        "eventTime" -> dateTime.toString
      )

      wsClient.url(pioUrl).withQueryString("accessKey" -> accessKey).post(pioJson).flatMap { response =>
        response.status match {
          case Status.CREATED => Future.successful(response.json)
          case _ => Future.failed(new Exception((response.json \ "message").as[String]))
        }
      }
    }
  }
}
