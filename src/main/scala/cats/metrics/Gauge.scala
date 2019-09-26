package cats
package metrics

import cats.effect.{Sync, Timer}
import cats.effect.concurrent.Ref
import cats.syntax.functor._

import fs2.Stream

import scala.concurrent.duration._

trait Gauge[F[_], A] extends Instrument[F, A] {
  def set(value: A): F[Unit]

  def data: Stream[F, A]
}

object Gauge {
  def of[F[_]: Sync: Timer, A: Numeric](initial: A): F[Gauge[F, A]] =
    Ref[F].of(initial).map(new Impl[F, A](initial, _))

  private class Impl[F[_]: Sync: Timer, A: Numeric](initial: A, value: Ref[F, A]) extends Gauge[F, A] {
    def set(v: A): F[Unit] = value.set(v)

    def peek(): F[A] = value.get
    def reset(): F[A] = value.getAndSet(initial)

    def data: Stream[F, A] = {
      Stream.awakeDelay[F](5.seconds).evalMap(_ => value.get)
    }
  }

}