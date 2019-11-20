package cats.metrics
package store

import cats.metrics.instrument.{Chronometer, Counter, Gauge, Histogram}
import monocle.macros.Lenses

@Lenses
private[metrics] case class Instruments[F[_]](
    counters: Map[String, Counter[F]],
    gauges: Map[String, Gauge[F]],
    histograms: Map[String, Histogram[F]],
    chronometers: Map[String, Chronometer[F]]
)
private[metrics] object Instruments {
  def empty[F[_]]: Instruments[F] = Instruments(Map.empty, Map.empty, Map.empty, Map.empty)
}
