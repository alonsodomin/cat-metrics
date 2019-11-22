package cats.metrics

import cats.effect.IO

trait Reporter[F[_]] {
  def flush(snapshot: Snapshot): F[Unit]
}
object Reporter {
  def instance[F[_]](f: Snapshot => F[Unit]): Reporter[F] = new Reporter[F] {
    override def flush(snapshot: Snapshot): F[Unit] = f(snapshot)
  }

  def stdout: Reporter[IO] = instance[IO](snap => IO(println(snap.toString)))
}
