package services

import javax.inject.{Inject, Singleton}

import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient

import scala.concurrent.Future


trait PredictionIO {
  def predict(startDateTime: DateTime, endDateTime: DateTime): Future[JsObject]
}

@Singleton
class PredictionIOImpl @Inject() (configuration: Configuration, wsClient: WSClient) extends PredictionIO {
  override def predict(startDateTime: DateTime, endDateTime: DateTime): Future[JsObject] = {
    // todo: make actual request to predictionio
    Future.successful {
      Json.obj(
        "demand" -> 74
      )
    }
  }
}
