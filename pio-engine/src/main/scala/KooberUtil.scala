package edu.cs5152.predictionio.demandforecasting


import grizzled.slf4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.joda.time.DateTime

/**
  * Created by YitingWang on 2/12/17.
  */
object KooberUtil {

  /**
    * create a map from eventTime to Normalized eventTime
    * @param values
    * @return
    */
  def createNormalizedMap(values:RDD[UserEvent]): RDD[(DateTime, Long)] ={
    values map { ev =>
      (ev.eventTime, normalize(ev.eventTime, "minute"))}//CHANGE THIS to change the granularity of how to group demands
  }

  /**
    * create a map from normalized eventTime to the count (demand)
    * @param values
    * @return
    */
  def createCountMap(values: RDD[Long])={
    values map ((normalizedTime) => (normalizedTime, 1)) countByKey()
  }

  def prepareDemand(trainingData: RDD[UserEvent]) = {
    val eventTimes = trainingData map { _.eventTime } distinct () collect ()

    val timeMap = createNormalizedMap(trainingData)

    val countMap = createCountMap(timeMap.values)

    val data = timeMap map { timeMapEntry =>
      LabeledPoint(countMap.get(timeMapEntry._2).get, PreparatorObject.toFeaturesVector(timeMapEntry._1))
    } cache ()


    data
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

object PreparatorObject {

  @transient lazy val logger = Logger[this.type]

  def toFeaturesVector(eventTime: DateTime): Vector = {
    Vectors.dense(
      Array(
        eventTime.dayOfWeek().get().toDouble,
        eventTime.dayOfMonth().get().toDouble,
        eventTime.minuteOfDay().get().toDouble,
        eventTime.monthOfYear().get().toDouble
      )) //will be changed when Preparator is properly implemented
  }
}
