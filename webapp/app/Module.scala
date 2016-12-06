import com.google.inject.AbstractModule
import services.{Kafka, KafkaImpl}

class Module extends AbstractModule {

  override def configure() = {
    bind(classOf[Kafka]).to(classOf[KafkaImpl])
  }

}
