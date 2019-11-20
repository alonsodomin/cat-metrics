package cats.metrics.graphite

import java.time.Instant

case class CarbonMetric(path: String, value: String, timestamp: Instant)
