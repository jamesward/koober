package services

import java.util.UUID
import javax.inject.{Inject, Singleton}

import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.scaladsl.{Sink, Source}
import helpers.KafkaHelper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import play.api.Configuration
import play.api.libs.json.JsValue


trait Kafka {
  def sink: Sink[ProducerRecord[String, JsValue], _]
  def source(topic: String): Source[ConsumerRecord[String, JsValue], _]
}

@Singleton
class KafkaImpl @Inject() (configuration: Configuration) extends Kafka {

  def producerSettings: ProducerSettings[String, JsValue] = {
    val keySerializer = new StringSerializer()

    val config = KafkaHelper.kafkaConfig(configuration.underlying, "akka.kafka.producer")

    ProducerSettings[String, JsValue](config, keySerializer, KafkaHelper.jsValueSerializer)
      .withBootstrapServers(KafkaHelper.kafkaUrl(configuration.underlying))
  }

  def consumerSettings: ConsumerSettings[String, JsValue] = {
    val keyDeserializer = new StringDeserializer()

    val config = KafkaHelper.kafkaConfig(configuration.underlying, "akka.kafka.consumer")

    ConsumerSettings(config, keyDeserializer, KafkaHelper.jsValueDeserializer)
      .withBootstrapServers(KafkaHelper.kafkaUrl(configuration.underlying))
      .withGroupId(UUID.randomUUID().toString)
  }

  def sink: Sink[ProducerRecord[String, JsValue], _] = {
    Producer.plainSink(producerSettings)
  }

  def source(topic: String): Source[ConsumerRecord[String, JsValue], _] = {
    val subscriptions = Subscriptions.topics(topic)
    Consumer.plainSource(consumerSettings, subscriptions)
  }

}
