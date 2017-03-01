package controllers

import javax.inject._

import play.api.mvc.{Action, Controller}
import services.PredictionIO

import scala.concurrent.Future


@Singleton
class HomeController @Inject() (predictionIO: PredictionIO) extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def predict = Action.async {
    // todo
    //predictionIO.predict()
    Future.successful(NotImplemented)
  }

}
