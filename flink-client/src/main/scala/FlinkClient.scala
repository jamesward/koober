import java.util.Properties

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010
import org.apache.flink.streaming.util.serialization.DeserializationSchema
import play.api.libs.json.{JsValue, Json}

object FlinkClient extends App {

  val env = StreamExecutionEnvironment.createLocalEnvironment()

  val kafkaConsumerProperties = new Properties()
  kafkaConsumerProperties.setProperty("bootstrap.servers", "localhost:9092")
  kafkaConsumerProperties.setProperty("group.id", "flink")

  val deserializationSchema = new DeserializationSchema[JsValue] {
    override def isEndOfStream(nextElement: JsValue): Boolean = false

    override def deserialize(message: Array[Byte]): JsValue = Json.parse(message)

    override def getProducedType: TypeInformation[JsValue] = createTypeInformation[JsValue]
  }

  val kafkaConsumer = new FlinkKafkaConsumer010[JsValue]("driver", deserializationSchema, kafkaConsumerProperties)

  case class RouteTotals(num: Int = 0, seconds: Int = 0) {
    lazy val average: Int = seconds / num
  }

  val stream = env.addSource(kafkaConsumer)

  val onlyRoutesStream = stream.flatMap(_.\("route").toOption).keyBy(_ => "") // todo: should partition by broad location

  val routeTotalsStream = onlyRoutesStream.fold(RouteTotals()) { case (routeTotals, jsValue) =>
    val seconds = (jsValue \ "duration").as[Float].toInt
    routeTotals.copy(routeTotals.num + 1, routeTotals.seconds + seconds)
  }

  routeTotalsStream.map(_.average).print()

  env.execute()

}
