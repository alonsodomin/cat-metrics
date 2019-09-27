package cats.metrics

import cats.effect.{CancelToken, Concurrent, Fiber, Timer}
import cats.effect.concurrent.Ref
import cats.effect.implicits._
import cats.implicits._
import cats.metrics.store.{Snapshot, Store}

import fs2.Stream
import fs2.concurrent.Topic

import scala.concurrent.duration.FiniteDuration

trait Monitor[F[_]] {
  def attach(reporter: Reporter[F]): F[CancelToken[F]]
}

object Monitor {

  case class ReporterAlreadyDetached() extends Exception

  def apply[F[_]: Concurrent: Timer](
      store: Store[F],
      flushFrequency: FiniteDuration
  ): F[Monitor[F]] = {
    def startFlushing(topic: Topic[F, Snapshot]) =
      Stream
        .awakeEvery[F](flushFrequency)
        .evalMap(_ => store.snapshot)
        .filter(!_.isEmpty)
        .through(topic.publish)
        .compile
        .drain
        .start

    for {
      topic      <- Topic[F, Snapshot](Snapshot.Empty)
      flushFiber <- startFlushing(topic)
      reporters  <- Ref[F].of(Vector.empty[Fiber[F, Unit]])
    } yield new Impl[F](flushFiber, topic, reporters)
  }

  private class Impl[F[_]: Concurrent](
      flushFiber: Fiber[F, Unit],
      snapshotTopic: Topic[F, Snapshot],
      attachedFibers: Ref[F, Vector[Fiber[F, Unit]]]
  ) extends Monitor[F] {

    def attach(reporter: Reporter[F]): F[CancelToken[F]] = {
      def startReporter: F[Fiber[F, Unit]] =
        snapshotTopic.subscribe(1).evalMap(reporter.flush).compile.drain.start

      def detachReporterToken(idx: Long): F[CancelToken[F]] = {
        val reporterOpt = attachedFibers.get.map(_.get(idx))
        reporterOpt.map(_.map(_.cancel).getOrElse(ReporterAlreadyDetached().raiseError[F, Unit]))
      }

      for {
        fiber <- startReporter
        idx <- attachedFibers.modify { fibers =>
          val idx = fibers.size
          (fiber +: fibers, idx)
        }
        detachToken <- detachReporterToken(idx)
      } yield detachToken
    }

    def shutdown(): F[Unit] =
      for {
        _      <- flushFiber.cancel
        fibers <- attachedFibers.getAndSet(Vector.empty)
        _      <- fibers.traverse_(_.cancel)
      } yield ()
  }

}
