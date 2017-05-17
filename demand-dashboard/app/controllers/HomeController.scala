package controllers

import javax.inject._

import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.{Json, JsValue, Reads, Writes}
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
    Ok(views.html.index())
  }

  def header = Action {
    Ok(views.html.header())
  }

  def documentationSectionHeader = Action {
    Ok(views.html.documentationSectionHeader())
  }

  def documentationData = Action {
    Ok(views.html.documentationData())
  }

  def documentationModels = Action {
    Ok(views.html.documentationModels())
  }

  def dashboardSectionHeader = Action {
    Ok(views.html.dashboardSectionHeader())
  }

  def dashboardAnalysis = Action {
    Ok(views.html.dashboardAnalysis(mapboxAccessToken))
  }

  def dashboardPrediction = Action {
    Ok(views.html.dashboardPrediction(mapboxAccessToken))
  }

  // todo: input checking and error handling
  def predict(eventTime: String, lat: Double, lng: Double, temperature: Double,
              clear: Int, fog: Int, rain: Int, snow: Int, hail: Int, thunder: Int, tornado: Int, algorithm:Int) = Action.async {

    var lngLatArray = makeCluster(lat, lng)

    val resultSeq = (0 to (lngLatArray(0).length - 1)).map(i => {
      var query = Json.obj(
        "eventTime" -> eventTime,
        "lat" -> lngLatArray(0)(i),
        "lng" -> lngLatArray(1)(i),
        "temperature" -> temperature,
        "clear" -> clear,
        "fog" -> fog,
        "rain" -> rain,
        "snow" -> snow,
        "hail" -> hail,
        "thunder" -> thunder,
        "tornado" -> tornado
      )
      var prediction = predictionIO.predict(query)
      prediction.map { json => toGeoJson2(json, lngLatArray(0)(i), lngLatArray(1)(i), i, algorithm) }
    })

    var result = Future.sequence(resultSeq)
    result.map { r => Ok(toGeoJsonCollection(r))
    }
  }

  def analyze(algorithm: String, eventTime: String, coordinates: String) = Action.async {

    var coordinatesData = deserializeCoordinatesData(coordinates)
    val resultSeq = (0 to (coordinatesData.length - 1)).map(i => {
      var query = Json.obj(
        "eventTime" -> eventTime,
        "lat" -> coordinatesData(i)(0),
        "lng" -> coordinatesData(i)(1),
        "temperature" -> 20,
        "clear" -> 1,
        "fog" -> 0,
        "rain" -> 0,
        "snow" -> 0,
        "hail" -> 0,
        "thunder" -> 0,
        "tornado" -> 0
      )
      var prediction = predictionIO.predict(query)
      prediction.map { json => toGeoJsonForAlg(algorithm, json, coordinatesData(i)(1), coordinatesData(i)(0), i) }
    })

    var result = Future.sequence(resultSeq)
    result.map { r => Ok(toGeoJsonCollection(r))
    }
  }

  private def toGeoJsonForAlg(algorithm: String, json: JsValue, lat: Double, lon: Double, id: Int) = {
    var demand = (json \ "algorithms" \ algorithm).as[Double]

    Json.obj(
      "type" -> "Feature",
      "properties" -> Json.obj(
        "Primary ID" -> id,
        "demand" -> demand
      ),
      "geometry" -> Json.obj(
        "type" -> "Point",
        "coordinates" -> Json.arr(lon, lat)
      )
    )
  }

  private def toGeoJson2(json: JsValue, lat: Double, lon: Double, id: Int, alg: Int) = {
    var demand = (json \ "demand").as[Double]
    alg match {
      case 0 => demand = (json \ "demand").as[Double]
      case 1 => demand = (json \ "algorithms" \"algGBTree").as[Double]
      case 2 => demand = (json \ "algorithms" \"algRegression").as[Double]
      case 3 => demand = (json \ "algorithms" \"ridgeRegression").as[Double]
      case 4 => demand = (json \ "algorithms" \"randomForest").as[Double]
      case default => demand = (json \ "demand").as[Double]
    }

    Json.obj(
      "type" -> "Feature",
      "properties" -> Json.obj(
        "Primary ID" -> id,
        "demand" -> demand
      ),
      "geometry" -> Json.obj(
        "type" -> "Point",
        "coordinates" -> Json.arr(lon, lat)
      )
    )
  }

  private def toGeoJson(value: Double, lat: Double, lon: Double) = {
    Json.obj(
      "type" -> "Feature",
      "properties" -> Json.obj(
        "Primary ID" -> value,
        "demand" -> Random.nextInt(10)
      ),
      "geometry" -> Json.obj(
        "type" -> "Point",
        "coordinates" -> Json.arr(lon, lat)
      )
    )
  }

  private def toGeoJsonCollection(demands: IndexedSeq[JsValue]) = {
    Json.obj(
      "type" -> "FeatureCollection",
      "features" -> demands
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


  def makeCluster(lat:Double, lng:Double) : Array[Array[Double]] = {
    var lats = Array(lat, lat + 2*0.0016, lat + 0.0016, lat + 0.0016, lat + 0.0016, lat, lat, lat, lat, lat - 0.0016, lat - 0.0016, lat - 0.0016, lat - 2*0.0016)
    var lngs = Array(lng, lng, lng - 0.0016, lng, lng + 0.0016, lng - 2*0.0016, lng - 0.0016, lng + 0.0016, lng + 2*0.0016, lng - 0.0016, lng, lng + 0.0016, lng)
    Array(lats, lngs)
  }

  def deserializeCoordinatesData(coordinates:String): Array[Array[Double]] = {
    var tokens = coordinates.substring(2, coordinates.length-2).split("\\],\\[")
    var coordinatesArray:Array[Array[String]] = tokens.map(_.split(',')).toArray
    var coordinatesData = coordinatesArray.map( _.map { tmp =>
      try {
        tmp.toDouble
      } catch {
        case _: NumberFormatException => 1.0
      }
    })
    coordinatesData
  }

}
