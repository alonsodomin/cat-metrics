package cats.metrics.instrument

import cats.{Functor, Show}
import cats.derived.semi
import cats.implicits._

final case class Distribution[A](count: A, min: A, max: A)
object Distribution {
  private[instrument] def fromHistogram(hist: org.HdrHistogram.Histogram): Distribution[Long] =
    Distribution(hist.getTotalCount, hist.getMinValue, hist.getMaxValue)

  implicit val distributionFunctor: Functor[Distribution] =
    semi.functor

  implicit def distributionShow[A: Show]: Show[Distribution[A]] = Show.show { dist =>
    show"count: ${dist.count}, min: ${dist.min}, max: ${dist.max}"
  }

}
