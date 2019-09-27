package cats.metrics.store

import cats.effect.concurrent.Ref
import cats.effect.{Sync, Timer}
import cats.implicits._
import cats.metrics.instrument.{Counter, Gauge, Instrument}

import monocle.Lens
import monocle.macros.Lenses

trait Store[F[_]] {

  def counter(name: String, initial: Long = 0L): F[Counter[F]]

  def gauge(name: String, initial: Double = 0.0): F[Gauge[F]]

  def snapshot: F[Snapshot]

}

object Store {
  @Lenses
  private case class Instruments[F[_]](
                                  counters: Map[String, Counter[F]],
                                  gauges: Map[String, Gauge[F]]
                                  )
  private object Instruments {
    def empty[F[_]]: Instruments[F] = Instruments(Map.empty, Map.empty)
  }

  def apply[F[_]: Sync: Timer]: F[Store[F]] = {
    Ref[F].of(Instruments.empty[F]).map(new Impl[F](_))
  }

  private class Impl[F[_]: Timer](
      instrumentsRef: Ref[F, Instruments[F]]
  )(implicit F: Sync[F]) extends Store[F] {

    def counter(name: String, initial: Long): F[Counter[F]] =
      instrument[Counter[F]](name, Instruments.counters, Counter(_, initial))

    def gauge(name: String, initial: Double): F[Gauge[F]] =
      instrument[Gauge[F]](name, Instruments.gauges, Gauge(_, initial))

    def snapshot: F[Snapshot] = {
      def snapshotThem[A](insts: List[(String, Instrument.Aux[F, A])]): F[List[Metric[A]]] =
        insts.traverse { case (name, inst) => inst.get.map(Metric(name, _)) }

      instrumentsRef.get.flatMap { instruments =>
        val counters = snapshotThem(instruments.counters.toList)
        val gauges = snapshotThem(instruments.gauges.toList)
        (counters, gauges).mapN(Snapshot.apply)
      }
    }

    private def instrument[Inst <: Instrument[F]](
        name: String,
        indexLens: Lens[Instruments[F], Map[String, Inst]],
        createInstrument: String => F[Inst]
    ): F[Inst] = {
      def doCreate: F[Inst] = for {
        instrument <- createInstrument(name)
        _ <- instrumentsRef.update(indexLens.modify(_ + (name -> instrument)))
      } yield instrument

      for {
        instruments <- instrumentsRef.get
        opt <- F.pure(indexLens.get(instruments).get(name))
        inst <- opt.map(F.pure).getOrElse(doCreate)
      } yield inst
    }


  }

//  case class Snapshot(counters: Map[String, Instrument.Snapshot[Long]])
//
//  type Store[F[_], Inst] = StateT[F, Map[String, Inst], Inst]
//
//  def counter[F[_]: Sync: Timer](name: String, initial: Long = 0L): Store[F, Counter[F]] =
//    instrument[F, Counter[F]](name)(Counter.startAt(initial))
//
//  def gauge[F[_]: Sync: Timer, A](name: String)(implicit A: Monoid[A]): Store[F, Gauge[F]] = gauge[F, A](name, A.empty)
//  def gauge[F[_]: Sync: Timer, A](name: String, initial: A): Store[F, Gauge[F]] =
//    instrument[F, Gauge[F]](name)(Gauge.of(initial))
//
//  def histogram[F[_]: Sync: Timer](name: String, dynamicRange: DynamicRange): Store[F, Histogram[F]] =
//    instrument[F, Histogram[F]](name)(Histogram.in[F](dynamicRange))
//
//  def chronometer[F[_]: Sync: Timer](name: String, precision: TimeUnit, dynamicRange: DynamicRange): Store[F, Chronometer[F]] =
//    instrument[F, Chronometer[F]](name)(Chronometer.of(precision, dynamicRange))
//
//  private def instrument[F[_]: Timer, Inst <: Instrument[F]](name: String)(createInstrument: F[Inst])(implicit F: Sync[F]): Store[F, Inst] =
//    StateT { instruments =>
//      def doCreate = for {
//        inst <- createInstrument
//        newInstrumentMap <- F.pure(instruments + (name -> inst))
//      } yield (newInstrumentMap, inst)
//
//      F.pure(instruments.get(name)).flatMap { instOpt =>
//        instOpt.fold(doCreate)(i => F.pure(instruments -> i))
//      }
//    }

}