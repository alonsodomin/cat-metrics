package cats.metrics.instrument

import cats.effect.concurrent.Ref
import cats.effect.{Sync, Timer}
import cats.syntax.functor._

trait Gauge[F[_]] extends Instrument[F] {
  type Value = Double

  def set(value: Double): F[Unit]
}

object Gauge {

  def apply[F[_]: Sync: Timer](name: String, initial: Double = 0.0): F[Gauge[F]] =
    Ref[F].of(initial).map(new Impl[F](name, initial, _))

  private class Impl[F[_]: Sync: Timer](val name: String, initial: Double, value: Ref[F, Double])
      extends Gauge[F] {
    def get: F[Double]          = value.get
    def set(v: Double): F[Unit] = value.set(v)
  }

}
