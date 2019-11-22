package cats.metrics.instrument

trait Histogram[F[_]] extends Instrument[F] {
  type Value = Distribution[Long]

  def record(value: Long): F[Unit]

}

object Histogram extends PlatformHistogram
