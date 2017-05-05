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
  iterations:        Int    = 500,
  regParam:          Double = 0.0,
  miniBatchFraction: Double = 0.5,
  stepSize:          Double = 0.01
) extends Params

class Algorithm(val ap: AlgorithmParams)
  extends P2LAlgorithm[PreparedData, Model, Query, PredictedResult] with MyQuerySerializer {

  @transient lazy val logger = Logger[this.type]

  def train(sc: SparkContext, preparedData: PreparedData): Model = {
    val lin = new LinearRegressionWithSGD()

    lin.setIntercept(true)
    lin.setValidateData(true)
    lin.optimizer
      .setNumIterations(ap.iterations)
      .setMiniBatchFraction(ap.miniBatchFraction)
      .setStepSize(ap.stepSize)
      .setRegParam(ap.regParam)

    val linearRegressionModel = lin.run(preparedData.data)
    new Model(linearRegressionModel, Preparator.locationClusterModel.get, Preparator.standardScalerModel.get)
  }

  def predict(model: Model, query: Query): PredictedResult = {
    val label : Double = model.predict(query)
    new PredictedResult(label, Map("algRegression" -> label))
  }
}

class Model(mod: LinearRegressionModel, locationClusterModel: KMeansModel, standardScalerModel: StandardScalerModel) extends Serializable {
  @transient lazy val logger = Logger[this.type]

  def predict(query: Query): Double = {
    val normalizedFeatureVector = standardScalerModel.transform(Preparator.toFeaturesVector(DateTime.parse(query.eventTime),
      query.temperature, query.clear, query.fog, query.rain, query.snow, query.hail, query.thunder, query.tornado))
    val locationClusterLabel = locationClusterModel.predict(Vectors.dense(query.lat, query.lng))
    val features = Preparator.combineFeatureVectors(normalizedFeatureVector, locationClusterLabel)
    mod.predict((features))
  }
}

trait MyQuerySerializer extends CustomQuerySerializer {
  @transient override lazy val querySerializer = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all
}

