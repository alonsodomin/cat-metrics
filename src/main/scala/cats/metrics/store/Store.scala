package cats.metrics.store

import cats.effect.concurrent.Ref
import cats.effect.{Sync, Timer}
import cats.implicits._
import cats.metrics.instrument.{Chronometer, Counter, DynamicRange, Gauge, Histogram, Instrument}

import monocle.Lens
import monocle.macros.Lenses

import scala.concurrent.duration.TimeUnit

trait Store[F[_]] {

  def counter(name: String, initial: Long = 0L): F[Counter[F]]

  def gauge(name: String, initial: Double = 0.0): F[Gauge[F]]

  def histogram(name: String, dynamicRange: DynamicRange = DynamicRange.Default): F[Histogram[F]]

  def chronometer(name: String, precision: TimeUnit, dynamicRange: DynamicRange = DynamicRange.Default): F[Chronometer[F]]

  def snapshot: F[Snapshot]

}

object Store {
  @Lenses
  private case class Instruments[F[_]](
      counters: Map[String, Counter[F]],
      gauges: Map[String, Gauge[F]],
      histograms: Map[String, Histogram[F]],
      chronometers: Map[String, Chronometer[F]]
  )
  private object Instruments {
    def empty[F[_]]: Instruments[F] = Instruments(Map.empty, Map.empty, Map.empty, Map.empty)
  }

  def apply[F[_]: Sync: Timer]: F[Store[F]] =
    Ref[F].of(Instruments.empty[F]).map(new Impl[F](_))

  private class Impl[F[_]: Timer](
      instrumentsRef: Ref[F, Instruments[F]]
  )(implicit F: Sync[F])
      extends Store[F] {

    def counter(name: String, initial: Long): F[Counter[F]] =
      instrument[Counter[F]](name, Instruments.counters, Counter(_, initial))

    def gauge(name: String, initial: Double): F[Gauge[F]] =
      instrument[Gauge[F]](name, Instruments.gauges, Gauge(_, initial))

    def histogram(name: String, dynamicRange: DynamicRange): F[Histogram[F]] =
      instrument[Histogram[F]](name, Instruments.histograms, Histogram(_, dynamicRange))

    def chronometer(name: String, precision: TimeUnit, dynamicRange: DynamicRange): F[Chronometer[F]] = ???

    def snapshot: F[Snapshot] = {
      def snapshotThem[A](insts: List[(String, Instrument.Aux[F, A])]): F[List[Metric[A]]] =
        insts.traverse { case (name, inst) => inst.get.map(Metric(name, _)) }

      instrumentsRef.get.flatMap { instruments =>
        val counters = snapshotThem(instruments.counters.toList)
        val gauges   = snapshotThem(instruments.gauges.toList)
        val histograms = snapshotThem(instruments.histograms.toList)
        val chronometers = snapshotThem(instruments.chronometers.toList)
        (counters, gauges, histograms, chronometers).mapN(Snapshot.apply)
      }
    }

    private def instrument[Inst <: Instrument[F]](
        name: String,
        indexLens: Lens[Instruments[F], Map[String, Inst]],
        createInstrument: String => F[Inst]
    ): F[Inst] = {
      def doCreate: F[Inst] =
        for {
          instrument <- createInstrument(name)
          _          <- instrumentsRef.update(indexLens.modify(_ + (name -> instrument)))
        } yield instrument

      for {
        instruments <- instrumentsRef.get
        opt         <- F.pure(indexLens.get(instruments).get(name))
        inst        <- opt.map(F.pure).getOrElse(doCreate)
      } yield inst
    }

  }

}
