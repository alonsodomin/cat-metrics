package cats.metrics.store

import cats.Functor

case class Metric[V](name: String, value: V)
object Metric {

  implicit val metricFunctor: Functor[Metric] = new Functor[Metric] {
    override def map[A, B](fa: Metric[A])(f: A => B): Metric[B] =
      fa.copy(value = f(fa.value))
  }

}
