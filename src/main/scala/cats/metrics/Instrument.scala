package cats
package metrics

import fs2.Stream

import scala.concurrent.duration.FiniteDuration

trait Instrument[F[_]] {
  type Value

  def subscribe(frequency: FiniteDuration): Stream[F, Value]
}