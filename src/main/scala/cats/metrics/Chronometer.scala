package cats.metrics

import java.util.concurrent.TimeUnit

import cats.effect.{Clock, Sync, Timer}
import cats.effect.implicits._
import cats.implicits._

import scala.concurrent.duration.{FiniteDuration, TimeUnit}

trait Chronometer[F[_]] extends Instrument[F] {
  type Value = FiniteDuration

  def measure[A](fa: F[A]): F[A]

}

object Chronometer {
  def apply[F[_]: Sync](implicit timer: Timer[F]): F[Chronometer[F]] =
    of[F](TimeUnit.MICROSECONDS, DynamicRange.Default)

  def of[F[_]: Sync](precision: TimeUnit, dynamicRange: DynamicRange)(implicit timer: Timer[F]): F[Chronometer[F]] =
    Histogram.in[F](dynamicRange).map(new Impl[F](precision, _))

  private class Impl[F[_]](precision: TimeUnit, histogram: Histogram[F])(implicit F: Sync[F], clock: Clock[F]) extends Chronometer[F] {

    def measure[A](fa: F[A]): F[A] = {
      for {
        startTime <- captureTime
        result    <- fa.guarantee(reportElapsedTime(startTime))
      } yield result
    }

    def captureTime: F[Long] = clock.monotonic(precision)

    def reportElapsedTime(startTime: Long): F[Unit] = {
      for {
        duration <- captureTime.map(endTime => FiniteDuration(endTime - startTime, precision))
        _ <- histogram.record(duration.toUnit(precision).toLong)
      } yield ()
    }

    def subscribe(frequency: FiniteDuration): fs2.Stream[F, FiniteDuration] =
      histogram.subscribe(frequency).map(FiniteDuration(_, precision))
  }

}
