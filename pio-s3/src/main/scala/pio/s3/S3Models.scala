package pio.s3

import java.io.ByteArrayInputStream

import awscala.s3.S3
import com.amazonaws.services.s3.model.ObjectMetadata
import grizzled.slf4j.Logging
import org.apache.predictionio.data.storage.{Model, Models, StorageClientConfig}

import scala.io.Source
import scala.util.Try

class S3Models(s3: S3, config: StorageClientConfig, prefix: String) extends Models with Logging {

  private val s3BucketName = config.properties("BUCKET_NAME")

  def insert(model: Model): Unit = {
    val objectMetadata = new ObjectMetadata()
    val inputStream = new ByteArrayInputStream(model.models)
    s3.putObject(s3BucketName, model.id, inputStream, objectMetadata)
    inputStream.close()
  }

  def get(id: String): Option[Model] = {
    Try(s3.getObject(s3BucketName, id)).toOption.map { s3Object =>
      val inputStream = s3Object.getObjectContent
      val byteArray = Source.fromInputStream(inputStream).map(_.toByte).toArray
      inputStream.close()
      Model(id, byteArray)
    }
  }

  def delete(id: String): Unit = {
    s3.deleteObject(s3BucketName, id)
  }

}
