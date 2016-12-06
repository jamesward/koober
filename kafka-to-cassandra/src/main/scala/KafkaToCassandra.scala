import java.util.UUID

import com.datastax.driver.core.{Cluster, ConsistencyLevel}
import com.datastax.spark.connector.streaming._
import com.datastax.spark.connector.writer.WriteConf
import com.datastax.spark.connector.SomeColumns
import helpers.{JsValueDeserializer, KafkaHelper}
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}
import play.api.libs.json.JsValue

object KafkaToCassandra extends App {

  // setup cassandra
  val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()
  val cassandraSession = cluster.connect()
  cassandraSession.execute("CREATE KEYSPACE IF NOT EXISTS koober WITH replication = {'class':'SimpleStrategy', 'replication_factor':3};")
  cassandraSession.execute("CREATE TABLE IF NOT EXISTS koober.routes (uid uuid primary key, driver uuid, rider uuid, lnglat map<text, float>, status text, route text);")
  cluster.close()


  val conf = new SparkConf(true).set("spark.cassandra.connection.host", "127.0.0.1").set("spark.sql.warehouse.dir", "spark-warehouse")
  val context = new SparkContext("local", "KafkaToCassandra", conf)
  val streamingContext = new StreamingContext(context, Seconds(1))
  val sparkSession = SparkSession.builder.config(conf).getOrCreate()

  import sparkSession.implicits._

  val kafkaParams = Map(
    "bootstrap.servers" -> KafkaHelper.kafkaUrl(),
    "key.deserializer" -> classOf[StringDeserializer],
    "value.deserializer" -> classOf[JsValueDeserializer],
    "group.id" -> "kafka-to-cassandra"
  )

  val ls = LocationStrategies.PreferBrokers
  val cs = ConsumerStrategies.Subscribe[String, JsValue](List("driver", "rider"), kafkaParams)
  val rawKafkaStream = KafkaUtils.createDirectStream(streamingContext, ls, cs)

  case class Route(uid: String, driver: Option[String], rider: Option[String], lnglat: Map[String, Float], status: Option[String], route: Option[String])

  val jobStream = rawKafkaStream.map { consumerRecord =>
    Route(
      UUID.randomUUID().toString,
      (consumerRecord.value() \ "driver").asOpt[String],
      (consumerRecord.value() \ "rider").asOpt[String],
      (consumerRecord.value() \ "lngLat").asOpt[Map[String, Float]].getOrElse(Map.empty[String, Float]),
      (consumerRecord.value() \ "status").asOpt[String],
      (consumerRecord.value() \ "route").asOpt[String]
    )
  }

  val columnMapping = SomeColumns("uid", "driver", "rider", "lnglat", "status", "route")
  val cassandraWriteConf = WriteConf.fromSparkConf(conf).copy(consistencyLevel = ConsistencyLevel.ONE)
  jobStream.saveToCassandra("koober", "routes", columnMapping, cassandraWriteConf)
  jobStream.foreachRDD(_.toDF().show())

  streamingContext.start()
  streamingContext.awaitTermination()

}
