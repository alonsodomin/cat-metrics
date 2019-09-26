package cats.metrics

trait Chronometer[F[_]] {

  def measure[A](fa: F[A]): F[A]

}
