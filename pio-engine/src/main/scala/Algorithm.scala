package detrevid.predictionio.loadforecasting

import io.prediction.controller.P2LAlgorithm
import io.prediction.controller.Params

import grizzled.slf4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.mllib.regression.{LinearRegressionModel, LinearRegressionWithSGD}

case class AlgorithmParams(
  iterations:        Int    = 10000,
  regParam:          Double = 0.0,
  miniBatchFraction: Double = 1.0, 
  stepSize:          Double = 0.1
) extends Params

class Algorithm(val ap: AlgorithmParams)
  extends P2LAlgorithm[PreparedData, Model, Query, PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  def train(sc: SparkContext, data: PreparedData): Model = {
    val lin = new LinearRegressionWithSGD()
    lin.setIntercept(true)
    lin.optimizer
      .setNumIterations(ap.iterations)
      .setRegParam(ap.regParam)
      .setMiniBatchFraction(ap.miniBatchFraction)
      .setStepSize(ap.stepSize)

    val mod: Map[Int, LinearRegressionModel] = (data.circuitsIds map {
      cid =>
        val circuitData = data.data filter {_._1 == cid} map {_._2} cache()
        (cid, lin.run(circuitData))
    }).toMap

    new Model(mod)
  }

  def predict(model: Model, query: Query): PredictedResult = {
    val label : Double = model.predict(query)
    new PredictedResult(label)
  }
}

class Model(val mod: Map[Int, LinearRegressionModel]) extends Serializable {

  @transient lazy val logger = Logger[this.type]

  def predict(query: Query): Double = {
    val features = Preparator.toFeaturesVector(query.circuitId, query.timestamp)
    mod(query.circuitId).predict(features)
  }
}

