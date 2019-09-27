package cats.metrics

import cats.metrics.store.Snapshot

trait Reporter[F[_]] {
  def flush(snapshot: Snapshot): F[Unit]
}
