import AssemblyKeys._

assemblySettings

scalaVersion := "2.11.8"
val sparkVersion = "2.1.1"

name := "predictionio-load-forecasting"

libraryDependencies ++= Seq(
  "org.apache.predictionio" %% "apache-predictionio-core" % "0.11.0-incubating" % "provided",
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-mllib" % sparkVersion % "provided"
)
