package cats
package metrics

import cats.effect.Sync
import cats.effect.concurrent._
import cats.syntax.functor._

trait Counter[F[_]] extends Instrument[F, Long] {
  def increment(): F[Unit]
  def incrementBy(amount: Long): F[Unit]
}

object Counter {

  def apply[F[_]](implicit F: Sync[F]): F[Counter[F]] = startAt[F](0)

  def startAt[F[_]](initial: Long)(implicit F: Sync[F]): F[Counter[F]] =
    Ref[F].of(initial).map(new Impl[F](initial, _))

  private class Impl[F[_]](initial: Long, value: Ref[F, Long]) extends Counter[F] {
    def increment(): F[Unit] = incrementBy(1)
    def incrementBy(amount: Long): F[Unit] =
      value.modify(curr => (curr + amount, ()))

    def peek(): F[Long] = value.get
    def reset(): F[Long] = value.getAndSet(initial)
  }

}