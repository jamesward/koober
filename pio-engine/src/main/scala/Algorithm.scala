package edu.cs5152.predictionio.demandforecasting

import grizzled.slf4j.Logger
import org.apache.predictionio.controller.{P2LAlgorithm, Params}
import org.apache.spark.SparkContext
import org.apache.spark.mllib.regression.{LinearRegressionModel, LinearRegressionWithSGD}
import org.joda.time.DateTime

case class AlgorithmParams(
  iterations:        Int    = 10000,
  regParam:          Double = 0.0,
  miniBatchFraction: Double = 1.0, 
  stepSize:          Double = 0.1
) extends Params

class Algorithm(val ap: AlgorithmParams)
  extends P2LAlgorithm[PreparedData, Model, Query, PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  def train(sc: SparkContext, preparedData: PreparedData): Model = {
    val lin = new LinearRegressionWithSGD()
    lin.setIntercept(true)
    lin.optimizer
      .setNumIterations(ap.iterations)
      .setRegParam(ap.regParam)
      .setMiniBatchFraction(ap.miniBatchFraction)
      .setStepSize(ap.stepSize)
    new Model(lin.run(preparedData.data))
  }

  def predict(model: Model, query: Query): PredictedResult = {
    val label : Double = model.predict(query)
    new PredictedResult(label)
  }
}

class Model(mod: LinearRegressionModel) extends Serializable { // will not be DateTime after changes
                                                                                  // to Preparator
  @transient lazy val logger = Logger[this.type]

  def predict(query: Query): Double = {
    val features = Preparator.toFeaturesVector(new DateTime(query.eventTime))
    mod.predict(features)
  }
}

