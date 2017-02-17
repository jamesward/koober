package edu.cs5152.predictionio.demandforecasting

import org.apache.predictionio.controller.{Engine, EngineFactory}
import org.joda.time.DateTime

class Query(
             val eventTime: String,
             val lng: Double,
             val lat: Double

           ) extends Serializable

class PredictedResult(
                       val demand: Double
                     ) extends Serializable

class ActualResult(
                    val demand: Double
                  ) extends Serializable

object ForecastingEngine extends EngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("alg" -> classOf[Algorithm]),
      classOf[Serving])
  }
}
