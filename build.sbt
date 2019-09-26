scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "org.hdrhistogram" % "HdrHistogram" % "2.1.11",

  "org.typelevel" %% "cats-effect" % "2.0.0",
  "co.fs2" %% "fs2-core" % "2.0.0"
)