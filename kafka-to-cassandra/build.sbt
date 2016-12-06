name := "kafka-to-cassandra"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "2.0.2",
  "org.apache.spark" %% "spark-sql" % "2.0.2",
  "org.apache.spark" %% "spark-streaming" % "2.0.2",
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % "2.0.2",
  "com.datastax.spark" %% "spark-cassandra-connector" % "2.0.0-M3"
)
