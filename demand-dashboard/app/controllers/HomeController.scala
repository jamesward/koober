package controllers

import javax.inject._

import org.joda.time.DateTime
import play.api.libs.json.{Json, Reads, Writes}
import play.api.mvc.{Action, Controller}
import services.PredictionIO

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random


@Singleton
class HomeController @Inject() (predictionIO: PredictionIO)(implicit executionContext: ExecutionContext) extends Controller {

  implicit val dateWrites = Writes.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  implicit val dateReads = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  def index = Action {
    Ok(views.html.index())
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

}
