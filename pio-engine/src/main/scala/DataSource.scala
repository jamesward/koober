package edu.cs5152.predictionio.demandforecasting

import org.apache.predictionio.controller.PDataSource
import org.apache.predictionio.controller.EmptyEvaluationInfo
import org.apache.predictionio.controller.Params
import org.apache.predictionio.controller.SanityCheck
import org.apache.predictionio.data.store.PEventStore
import grizzled.slf4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.joda.time.DateTime

case class DataSourceParams(
                             appName: String,
                             evalK: Option[Double]
                           ) extends Params

class UserEvent(
  val eventTime:  DateTime,
  val lat:        Double,
  val lng:        Double
) extends Serializable

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData, EmptyEvaluationInfo, Query, ActualResult] {

  type UserEvents = RDD[UserEvent]

  @transient lazy val logger = Logger[this.type]

  def readData(sc: SparkContext): UserEvents = {
    PEventStore.find(
      appName = dsp.appName,
      entityType = Some("location"))(sc)
      .map { event =>
      try {
        new UserEvent(
          eventTime = event.eventTime,
          lat = event.properties.get[Double]("lat"),
          lng = event.properties.get[Double]("lng")
        )
      } catch {
        case e: Exception =>
          logger.error(s"Failed to get properties $event.properties of" +
            s" $event.entityId. Exception: $e.")
          throw e
      }
    }
      .cache()
  }

  override
  def readTraining(sc: SparkContext): TrainingData = {
    val data: UserEvents = readData(sc)
    new TrainingData(data)
  }

  override
  def readEval(sc: SparkContext)
  : Seq[(TrainingData, EmptyEvaluationInfo, RDD[(Query, ActualResult)])] = {
    require(dsp.evalK.nonEmpty, "DataSourceParams.evalK must not be None")

    val data: UserEvents = readData(sc) // have eventTime, lng and lat

    // splitting by time based on what evalK is. evalK should be the percent of data to be in the training points
    val evalK = dsp.evalK.get
    val sortedData = data.sortBy(ue=>ue.eventTime.getMillis());
    val indexedPoints: RDD[(UserEvent, Long)] = sortedData.zipWithIndex()
    val count = sortedData.count().toInt

    (0 until count).map { idx =>
      val trainingPoints = indexedPoints.filter(_._2 <= evalK*count).map(_._1)
      val testingPoints = indexedPoints.filter(_._2 > evalK*count).map(_._1)
      val testingNormalized = KooberUtil.createNormalizedMap(testingPoints)
      val testingCountMap = KooberUtil.createCountMap(testingNormalized.values)
      (
        new TrainingData(trainingPoints),
        new EmptyEvaluationInfo(),
        testingPoints.map {
          p => (new Query(p.eventTime.toString(), p.lat, p.lng),
            new ActualResult(testingCountMap.get(testingNormalized.filter(e=>e._1 == p.eventTime).collect()(0)._2).get))
        }
      )
    }
  }
}

class TrainingData(
  val data: RDD[UserEvent]
) extends Serializable with SanityCheck {


  override def sanityCheck(): Unit = {
    require(data.take(1).nonEmpty, s"data cannot be empty!")
  }
}
