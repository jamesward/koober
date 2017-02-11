package detrevid.predictionio.loadforecasting

import org.apache.predictionio.controller.PDataSource
import org.apache.predictionio.controller.EmptyEvaluationInfo
import org.apache.predictionio.controller.Params
import org.apache.predictionio.controller.SanityCheck
import org.apache.predictionio.data.store.PEventStore

import grizzled.slf4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class DataSourceParams(
  appName: String,
  evalK: Option[Int]
) extends Params

class UserEvent(
  val eventTime:  Long,
  val lat:        Double,
  val lng:        Double
) extends Serializable

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData, EmptyEvaluationInfo, Query, ActualResult] {

  type UserEvents = RDD[UserEvent]

  @transient lazy val logger = Logger[this.type]

  def readData(sc: SparkContext): UserEvents = {
    PEventStore.aggregateProperties(
      appName = dsp.appName,
      entityType = "location",
      // only keep entities with these required properties
      required = Some(List("eventTime", "lat", "lng")))(sc)
      .map { case (entityId, eventTime, properties) =>
      try {
        new UserEvent(
          eventTime=eventTime,
          lat=properties.get[Double]("lat"),
          lng=properties.get[Double]("lng")
        )
      } catch {
        case e: Exception =>
          logger.error(s"Failed to get properties $properties of" +
            s" $entityId. Exception: $e.")
          throw e
      }
    }.cache()
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

    val data: UserEvents = readData(sc)

    // K-fold splitting
    val evalK = dsp.evalK.get
    val indexedPoints: RDD[(UserEvent, Long)] = data.zipWithIndex()

    (0 until evalK).map { idx =>
      val trainingPoints = indexedPoints.filter(_._2 % evalK != idx).map(_._1)
      val testingPoints = indexedPoints.filter(_._2 % evalK == idx).map(_._1)

      (
        new TrainingData(trainingPoints),
        new EmptyEvaluationInfo(),
        testingPoints.map {
          p => (new Query(p.eventTime, p.lat, p.lng), new ActualResult(p.demand))
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
