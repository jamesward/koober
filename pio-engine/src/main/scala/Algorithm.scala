package edu.cs5152.predictionio.demandforecasting

import grizzled.slf4j.Logger
import org.apache.predictionio.controller.{CustomQuerySerializer, P2LAlgorithm, Params}
import org.apache.spark.SparkContext
import org.apache.spark.mllib.clustering.KMeansModel
import org.apache.spark.mllib.feature.StandardScalerModel
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.{LinearRegressionModel, LinearRegressionWithSGD}
import org.joda.time.DateTime

case class AlgorithmParams(
  iterations:        Int    = 20,
  regParam:          Double = 0.1,
  miniBatchFraction: Double = 1.0,
  stepSize:          Double = 0.001
) extends Params

class Algorithm(val ap: AlgorithmParams)
  extends P2LAlgorithm[PreparedData, Model, Query, PredictedResult] with MyQuerySerializer {

  @transient lazy val logger = Logger[this.type]

  def train(sc: SparkContext, preparedData: PreparedData): Model = {
    val lin = new LinearRegressionWithSGD()

    lin.setIntercept(true)
    lin.setValidateData(true)
    lin.optimizer
      .setNumIterations(1000)
      .setMiniBatchFraction(1.0)
      .setStepSize(0.001)
      .setRegParam(0.5)

// We can use the following sampling to reduce training set by sampling or increase training set by bootstrap
    val sample = preparedData.data.sample(true, 0.01).cache();
    sample.foreach(println)

    val linearRegressionModel = lin.run(preparedData.data)
    println(linearRegressionModel.intercept)
    println(linearRegressionModel.weights)
    new Model(linearRegressionModel, Preparator.locationClusterModel.get, Preparator.standardScalerModel.get)
  }

  def predict(model: Model, query: Query): PredictedResult = {
    val label : Double = model.predict(query)
    new PredictedResult(label)
  }
}

class Model(mod: LinearRegressionModel, locationClusterModel: KMeansModel, standardScalerModel: StandardScalerModel) extends Serializable {
  @transient lazy val logger = Logger[this.type]

  def predict(query: Query): Double = {
    val normalizedTimeFeatureVector = standardScalerModel.transform(Preparator.toFeaturesVector(DateTime.parse(query.eventTime), query.lat, query.lng))
    val locationClusterLabel = locationClusterModel.predict(Vectors.dense(query.lat, query.lng))
    val features = Preparator.toFeaturesVector(normalizedTimeFeatureVector, locationClusterLabel)
    mod.predict((features))
  }
}

trait MyQuerySerializer extends CustomQuerySerializer {
  @transient override lazy val querySerializer = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all
}

