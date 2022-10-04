package cats.metrics.graphite

import java.nio.charset.StandardCharsets
import cats.effect.{Concurrent, Resource}
import com.comcast.ip4s.{Host, SocketAddress}
import fs2.Chunk
import fs2.io.net.{Network, Socket}

trait CarbonClient[F[_]] {
  def send(metric: CarbonMetric): F[Unit]
}
object CarbonClient {
  def apply[F[_]: Concurrent: Network](
      address: SocketAddress[Host],
  ): Resource[F, CarbonClient[F]] = {
    Network[F].client(address).map(new Impl(_))
  }

  private final class Impl[F[_]](socket: Socket[F]) extends CarbonClient[F] {
    def send(metric: CarbonMetric): F[Unit] = {
      val payload = Chunk.array(
        s"${metric.path} ${metric.value} ${metric.timestamp}".getBytes(StandardCharsets.UTF_8)
      )
      socket.write(payload)
    }
  }
}
