package cats.metrics

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._

trait InstrumentStore[F[_]] {
  def counter(name: String, initial: Long): F[Counter[F]]
}
object InstrumentStore {
  def apply[F[_]: Sync]: F[InstrumentStore[F]] =
    Ref[F].of(Map.empty[String, Counter[F]]).map(new Impl[F](_))

  private class Impl[F[_]](countersRef: Ref[F, Map[String, Counter[F]]])(implicit F: Sync[F]) extends InstrumentStore[F] {
    def counter(name: String, initial: Long): F[Counter[F]] = {
      def createCounter = for {
        counter <- Counter.startAt[F](initial)
        _ <- countersRef.update(_ + (name -> counter))
      } yield counter

      for {
        counters <- countersRef.get
        counter <- counters.get(name).fold(createCounter)(F.pure)
      } yield counter
    }
  }

}