package cats.metrics.instrument

import cats.effect.Sync

import org.HdrHistogram.AtomicHistogram

trait Histogram[F[_]] extends Instrument[F] {
  type Value = Distribution[Long]

  def record(value: Long): F[Unit]

}

object Histogram {

  def apply[F[_]](name: String, dynamicRange: DynamicRange = DynamicRange.Default)(
      implicit F: Sync[F]
  ): F[Histogram[F]] = F.delay {
    val hist = new AtomicHistogram(
      dynamicRange.lowestDiscernibleValue,
      dynamicRange.highestTrackableValue,
      dynamicRange.significantValueDigits
    )
    new Impl(name, hist)
  }

  private class Impl[F[_]](val name: String, hist: AtomicHistogram)(implicit F: Sync[F])
      extends Histogram[F] {

    def get: F[Distribution[Long]] =
      F.delay(Distribution.fromHistogram(hist))

    def record(value: Long): F[Unit] = F.delay(hist.recordValue(value))

    def reset: F[Unit] = F.delay(hist.reset())

    def getAndReset: F[Distribution[Long]] = F.delay {
      val dist = Distribution.fromHistogram(hist)
      hist.reset()
      dist
    }

  }

}
