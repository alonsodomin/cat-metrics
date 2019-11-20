package cats.metrics

import cats.metrics.instrument.Distribution
import cats.metrics.store.Metric

import scala.concurrent.duration.FiniteDuration

final case class Snapshot(
    counters: List[Metric[Long]],
    gauges: List[Metric[Double]],
    histograms: List[Metric[Distribution[Long]]],
    chronometers: List[Metric[Distribution[FiniteDuration]]]
) {

  def isEmpty: Boolean =
    counters.isEmpty && gauges.isEmpty && histograms.isEmpty && chronometers.isEmpty

}
object Snapshot {
  val Empty = Snapshot(List.empty, List.empty, List.empty, List.empty)
}
