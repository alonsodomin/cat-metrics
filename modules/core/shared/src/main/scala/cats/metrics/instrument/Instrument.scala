package cats.metrics.instrument

trait Instrument[F[_]] {
  type Value

  def name: String

  def get: F[Value]
  def reset: F[Unit]

  def getAndReset: F[Value]
}

object Instrument {
  type Aux[F[_], Value0] = Instrument[F] { type Value = Value0 }
}
