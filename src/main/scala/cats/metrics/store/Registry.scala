package cats.metrics
package store

import cats.effect.concurrent.Ref
import cats.effect._
import cats.implicits._

import cats.metrics.instrument.{Chronometer, Counter, DynamicRange, Gauge, Histogram, Instrument}

import monocle.Lens

import scala.concurrent.duration.TimeUnit

trait Registry[F[_]] {

  def counter(name: String, initial: Long = 0L): F[Counter[F]]

  def gauge(name: String, initial: Double = 0.0): F[Gauge[F]]

  def histogram(name: String, dynamicRange: DynamicRange = DynamicRange.Default): F[Histogram[F]]

  def chronometer(
      name: String,
      precision: TimeUnit,
      dynamicRange: DynamicRange = DynamicRange.Default
  ): F[Chronometer[F]]

  private[metrics] def instruments: F[Instruments[F]]

}

object Registry {

  def apply[F[_]: Sync: Timer]: F[Registry[F]] =
    Ref[F].of(Instruments.empty[F]).map(new Impl[F](_))

  private class Impl[F[_]: Timer](
      instrumentsRef: Ref[F, Instruments[F]]
  )(implicit F: Sync[F])
      extends Registry[F] {

    def instruments: F[Instruments[F]] = instrumentsRef.get

    def counter(name: String, initial: Long): F[Counter[F]] =
      instrument[Counter[F]](name, Instruments.counters, Counter(_, initial))

    def gauge(name: String, initial: Double): F[Gauge[F]] =
      instrument[Gauge[F]](name, Instruments.gauges, Gauge(_, initial))

    def histogram(name: String, dynamicRange: DynamicRange): F[Histogram[F]] =
      instrument[Histogram[F]](name, Instruments.histograms, Histogram(_, dynamicRange))

    def chronometer(
        name: String,
        precision: TimeUnit,
        dynamicRange: DynamicRange
    ): F[Chronometer[F]] =
      instrument[Chronometer[F]](
        name,
        Instruments.chronometers,
        Chronometer(_, precision, dynamicRange)
      )

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
