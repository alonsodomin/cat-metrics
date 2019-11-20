package cats.metrics.graphite

import java.time.Instant

import cats.Applicative
import cats.metrics.{Reporter, Snapshot}
import cats.implicits._

final class CarbonReporter[F[_]: Applicative](client: CarbonClient[F]) extends Reporter[F] {

  def flush(snapshot: Snapshot): F[Unit] =
    snapshot.counters
      .map(m => CarbonMetric(m.name, m.value.toString, Instant.now()))
      .traverse_(client.send)

}
