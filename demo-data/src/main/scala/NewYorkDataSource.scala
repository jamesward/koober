import java.io.{File, FileOutputStream}
import java.net.URL

import akka.NotUsed
import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.ByteString
import org.apache.commons.io.IOUtils
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.libs.json.{JsObject, Json}

import scala.collection.immutable.Range.Inclusive
import scala.collection.immutable.{IndexedSeq, Seq}
import scala.util.Try

// Right now only yellow cab data
// List of sources: https://github.com/toddwschneider/nyc-taxi-data/blob/master/raw_data_urls.txt
object NewYorkDataSource {

  val years: Inclusive = 2009 to 2016
  val months: Inclusive = 1 to 12
  val yearsMonths: IndexedSeq[(Int, Int)] = for {
    year <- years
    month <- months
    if !(year >= 2016 && month >= 7) // the data does not contain GPS coordinates starting in July 2016
  } yield (year, month)

  val dateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  def filename(yearMonth: (Int, Int)): String = {
    val (year, month) = yearMonth
    val monthString = if (month >= 10) month else "0" + month
    s"yellow_tripdata_$year-$monthString.csv"
  }

  // todo: maybe random files?
  def apply(numMonths: Int, sampleRate: Int): Source[JsObject, NotUsed] = {

    // takes the most recent months
    val filenames: Seq[String] = yearsMonths.takeRight(numMonths).map(filename)

    // todo: don't save these locally if there isn't the space for it
    val files = filenames.map { filename =>

      val url = new URL(s"https://s3.amazonaws.com/predictionio-temp-file/ny_yellowcab_with_weather_data.csv")

      val tmpFile = new File("/tmp", filename)

      if (!tmpFile.exists()) {
        val inputStream = url.openConnection().getInputStream
        val tmpFileOutputStream = new FileOutputStream(tmpFile)

        println(s"Downloading $url to $tmpFile")

        IOUtils.copy(inputStream, tmpFileOutputStream)

        tmpFileOutputStream.close()
        inputStream.close()
      }

      tmpFile
    }

    val fileSources = Source.zipN[ByteString](files.map(file => FileIO.fromPath(file.toPath))).mapConcat(identity)

    val allLinesSource = fileSources.via(Framing.delimiter(ByteString(System.lineSeparator), 10000)).map(_.utf8String)

    val partitionedLinesSource = allLinesSource.grouped(sampleRate).mapConcat(_.headOption.toList)

    // todo: data structure changes?
    val parsedLines = partitionedLinesSource.mapConcat { line =>

      val parseTry = Try {
        val parts = line.split(",")

        val lng = parts(5).toDouble
        val lat = parts(6).toDouble
        val datetime = dateTimeFormatter.parseDateTime(parts(1))
        val temperature = parts(19).toDouble
        val clear = parts(20).toInt
        val fog = parts(21).toInt
        val rain = parts(22).toInt
        val snow = parts(23).toInt
        val hail = parts(24).toInt
        val thunder = parts(25).toInt
        val tornado = parts(26).toInt
        val heat = parts(21).toDouble
        val windchill = parts(22).toDouble
        val precipitation = parts(23).toDouble

        (lat, lng, datetime, temperature, clear, fog, rain, snow, hail, thunder, tornado, heat, windchill, precipitation)
      }

      val onlyGoodLocations = parseTry.filter {
        case (lat, lng, datetime, temperature, clear, fog, rain, snow, hail, thunder, tornado, heat, windchill, precipitation) =>
          lat != 0 && lng != 0
      }

      val jsonTry = onlyGoodLocations.map {
        case (lat, lng, datetime, temperature, clear, fog, rain, snow, hail, thunder, tornado, heat, windchill, precipitation) =>
          Json.obj(
            "properties" -> Json.obj(
              // Location Properties
              "lat" -> lat,
              "lng" -> lng,
              // Weather Properties
              "temperature" -> temperature,
              "clear" -> clear,
              "fog" -> fog,
              "rain" -> rain,
              "snow" -> snow,
              "hail" -> hail,
              "thunder" -> thunder,
              "tornado" -> tornado,
              "heat" -> heat,
              "windchill" -> windchill,
              "precipitation" -> precipitation
            ),
            "status" -> "pickup",
            "datetime" -> datetime
          )
      }

      // we won't be able to parse some rows
      jsonTry.toOption.toList
    }

    parsedLines
  }

}
