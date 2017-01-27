name := "kafka-common"

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka-clients" % "0.10.0.1",
  "com.github.jkutner" % "env-keystore" % "0.1.2",
  "com.typesafe.play" %% "play-json" % "2.5.12" exclude("com.fasterxml.jackson.core", "jackson-databind")
)
