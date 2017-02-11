package detrevid.predictionio.loadforecasting

import org.apache.predictionio.controller.{Engine, EngineFactory}

class Query(
             val eventTime: Long,
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
