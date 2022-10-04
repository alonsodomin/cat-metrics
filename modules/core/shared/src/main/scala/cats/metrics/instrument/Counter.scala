package cats.metrics.instrument

import cats.effect._
import cats.syntax.functor._

trait Counter[F[_]] extends Instrument[F] {
  type Value = Long

  def increment(): F[Unit] = incrementBy(1)
  def incrementBy(amount: Long): F[Unit]
}

object Counter {

  def apply[F[_]: Sync](name: String, initial: Long = 0L): F[Counter[F]] =
    Ref[F].of(initial).map(new Impl[F](name, initial, _))

  private class Impl[F[_]: Sync](val name: String, initial: Long, value: Ref[F, Long])
      extends Counter[F] {

    def incrementBy(amount: Long): F[Unit] =
      value.modify(curr => (curr + amount, ()))

    def get: F[Long] = value.get

    def reset: F[Unit] = value.set(initial)

    def getAndReset: F[Long] = value.getAndSet(initial)
  }

}
