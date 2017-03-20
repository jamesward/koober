name := "demand-dashboard"

libraryDependencies ++= Seq(
  ws,
  "org.webjars" % "jquery" % "3.1.1"
)

pipelineStages := Seq(digest)
