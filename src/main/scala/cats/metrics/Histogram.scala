package cats.metrics

import cats.effect.{Sync, Timer}
import cats.implicits._

import fs2.Stream

import org.HdrHistogram.AtomicHistogram

import scala.concurrent.duration.FiniteDuration

trait Histogram[F[_]] extends Instrument[F] {
  type Value = Long

  def record(value: Long): F[Unit]

}

object Histogram {

  def apply[F[_]: Sync: Timer]: F[Histogram[F]] = in[F](dynamicRange = DynamicRange.Default)

  def in[F[_]: Timer](dynamicRange: DynamicRange)(implicit F: Sync[F]): F[Histogram[F]] =
    F.delay(new AtomicHistogram(dynamicRange.lowestDiscernibleValue, dynamicRange.highestTrackableValue, dynamicRange.significantValueDigits))
      .map(new Impl(_))

  private class Impl[F[_]: Timer](hist: AtomicHistogram)(implicit F: Sync[F]) extends Histogram[F] {
    def record(value: Long): F[Unit] = F.delay(hist.recordValue(value))

    def subscribe(frequency: FiniteDuration): Stream[F, Long] =
      Stream.awakeEvery[F](frequency).evalMap(_ => F.delay(hist.getTotalCount))
  }

}
