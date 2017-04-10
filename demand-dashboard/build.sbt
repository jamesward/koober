name := "demand-dashboard"

libraryDependencies ++= Seq(
  ws,
  "org.webjars" % "jquery-ui" % "1.12.1",
  "org.webjars" % "jquery" % "3.1.1"
)

pipelineStages := Seq(digest)
