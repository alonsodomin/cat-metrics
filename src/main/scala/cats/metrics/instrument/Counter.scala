package cats.metrics.instrument

import cats.effect._
import cats.effect.concurrent._
import cats.syntax.functor._

trait Counter[F[_]] extends Instrument[F] {
  type Value = Long

  def increment(): F[Unit]
  def incrementBy(amount: Long): F[Unit]
}

object Counter {

  def apply[F[_]: Sync : Timer](name: String, initial: Long = 0L): F[Counter[F]] =
    Ref[F].of(initial).map(new Impl[F](name, initial, _))

  private class Impl[F[_]: Sync: Timer](val name: String, initial: Long, value: Ref[F, Long]) extends Counter[F] {
    def increment(): F[Unit] = incrementBy(1)
    def incrementBy(amount: Long): F[Unit] =
      value.modify(curr => (curr + amount, ()))

    def get: F[Long] = value.get
  }

}