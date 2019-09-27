package cats.metrics.instrument

import java.util.concurrent.TimeUnit

// Credit to https://github.com/kamon-io/Kamon/blob/master/kamon-core/src/main/scala/kamon/metric/DynamicRange.scala

/**
 * Describes the dynamic range in which a histogram can operate. Kamon uses the HdrHistogram under the hood to power
 * all histogram-based metrics and such implementation requires to setup a dynamic range in advance so that the
 * necessary data structures can be created before hand, a DynamicRange instance provides the necessary settings to
 * create those data structures. There are three defining characteristics in a Dynamic Range:
 *
 * The lowest discernible value limits the smallest value that can be discerned within the range. E.g. if you are
 * tracking latency for a range between 0 and 3.6e+12 (one hour in nanoseconds), it might be the case that you will
 * never experience measurements bellow 1 microsecond, and thus, all buckets created to cover the range between 0 and
 * 1000 nanoseconds are never used. Setting a lowest discernible value of 1000 will prevent the allocation of buckets
 * for the range between 0 and 1000, saving a bit of space.
 *
 * The highest trackable value sets the upper limit in the range covered by a Histogram. Any value bigger than this
 * might not be recorded in a Histogram.
 *
 * And finally, the number of significant value digits which define the precision with which the range will be covered.
 * One significant value digit gives 10% error margin, two significant value digits give 1% error margin which is the
 * default in Kamon and three significant value digits give 0.1% error margin. Even though it is possible to have even
 * smaller error margin, it proves impractical to try to do so given the memory requirements of such configuration.
 */
case class DynamicRange(lowestDiscernibleValue: Long, highestTrackableValue: Long, significantValueDigits: Int) {

  /**
   * Returns a new DynamicRange with the provided highest trackable value.
   */
  def upTo(highestTrackableValue: Long): DynamicRange =
    copy(highestTrackableValue = highestTrackableValue)

  /**
   * Returns a new DynamicRange with the provided lowest discernible value.
   */
  def withLowestDiscernibleValue(lowestDiscernibleValue: Long): DynamicRange =
    copy(lowestDiscernibleValue = lowestDiscernibleValue)
}

object DynamicRange {

  private val OneHourInNanoseconds = TimeUnit.HOURS.toNanos(1)

  /**
   * Provides a range from 0 to 3.6e+12 (one hour in nanoseconds) with a value precision of 1 significant digit (10%)
   * across that range.
   */
  val Loose = DynamicRange(1L, OneHourInNanoseconds, 1)

  /**
   * Provides a range from 0 to 3.6e+12 (one hour in nanoseconds) with a value precision of 2 significant digits (1%)
   * across that range.
   */
  val Default = DynamicRange(1L, OneHourInNanoseconds, 2)

  /**
   * Provides a range from 0 to 3.6e+12 (one hour in nanoseconds) with a value precision of 3 significant digits (0.1%)
   * across that range.
   */
  val Fine = DynamicRange(1L, OneHourInNanoseconds, 3)

  /**
   * Provides a range from 1 to 100. Useful when creating histograms to sample values that can be expressed as
   * percentages like CPU usage, disk usage and so on.-
   */
  val Percentage = DynamicRange(1L, 100L, 2)
}
