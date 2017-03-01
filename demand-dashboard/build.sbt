name := "demand-dashboard"

libraryDependencies ++= Seq(
  ws,
  "org.webjars" % "vue" % "2.1.3"
)

pipelineStages := Seq(digest)
