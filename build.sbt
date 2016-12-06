name := "koober"

lazy val commonSettings = Seq(
  scalaVersion := "2.11.8"
)

lazy val kafkaServer = (project in file("kafka-server")).settings(commonSettings: _*)

lazy val cassandraServer = (project in file("cassandra-server")).settings(commonSettings: _*)

lazy val kafkaCommon = (project in file("kafka-common")).settings(commonSettings: _*)

lazy val webapp = (project in file("webapp")).settings(commonSettings: _*).dependsOn(kafkaCommon).enablePlugins(PlayScala, SbtWeb)

lazy val flinkClient = (project in file("flink-client")).settings(commonSettings: _*)

lazy val kafkaToCassandra = (project in file("kafka-to-cassandra")).settings(commonSettings: _*).dependsOn(kafkaCommon)
