name := "pio-data-s3"

libraryDependencies ++= Seq(
  "jp.co.bizreach" %% "aws-s3-scala" % "0.0.11",
  "org.apache.predictionio" %% "apache-predictionio-data" % "0.11.0-incubating" % "provided"
)

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

assemblyJarName in assembly := "pio-s3.jar"
