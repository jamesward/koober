package edu.cs5152.predictionio.demandforecasting

import org.apache.predictionio.controller.{Engine, EngineFactory}
import org.joda.time.DateTime

class Query(
             val eventTime: String,
             val lng: Double,
             val lat: Double,
             val temperature: Double,
             val clear: Int,
             val fog: Int,
             val rain: Int,
             val snow: Int,
             val hail: Int,
             val thunder: Int,
             val tornado: Int,
             val heat: Double,
             val windchill: Double,
             val precipitation: Double

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
      Map("algRegression" -> classOf[Algorithm],
          "algGBTree"     -> classOf[AlgorithmGBTree],
          "multinomialLogistic" -> classOf[MultinomialLogisticRegressionAlgorithm]),
      classOf[Serving])
  }
}
//{
//  "name": "algGBTree",
//  "params": {
//  "iterations": 500,
//  "maxDepth": 10
//  }
//}
//,
//{
//  "name": "algRegression",
//  "params": {
//  "iterations": 4096,
//  "miniBatchFraction" : 1.0,
//  "regParam" : 0.0,
//  "stepSize": 0.9
//  }
//},
//{
//  "name": "multinomialLogistic",
//  "params": {
//  "iterations": 4096,
//  "numClasses" : 20,
//  "regParam" : 0.5,
//  "convergenceTol": 0.01
//  }
//}
