scalaVersion := "2.12.9"

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")

libraryDependencies ++= Seq(
  "org.hdrhistogram" % "HdrHistogram" % "2.1.11",

  "org.typelevel" %% "cats-effect" % "2.0.0",
  "co.fs2" %% "fs2-core" % "2.0.0"
)