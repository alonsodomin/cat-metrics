package cats
package metrics

trait Instrument[F[_], A] {
  def peek(): F[A]
  def reset(): F[A]
}