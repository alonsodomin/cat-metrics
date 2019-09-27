package cats.metrics.store

case class Snapshot(
    counters: List[Metric[Long]],
    gauges: List[Metric[Double]]
) {
  def isEmpty: Boolean = counters.isEmpty && gauges.isEmpty
}
object Snapshot {
  val Empty = Snapshot(List.empty, List.empty)
}
