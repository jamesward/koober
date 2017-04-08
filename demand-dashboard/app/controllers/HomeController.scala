package controllers

import javax.inject._

import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.{Json, Reads, Writes}
import play.api.mvc.{Action, Controller}
import services.PredictionIO

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random


@Singleton
class HomeController @Inject() (configuration: Configuration, predictionIO: PredictionIO)(implicit executionContext: ExecutionContext) extends Controller {

  implicit val dateWrites = Writes.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  implicit val dateReads = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  lazy val mapboxAccessToken: String = configuration.getString("mapbox.access-token").get

  def index = Action {
    Ok(views.html.index(mapboxAccessToken))
  }

  // todo: input checking and error handling
  def predict = Action.async(parse.json) { request =>
    val startDateTime = (request.body \ "startDateTime").as[DateTime]
    val endDateTime = (request.body \ "endDateTime").as[DateTime]

    predictionIO.predict(startDateTime, endDateTime).map { json =>
      Ok(json)
    }
  }

  def fakePredict = Action {
    Ok(
      Json.obj(
        "demand" -> Random.nextInt()
      )
    )
  }

  private def toGeoJson(value: Double, lat: Double, lon: Double) = {
    Json.obj(
      "type" -> "Feature",
      "properties" -> Json.obj(
        "Primary ID" -> value
      ),
      "geometry" -> Json.obj(
        "type" -> "Point",
        "coordinates" -> Json.arr(lon, lat)
      )
    )
  }

  def demand(lng: Double, lat: Double) = Action {
    val points = Seq.fill(50) {
      val newLng = lng + (0.1 * Random.nextDouble()) - 0.05
      val newLat = lat + (0.1 * Random.nextDouble()) - 0.05
      toGeoJson(1, newLat, newLng)
    }

    Ok(
      Json.obj(
        "type" -> "FeatureCollection",
        "crs" -> Json.obj(
          "type" -> "name",
          "properties" -> Json.obj(
            "name" -> "urn:ogc:def:crs:OGC:1.3:CRS84"
          )
        ),
        "features" -> points
      )
    )
  }

}
