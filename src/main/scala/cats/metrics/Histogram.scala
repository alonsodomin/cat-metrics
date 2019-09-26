package cats.metrics

import cats.effect.Sync
import cats.implicits._

import org.HdrHistogram.AtomicHistogram

trait Histogram[F[_]] {

  def record(value: Long): F[Unit]

}

object Histogram {

  def in[F[_]](dynamicRange: DynamicRange = DynamicRange.Default)(implicit F: Sync[F]): F[Histogram[F]] =
    F.delay(new AtomicHistogram(dynamicRange.lowestDiscernibleValue, dynamicRange.highestTrackableValue, dynamicRange.significantValueDigits))
      .map(new Impl(_))

  private class Impl[F[_]](hist: AtomicHistogram)(implicit F: Sync[F]) extends Histogram[F] {
    override def record(value: Long): F[Unit] = F.delay(hist.recordValue(value))
  }

}
