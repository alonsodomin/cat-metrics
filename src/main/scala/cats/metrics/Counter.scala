package cats
package metrics

import cats.effect._
import cats.effect.concurrent._
import cats.syntax.functor._

import fs2.Stream

import scala.concurrent.duration._

trait Counter[F[_]] extends Instrument[F, Long] {
  def increment(): F[Unit]
  def incrementBy(amount: Long): F[Unit]

  def data: Stream[F, Long]
}

object Counter {

  def apply[F[_]: Sync : Timer]: F[Counter[F]] = startAt[F](0)

  def startAt[F[_]: Sync : Timer](initial: Long): F[Counter[F]] =
    Ref[F].of(initial).map(new Impl[F](initial, _))

  private class Impl[F[_]: Sync: Timer](initial: Long, value: Ref[F, Long]) extends Counter[F] {
    def increment(): F[Unit] = incrementBy(1)
    def incrementBy(amount: Long): F[Unit] =
      value.modify(curr => (curr + amount, ()))

    def peek(): F[Long] = value.get
    def reset(): F[Long] = value.getAndSet(initial)

    def data: Stream[F, Long] = {
      Stream.awakeDelay[F](5.seconds).evalMap(_ => value.get)
    }
  }

}