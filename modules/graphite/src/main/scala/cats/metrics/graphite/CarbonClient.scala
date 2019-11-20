package cats.metrics.graphite

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

import cats.effect.{Blocker, ContextShift, Resource, Sync}
import fs2.Chunk
import fs2.io.tcp._

trait CarbonClient[F[_]] {
  def send(metric: CarbonMetric): F[Unit]
}
object CarbonClient {
  def apply[F[_]](
      address: InetSocketAddress,
      blocker: Blocker
  )(implicit F: Sync[F], cs: ContextShift[F]): Resource[F, CarbonClient[F]] = {
    val socket = SocketGroup[F](blocker).flatMap(_.client(address))
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
