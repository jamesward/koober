import java.nio.file.Files

import org.apache.cassandra.service.CassandraDaemon

object CassandraServer extends App {

  System.setProperty("cassandra.config", "cassandra.yaml")
  private val tmpCassandraDirectory: String = Files.createTempDirectory("cassandra").toString
  println(s"using cassandra tmp directory: $tmpCassandraDirectory")
  System.setProperty("cassandra.storagedir", tmpCassandraDirectory)
  System.setProperty("cassandra-foreground", "true")

  val cassandraDaemon = new CassandraDaemon()
  cassandraDaemon.activate()

}
