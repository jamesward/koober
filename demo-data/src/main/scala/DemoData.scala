import java.time.ZonedDateTime

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import helpers.KafkaHelper
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import play.api.libs.json.{JsObject, JsValue}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

object DemoData extends App {

  val sourceTry: Try[Source[JsObject, NotUsed]] = args match {
    case Array(dataType, numMonths, sampleRate) if dataType == "ny" =>
      println(s"Sending New York data to Kafka")
      Success(NewYorkDataSource(numMonths.toInt, sampleRate.toInt))
    case Array(dataType, numRecords, numMonths, numClusters) if dataType == "fake" =>
      println(s"Sending Fake data to Kafka")
      val endDate = ZonedDateTime.now()
      val startDate = endDate.minusMonths(numMonths.toInt)
      Success(FakeDataSource(numRecords.toInt, startDate, endDate, numClusters.toInt, 10))
    case _ =>
      Failure(CommandNotRecognized())
  }

  sourceTry.foreach { source =>
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val producerSettings = ProducerSettings[String, JsValue](system, new StringSerializer(), KafkaHelper.jsValueSerializer)
      .withBootstrapServers(KafkaHelper.kafkaUrl(system.settings.config))

    val kafkaSink = Producer.plainSink(producerSettings).contramap[JsObject](new ProducerRecord("rider", "", _))

    val countSink = Sink.fold[Int, JsObject](0) { case (count, _) => count + 1 }

    // drop the first row because it is the column names
    val flow = source.alsoTo(kafkaSink).runWith(countSink)

    flow.onComplete { result =>
      result.foreach { records => println(s"Sent $records records to Kafka") }
      system.terminate()
    }
  }

  sourceTry.recover {
    case t: Throwable => println(t.getMessage)
  }

  case class CommandNotRecognized() extends Throwable {
    override def getMessage: String = {
      """Command args must be either:
        |ny <number of months> <sample rate>
        |fake <number of records> <number of months> <number of clusters>""".stripMargin
    }
  }

}
