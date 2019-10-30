package cats.metrics.store

import cats.Functor
import cats.derived.semi

final case class Metric[V](name: String, value: V)
object Metric {

  implicit val metricFunctor: Functor[Metric] =
    semi.functor

}
