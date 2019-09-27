scalaVersion := "2.12.9"

addCompilerPlugin("com.olegpy"      %% "better-monadic-for" % "0.3.1")
addCompilerPlugin("org.typelevel"   %% "kind-projector"     % "0.10.3")
addCompilerPlugin("org.scalamacros" %% "paradise"           % "2.1.1" cross CrossVersion.full)

libraryDependencies ++= Seq(
  "org.hdrhistogram"           % "HdrHistogram"   % "2.1.11",
  "com.github.julien-truffaut" %% "monocle-core"  % "2.0.0",
  "com.github.julien-truffaut" %% "monocle-macro" % "2.0.0",
  "org.typelevel"              %% "cats-effect"   % "2.0.0",
  "co.fs2"                     %% "fs2-core"      % "2.0.0"
)

initialCommands in console += Seq(
  "import cats._",
  "import cats.implicits._",
  "import cats.effect._",
  "import scala.concurrent.duration._",
  "import scala.concurrent.ExecutionContext",
  "implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)",
  "implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)"
).mkString("\n")
