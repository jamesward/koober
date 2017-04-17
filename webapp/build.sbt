name := "webapp"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.14",
  "org.webjars" % "vue" % "2.1.3",
  "org.webjars" % "ionicons" % "2.0.1",
  "org.webjars.npm" % "polyline" % "0.2.0"
)

pipelineStages := Seq(digest)
