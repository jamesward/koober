name := "flink-client"

libraryDependencies ++= Seq(
  "com.github.jkutner" % "env-keystore" % "0.1.2",
  "org.apache.flink" %% "flink-streaming-scala" % "1.2.0",
  "org.apache.flink" %% "flink-connector-kafka-0.10" % "1.2.0",
  "com.typesafe.play" %% "play-json" % "2.5.12"
)
