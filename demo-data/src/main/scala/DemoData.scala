import java.time.ZonedDateTime

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.impl.fusing.GraphStages
import akka.stream.scaladsl.{Sink, Source}
import helpers.KafkaHelper
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import play.api.libs.json.{JsObject, JsValue}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object DemoData extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def sinkFromDestination(destination: String): Try[Sink[JsValue, Future[Done]]] = {
    if (destination.toLowerCase == "kafka") {
      val producerSettings = ProducerSettings[String, JsValue](materializer.system, new StringSerializer(), KafkaHelper.jsValueSerializer)
        .withBootstrapServers(KafkaHelper.kafkaUrl(materializer.system.settings.config))

      val sink = Producer.plainSink(producerSettings).contramap[JsValue](new ProducerRecord("rider", "", _))
      Success(sink)
    }
    else if (destination.toLowerCase == "pio") {
      val maybeAccessKey = sys.env.get("PIO_ACCESS_KEY")
      maybeAccessKey.fold[Try[Sink[JsValue, Future[Done]]]] {
        Failure(new Exception("You must set the PIO_ACCESS_KEY env var!"))
      } { accessKey =>
        println("Sending to PIO: " + PioSink.pioUrl)
        Success(PioSink(accessKey))
      }
    }
    else {
      Failure(new Exception(s"The destination $destination must be either kafka or pio"))
    }
  }

  val sourceSinkTry: Try[(Source[JsObject, NotUsed], Sink[JsValue, Future[Done]])] = args match {
    case Array(destination, dataType, numMonths, sampleRate) if dataType == "ny" =>
      val source = NewYorkDataSource(numMonths.toInt, sampleRate.toInt)
      val sinkTry = sinkFromDestination(destination)
      val sourceSinkTry = sinkTry.map(source -> _)
      sourceSinkTry.foreach(_ => println(s"Sending New York data to $destination"))
      sourceSinkTry
    case Array(destination, dataType, numRecords, numMonths, numClusters) if dataType == "fake" =>
      val endDate = ZonedDateTime.now()
      val startDate = endDate.minusMonths(numMonths.toInt)
      val source = FakeDataSource(numRecords.toInt, startDate, endDate, numClusters.toInt, 10)
      val sinkTry = sinkFromDestination(destination)
      val sourceSinkTry = sinkTry.map(source -> _)
      sourceSinkTry.foreach(_ => println(s"Sending Fake data to $destination"))
      sourceSinkTry
    case _ =>
      Failure(CommandNotRecognized())
  }

  sourceSinkTry.foreach { case (source, sink) =>
    val countSink = Sink.fold[Int, JsObject](0) { case (count, _) => count + 1 }

    val flow = source.alsoTo(sink).runWith(countSink)

    flow.onComplete { result =>
      result.foreach { records => println(s"Sent $records records") }
      system.terminate()
    }
  }

  sourceSinkTry.recover {
    case t: Throwable =>
      println(t.getMessage)
      system.terminate()
  }

  case class CommandNotRecognized() extends Throwable {
    override def getMessage: String = {
      """Command args must be either:
        | <pio|kafka> ny <number of months> <sample rate>
        | <pio|kafka> fake <number of records> <number of months> <number of clusters>""".stripMargin
    }
  }

}
