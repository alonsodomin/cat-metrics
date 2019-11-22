package cats.metrics.graphite

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

import cats.effect.{Blocker, Concurrent, ContextShift, Resource}
import fs2.Chunk
import fs2.io.tcp._

trait CarbonClient[F[_]] {
  def send(metric: CarbonMetric): F[Unit]
}
object CarbonClient {
  def apply[F[_]: Concurrent: ContextShift](
      address: InetSocketAddress,
      blocker: Blocker
  ): Resource[F, CarbonClient[F]] = {
    val socket = SocketGroup[F](blocker).flatMap(_.client[F](address))
    socket.map(new Impl(_))
  }

  private final class Impl[F[_]](socket: Socket[F]) extends CarbonClient[F] {
    def send(metric: CarbonMetric): F[Unit] = {
      val payload = Chunk.bytes(
        s"${metric.path} ${metric.value} ${metric.timestamp}".getBytes(StandardCharsets.UTF_8)
      )
      socket.write(payload)
    }
  }
}
