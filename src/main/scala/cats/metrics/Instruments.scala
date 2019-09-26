package cats.metrics

import cats.Monoid
import cats.data.StateT
import cats.effect.{Sync, Timer}
import cats.implicits._

import scala.concurrent.duration.TimeUnit

object Instruments {
  type Store[F[_], Inst] = StateT[F, Map[String, Inst], Inst]

  def counter[F[_]: Sync: Timer](name: String, initial: Long = 0L): Store[F, Counter[F]] =
    instrument[F, Counter[F]](name)(Counter.startAt(initial))

  def gauge[F[_]: Sync: Timer, A](name: String)(implicit A: Monoid[A]): Store[F, Gauge[F]] = gauge[F, A](name, A.empty)
  def gauge[F[_]: Sync: Timer, A](name: String, initial: A): Store[F, Gauge[F]] =
    instrument[F, Gauge[F]](name)(Gauge.of(initial))

  def histogram[F[_]: Sync: Timer](name: String, dynamicRange: DynamicRange): Store[F, Histogram[F]] =
    instrument[F, Histogram[F]](name)(Histogram.in[F](dynamicRange))

  def chronometer[F[_]: Sync: Timer](name: String, precision: TimeUnit, dynamicRange: DynamicRange): Store[F, Chronometer[F]] =
    instrument[F, Chronometer[F]](name)(Chronometer.of(precision, dynamicRange))

  private def instrument[F[_]: Timer, Inst <: Instrument[F]](name: String)(createInstrument: F[Inst])(implicit F: Sync[F]): Store[F, Inst] =
    StateT { instruments =>
      def doCreate = for {
        inst <- createInstrument
        newInstrumentMap <- F.pure(instruments + (name -> inst))
      } yield (newInstrumentMap, inst)

      F.pure(instruments.get(name)).flatMap { instOpt =>
        instOpt.fold(doCreate)(i => F.pure(instruments -> i))
      }
    }

}