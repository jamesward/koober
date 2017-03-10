import com.google.inject.AbstractModule
import services.{PredictionIO, PredictionIOImpl}

class Module extends AbstractModule {

  override def configure() = {
    bind(classOf[PredictionIO]).to(classOf[PredictionIOImpl])
  }

}
