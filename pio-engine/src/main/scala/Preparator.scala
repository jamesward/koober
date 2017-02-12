package edu.cs5152.predictionio.demandforecasting


import org.apache.predictionio.controller.PPreparator
import org.apache.predictionio.controller.SanityCheck
import edu.cs5152.predictionio.demandforecasting.KooberUtil

import grizzled.slf4j.Logger
import org.apache.predictionio.controller.{PPreparator, SanityCheck}
import org.apache.spark.SparkContext
import org.apache.spark.mllib.clustering.{KMeansModel, KMeans}
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.joda.time.DateTime

class PreparedData(
    val data: RDD[LabeledPoint]
) extends Serializable
    with SanityCheck {


  override def sanityCheck(): Unit = {
    require(data.take(1).nonEmpty, s"data cannot be empty!")
  }
}

class Preparator extends PPreparator[TrainingData, PreparedData] {

  @transient lazy val logger = Logger[this.type]

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {

    // clustering coordinates and assign cluster labels.
    val locationData = trainingData.data map {entry => Vectors.dense(entry.lat, entry.lng)} distinct() cache()
    val numClusters = 5
    val numIterations = 20
    // store them statically so that we can use them when querying
    Preparator.clusterModel = Some(KMeans.train(locationData, numClusters, numIterations))

    val normalizedTime = KooberUtil.createNormalizedMap(trainingData.data)

    val countMap = normalizedTime.values.countByValue()
    val normalizedTimeMap = normalizedTime.collectAsMap()

    val data = trainingData.data map { trainingDataEntry =>
      LabeledPoint(countMap.get(normalizedTimeMap.get(trainingDataEntry.eventTime).get).get, Preparator.toFeaturesVector(trainingDataEntry.eventTime, trainingDataEntry.lat, trainingDataEntry.lng))
    } cache ()


    new PreparedData(data)
  }
}

object Preparator {

  @transient lazy val logger = Logger[this.type]
  var clusterModel = null:Option[KMeansModel]

  def toFeaturesVector(eventTime: DateTime, lat: Double, lng: Double): Vector = {
    Vectors.dense(
      Array(
        eventTime.dayOfWeek().get().toDouble,
        eventTime.dayOfMonth().get().toDouble,
        eventTime.minuteOfDay().get().toDouble,
        eventTime.monthOfYear().get().toDouble,
        clusterModel.get.predict(Vectors.dense(lat, lng)).toDouble
      )) //will be changed when Preparator is properly implemented
  }
}
