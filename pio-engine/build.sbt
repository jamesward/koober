import AssemblyKeys._

assemblySettings

name := "predictionio-load-forecasting"

libraryDependencies ++= Seq(
  "org.apache.predictionio"    %% "apache-predictionio-core" % "0.10.0-incubating" % "provided",
  "org.apache.spark"  %% "spark-core"   % "1.5.2"           % "provided",
  "org.apache.spark"  %% "spark-mllib"  % "1.5.2"           % "provided"
)
