package cats.metrics.instrument

import cats.effect.Sync
import org.HdrHistogram.AtomicHistogram

private[instrument] trait PlatformHistogram {

  def apply[F[_]](name: String, dynamicRange: DynamicRange = DynamicRange.Default)(
    implicit F: Sync[F]
  ): F[Histogram[F]] = F.delay {
    val hist = new AtomicHistogram(
      dynamicRange.lowestDiscernibleValue,
      dynamicRange.highestTrackableValue,
      dynamicRange.significantValueDigits
    )
    new JVMHistogram[F](name, hist)
  }

  private[instrument] final class JVMHistogram[F[_]](val name: String, hist: AtomicHistogram)(implicit F: Sync[F]) extends Histogram[F] {
    def get: F[Distribution[Long]] =
      F.delay(distributionFromHistogram(hist))

    def record(value: Long): F[Unit] = F.delay(hist.recordValue(value))

    def reset: F[Unit] = F.delay(hist.reset())

    def getAndReset: F[Distribution[Long]] = F.delay {
      val dist = distributionFromHistogram(hist)
      hist.reset()
      dist
    }
  }

  def distributionFromHistogram(hist: org.HdrHistogram.Histogram): Distribution[Long] =
    Distribution(hist.getTotalCount, hist.getMinValue, hist.getMaxValue)
}
