import java.io.{File, FileOutputStream}
import java.net.URL
import java.util.zip.ZipInputStream

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.immutable.Iterable
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Flow, Framing}
import akka.util.ByteString
import helpers.KafkaHelper
import org.apache.commons.io.IOUtils
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.util.Try

object DemoData extends App {

  val numberOfRecordsToProcess = 10

  val url = new URL("https://s3-us-west-2.amazonaws.com/4740/yellow_tripdata_2015_all_sample.csv.zip")

  val tmpFile = new File("/tmp/koober-demo-data.csv")

  if (!tmpFile.exists()) {
    println(s"$tmpFile does not exists so downloading $url")

    val inputStream = url.openConnection().getInputStream
    val zipInputStream = new ZipInputStream(inputStream)

    val tmpFileOutputStream = new FileOutputStream(tmpFile)

    println(s"Saving ${zipInputStream.getNextEntry.getName} to $tmpFile")

    IOUtils.copy(zipInputStream, tmpFileOutputStream)

    tmpFileOutputStream.close()
    zipInputStream.close()
    inputStream.close()
  }

  val dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val fileSource = FileIO.fromPath(tmpFile.toPath)

  val lines = Flow[ByteString].via(Framing.delimiter(ByteString(System.lineSeparator), 10000)).map(_.utf8String)

  val parser = Flow[String].mapConcat { line =>
    val tryParse = Try {
      val parts = line.split(",")

      val lat = parts(12).toDouble
      val lng = parts(13).toDouble
      val datetime = dateTimeFormatter.parseDateTime(parts(19))


      Json.obj(
        "lngLat" -> Json.obj(
          "lat" -> lat,
          "lng" -> lng
        ),
        "status" -> "pickup",
        "datetime" -> datetime
      )
    }

    // we won't be able to parse some rows
    tryParse.map(Iterable(_)).getOrElse(Iterable.empty[JsObject])
  }

  val producerSettings = ProducerSettings[String, JsValue](system, new StringSerializer(), KafkaHelper.jsValueSerializer)
    .withBootstrapServers(KafkaHelper.kafkaUrl(system.settings.config))

  val kafkaSink = Producer.plainSink(producerSettings).contramap[JsObject](new ProducerRecord("rider", "", _))

  // drop the first row because it is the column names
  val flow = fileSource.via(lines).drop(1).via(parser).take(numberOfRecordsToProcess).runWith(kafkaSink)

  println(s"Reading $numberOfRecordsToProcess and sending them to Kafka")

  flow.onComplete { _ =>
    println(s"Sent $numberOfRecordsToProcess to Kafka")
    system.terminate()
  }

}
