package cats.metrics.instrument

import cats.effect.Sync

private[instrument] trait PlatformHistogram {
  def apply[F[_]](name: String, dynamicRange: DynamicRange = DynamicRange.Default)(
    implicit F: Sync[F]
  ): F[Histogram[F]] = F.raiseError(new RuntimeException("Histograms are not supported in ScalaJS"))
}
