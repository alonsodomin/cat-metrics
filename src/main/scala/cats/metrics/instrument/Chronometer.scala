package cats.metrics.instrument

import java.util.concurrent.TimeUnit

import cats.data.Nested
import cats.effect.implicits._
import cats.effect.{Clock, Sync, Timer}
import cats.implicits._

import scala.concurrent.duration.{FiniteDuration, TimeUnit}

trait Chronometer[F[_]] extends Instrument[F] {
  type Value = Distribution[FiniteDuration]

  def measure[A](fa: F[A]): F[A]

}

object Chronometer {

  def apply[F[_]: Sync](
      name: String,
      precision: TimeUnit = TimeUnit.MICROSECONDS,
      dynamicRange: DynamicRange = DynamicRange.Default
  )(implicit timer: Timer[F]): F[Chronometer[F]] =
    Histogram[F](name, dynamicRange).map(new Impl[F](name, precision, _))

  private class Impl[F[_]](val name: String, precision: TimeUnit, histogram: Histogram[F])(
      implicit F: Sync[F],
      clock: Clock[F]
  ) extends Chronometer[F] {

    def get: F[Distribution[FiniteDuration]] =
      histogram.get.map(_.map(FiniteDuration(_, precision)))

    def measure[A](fa: F[A]): F[A] =
      for {
        startTime <- captureTime
        result    <- fa.guarantee(reportElapsedTime(startTime))
      } yield result

    def captureTime: F[Long] = clock.monotonic(precision)

    def reportElapsedTime(startTime: Long): F[Unit] =
      for {
        duration <- captureTime.map(endTime => FiniteDuration(endTime - startTime, precision))
        _        <- histogram.record(duration.toUnit(precision).toLong)
      } yield ()

  }

}
