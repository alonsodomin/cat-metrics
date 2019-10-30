package cats.metrics.instrument

trait Instrument[F[_]] {
  type Value

  def get: F[Value]
  def reset: F[Unit]
}

object Instrument {
  type Aux[F[_], Value0] = Instrument[F] { type Value = Value0 }
}
