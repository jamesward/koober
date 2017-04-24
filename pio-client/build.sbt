name := "pio-client"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.14",
  "com.typesafe.play" %% "play-json" % "2.5.12",
  "com.typesafe.play" %% "play-ws" % "2.5.12"
)
