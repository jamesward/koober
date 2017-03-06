package edu.cs5152.predictionio.demandforecasting


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
      (ev.eventTime, normalize(ev.eventTime, "hour"))}//CHANGE THIS to change the granularity of how to group demands
  }

  /**
    * create a map from normalized eventTime to the count (demand)
    * @param values
    * @return
    */
  def createCountMap(values: RDD[Long])={
    values map ((normalizedTime) => (normalizedTime, 1)) countByKey()
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

  def bool2int(b:Boolean) = if (b) 1 else 0


  def convertIntToBinaryArray(n:Int, size:Int):Array[Double] = {
    val ret = new Array[Double](size)
    for( a <- 0 to (size-1)){
      ret.update(a, bool2int(n == a))
    }
    ret
  }
}

