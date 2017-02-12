package edu.cs5152.predictionio.demandforecasting

import grizzled.slf4j.Logger
import org.apache.predictionio.controller.{PPreparator, SanityCheck}
import org.apache.spark.SparkContext
import org.apache.spark.mllib.clustering.KMeans
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
    val clusters = KMeans.train(locationData, numClusters, numIterations)

    val clusterLabelData = trainingData.data map { ev =>
      (ev.eventTime -> clusters.predict(Vectors.dense(ev.lat, ev.lng)))}
    Preparator.clusterLabels = Some(clusterLabelData) // store them statically so that we can use them when querying

    val eventTimes = trainingData.data map { _.eventTime } distinct () collect ()

    val timeMap = trainingData.data map { ev =>
      (ev.eventTime, normalize(ev.eventTime, "minute"))}  //CHANGE THIS to change the granularity of how to group demands

    val countMap = timeMap.values map (
      (normalizedTime) => (normalizedTime, 1)
    ) countByKey()

    val data = timeMap map { timeMapEntry =>
      LabeledPoint(countMap.get(timeMapEntry._2).get, Preparator.toFeaturesVector(timeMapEntry._1))
    } cache ()


    new PreparedData(data)
  }

  /**
    * Based on different metrics, try to normalize the event time into a long so we can calculate demand
    * @param eventTime
    * @param metric
    *        Supports: minute, fiveMinutes, halfHour, hour
    * @return
    */
  def normalize(eventTime: DateTime, metric: String): Long ={
    metric match{
      case "minute"         => eventTime.getMillis()/(1000*60)
      case "fiveMinutes"    => eventTime.getMillis()/(1000*60*5)
      case "halfHour"       => eventTime.getMillis()/(1000*60*30)
      case "hour"           => eventTime.getMillis()/(1000*60*60)
      case _                => throw new NotImplementedError("This normalization method is not implemented")
    }
  }
}

object Preparator {

  @transient lazy val logger = Logger[this.type]
  var clusterLabels = null:Option[RDD[(DateTime, Int)]]

  def toFeaturesVector(eventTime: DateTime): Vector = {
    Vectors.dense(
      Array(
        eventTime.dayOfWeek().get().toDouble,
        eventTime.dayOfMonth().get().toDouble,
        eventTime.minuteOfDay().get().toDouble,
        eventTime.monthOfYear().get().toDouble,
        clusterLabels.get.collectAsMap().get(eventTime).get.toDouble
      )) //will be changed when Preparator is properly implemented
  }
}
