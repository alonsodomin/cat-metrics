package cats
package metrics

import cats.effect.{Sync, Timer}
import cats.effect.concurrent.Ref
import cats.syntax.functor._

import fs2.Stream

import scala.concurrent.duration._

trait Gauge[F[_]] extends Instrument[F] {
  def set(value: Value): F[Unit]
}

object Gauge {
  type Aux[F[_], A0] = Gauge[F] { type Value = A0 }

  def of[F[_]: Sync: Timer, A](initial: A): F[Gauge[F]] =
    Ref[F].of(initial).map(new Impl[F, A](initial, _))

  private class Impl[F[_]: Sync: Timer, A](initial: A, value: Ref[F, A]) extends Gauge[F] {
    type Value = A

    def set(v: A): F[Unit] = value.set(v)

    def peek(): F[A] = value.get
    def reset(): F[A] = value.getAndSet(initial)

    def subscribe(frequency: FiniteDuration): Stream[F, A] = {
      Stream.awakeEvery[F](frequency).evalMap(_ => value.get)
    }
  }

}