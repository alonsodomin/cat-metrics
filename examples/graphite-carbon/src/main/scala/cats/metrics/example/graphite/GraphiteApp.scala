package cats.metrics.example.graphite

import java.net.InetSocketAddress
import java.util.concurrent.Executors

import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}
import cats.metrics.Monitor
import cats.metrics.graphite.{CarbonClient, CarbonReporter}
import cats.metrics.store.Registry
import cats.implicits._

import scala.concurrent.duration._

object GraphiteApp extends IOApp {
  val CarbonAddress = new InetSocketAddress("localhost", 2003)

  def run(args: List[String]): IO[ExitCode] = {
    Registry[IO].flatMap { registry =>
      prepareMonitor(registry).use { _ =>
        mainCode(registry)
      }
    }.as(ExitCode.Success)
  }

  def mainCode(registry: Registry[IO]): IO[Unit] = {
    for {
      chrono <- registry.chronometer("chrono")
      _ <- chrono.measure(operationToMeasure)
      _ <- IO.sleep(5.seconds)
    } yield ()
  }

  def operationToMeasure: IO[Unit] = IO.sleep(3.seconds)

  private def prepareMonitor(registry: Registry[IO]): Resource[IO, Monitor[IO]] = {
    val createBlocker = Resource.make(IO(Executors.newCachedThreadPool()))(x => IO(x.shutdown()))
      .map(Blocker.liftExecutorService)

    for {
      blocker <- createBlocker
      reporter  <- CarbonClient[IO](CarbonAddress, blocker).map(new CarbonReporter[IO](_))
      monitor <- Monitor[IO](registry)
      _ <- Resource.liftF(monitor.attach(reporter))
    } yield monitor
  }

}
