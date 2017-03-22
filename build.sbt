name := "koober"

lazy val commonSettings = Seq(
  scalaVersion := "2.11.8",

  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % "test"
  )
)

lazy val kafkaServer = (project in file("kafka-server")).settings(commonSettings: _*)

lazy val cassandraServer = (project in file("cassandra-server")).settings(commonSettings: _*)

lazy val kafkaCommon = (project in file("kafka-common")).settings(commonSettings: _*)

lazy val webapp = (project in file("webapp")).settings(commonSettings: _*).dependsOn(kafkaCommon).enablePlugins(PlayScala, SbtWeb)

lazy val flinkClient = (project in file("flink-client")).settings(commonSettings: _*)

lazy val kafkaToCassandra = (project in file("kafka-to-cassandra")).settings(commonSettings: _*).dependsOn(kafkaCommon)

lazy val pioClient = (project in file("pio-client")).settings(commonSettings: _*).dependsOn(kafkaCommon)

lazy val demoData = (project in file("demo-data")).settings(commonSettings: _*).dependsOn(kafkaCommon, pioClient)

lazy val demandDashboard = (project in file("demand-dashboard")).settings(commonSettings: _*).enablePlugins(PlayScala, SbtWeb)
