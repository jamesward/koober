import awscala.s3.S3
import com.amazonaws.services.s3.model.ObjectMetadata
import grizzled.slf4j.Logging
import org.apache.predictionio.data.storage.{Model, Models, StorageClientConfig}

import scala.io.Source

class S3Models(s3: S3, config: StorageClientConfig, prefix: String) extends Models with Logging {

  println(s3, config.properties)

  private val s3Bucket = s3.bucket(config.properties("BUCKET_NAME")).get

  println(s3Bucket)

  def insert(model: Model): Unit = {
    val objectMetadata = new ObjectMetadata()
    s3Bucket.putObject(model.id, model.models, objectMetadata)(s3)
  }

  def get(id: String): Option[Model] = {
    s3Bucket.getObject(id)(s3).map { s3Object =>
      val byteArray = Source.fromInputStream(s3Object.content).map(_.toByte).toArray
      s3Object.content.close()
      Model(id, byteArray)
    }
  }

  def delete(id: String): Unit = {
    s3Bucket.delete(id)(s3)
  }

}
