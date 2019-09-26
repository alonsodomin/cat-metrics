package cats.metrics

import java.util.concurrent.TimeUnit

import cats.effect.{Clock, Concurrent}
import cats.effect.implicits._
import cats.implicits._

import fs2.Stream
import fs2.concurrent.Topic

import scala.concurrent.duration.{FiniteDuration, TimeUnit}

trait Chronometer[F[_]] {

  def measure[A](fa: F[A]): F[A]

  def data: Stream[F, FiniteDuration]

}

object Chronometer {
  def apply[F[_]: Concurrent](implicit clock: Clock[F]): F[Chronometer[F]] = of[F](TimeUnit.MICROSECONDS)

  def of[F[_]: Concurrent](precision: TimeUnit)(implicit clock: Clock[F]): F[Chronometer[F]] =
    Topic[F, FiniteDuration](FiniteDuration(0, precision)).map(new Impl[F](precision, _))

  private class Impl[F[_]](precision: TimeUnit, topic: Topic[F, FiniteDuration])(implicit F: Concurrent[F], clock: Clock[F]) extends Chronometer[F] {

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
        _ <- topic.publish1(duration)
      } yield ()
    }

    override def data: Stream[F, FiniteDuration] = topic.subscribe(1)
  }

}
