package helpers

import com.github.jkutner.EnvKeyStore
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import play.api.libs.json.{JsValue, Json}

import scala.collection.JavaConverters._
import scala.util.Try

object KafkaHelper {

  lazy val sslConfig: Config = {
    val envTrustStore = EnvKeyStore.createWithRandomPassword("KAFKA_TRUSTED_CERT")
    val envKeyStore = EnvKeyStore.createWithRandomPassword("KAFKA_CLIENT_CERT_KEY", "KAFKA_CLIENT_CERT")

    val trustStore = envTrustStore.storeTemp()
    val keyStore = envKeyStore.storeTemp()

    ConfigFactory.parseMap(
      Map(
        "kafka-clients" -> Map(
          CommonClientConfigs.SECURITY_PROTOCOL_CONFIG -> "SSL",
          SslConfigs.SSL_CLIENT_AUTH_CONFIG -> "required",
          SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG -> envTrustStore.`type`(),
          SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG -> trustStore.getAbsolutePath,
          SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG -> envTrustStore.password(),
          SslConfigs.SSL_KEYSTORE_TYPE_CONFIG -> envKeyStore.`type`(),
          SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG -> keyStore.getAbsolutePath,
          SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG -> envKeyStore.password
        ).asJava
      ).asJava
    )
  }

  def maybeKafkaUrl(config: Config): Try[String] = {
    Try(config.getString("kafka.url")).map { kafkaUrl =>
      import java.net.URI

      val kafkaUrls = kafkaUrl.split(",").map { urlString =>
        val uri = new URI(urlString)
        Seq(uri.getHost, uri.getPort).mkString(":")
      }

      kafkaUrls.mkString(",")
    }
  }

  def kafkaUrl(config: Config = ConfigFactory.empty()): String = maybeKafkaUrl(config).getOrElse("localhost:9092")

  def kafkaConfig(config: Config, configKey: String): Config = {
    val baseConfig = Try(config.getConfig(configKey)).getOrElse(ConfigFactory.empty())
    maybeKafkaUrl(config).map(_ => baseConfig.withFallback(sslConfig)).getOrElse(baseConfig)
  }

  val jsValueSerializer = new JsValueSerializer()

  val jsValueDeserializer = new JsValueDeserializer()

}

class JsValueSerializer extends Serializer[JsValue] {
  override def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit = Unit
  override def close(): Unit = Unit
  override def serialize(topic: String, jsValue: JsValue): Array[Byte] = jsValue.toString().getBytes
}

class JsValueDeserializer extends Deserializer[JsValue] {
  override def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit = Unit
  override def close(): Unit = Unit
  override def deserialize(topic: String, data: Array[Byte]): JsValue = Json.parse(data)
}
