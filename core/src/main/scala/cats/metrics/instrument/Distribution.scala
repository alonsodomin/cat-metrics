package cats.metrics.instrument

import cats.Functor

final case class Distribution[A](totalCount: A)
object Distribution {
  implicit val distributionFunctor: Functor[Distribution] = new Functor[Distribution] {
    def map[A, B](fa: Distribution[A])(f: A => B): Distribution[B] =
      Distribution(f(fa.totalCount))
  }
}
