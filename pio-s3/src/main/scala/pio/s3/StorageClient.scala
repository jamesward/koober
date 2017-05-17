package pio.s3

import awscala.Region
import grizzled.slf4j.Logging
import jp.co.bizreach.s3scala.S3
import org.apache.predictionio.data.storage.{BaseStorageClient, StorageClientConfig}

class StorageClient(val config: StorageClientConfig) extends BaseStorageClient with Logging {
  override val prefix = "S3"

  private val accessKeyId = config.properties("ACCESS_KEY_ID")
  private val secretAccessKey = config.properties("SECRET_ACCESS_KEY")

  override val client: awscala.s3.S3 = S3(accessKeyId, secretAccessKey)(Region.US_EAST_1)
}
