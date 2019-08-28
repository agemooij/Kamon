package kamon.graphite

import java.time.Instant

import kamon.metric.PeriodSnapshot
import kamon.tag.TagSet
import kamon.testkit.MetricSnapshotBuilder
import kamon.util.Filter
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class GraphiteSenderSpec extends WordSpec with BeforeAndAfterAll with Matchers {
  "the GraphiteSender" should {
    val from = Instant.ofEpochSecond(1517000974)
    val to = Instant.ofEpochSecond(1517000993)

    val senderConfig = GraphiteSenderConfig("hostX", 123, "kamon-graphiteprefix", legacySupport = false, TagSet.of("tag1", "111"), Filter.Accept, Seq(.5, .9, .999))

    "send counter metrics" in {
      //arrange
      val periodSnapshot = PeriodSnapshot(
        from, to,
        counters = List(MetricSnapshotBuilder.counter("custom.user.counter", TagSet.Empty, 42)),
        gauges = List.empty,
        histograms = List.empty,
        timers = List.empty,
        rangeSamplers = List.empty
      )
      val testee = new TestGraphiteSender(senderConfig)

      //act
      testee.reportPeriodSnapshot(periodSnapshot)

      //assert
      testee.messages shouldBe List("kamon-graphiteprefix.custom_user_counter.count;tag1=111 42 1517000993\n")
    }

    "send gauge metrics" in {
      //arrange
      val periodSnapshot = PeriodSnapshot(
        from, to,
        counters = List.empty,
        gauges = List(MetricSnapshotBuilder.gauge("jvm.heap-size", TagSet.Empty, 150000000)),
        histograms = List.empty,
        timers = List.empty,
        rangeSamplers = List.empty
      )
      val testee = new TestGraphiteSender(senderConfig)

      //act
      testee.reportPeriodSnapshot(periodSnapshot)

      //assert
      testee.messages shouldBe List("kamon-graphiteprefix.jvm_heap-size.value;tag1=111 1.5E8 1517000993\n")
    }

    "send histograms" in {
      //arrange
      val periodSnapshot = PeriodSnapshot(
        from, to,
        counters = List.empty,
        gauges = List.empty,
        histograms = List(MetricSnapshotBuilder.histogram("my.histogram", TagSet.Empty)(1, 2, 4, 6)),
        timers = List.empty,
        rangeSamplers = List.empty
      )
      val testee = new TestGraphiteSender(senderConfig)

      //act
      testee.reportPeriodSnapshot(periodSnapshot)

      //assert
      testee.messages shouldBe List(
        "kamon-graphiteprefix.my_histogram.count;tag1=111 4 1517000993\n",
        "kamon-graphiteprefix.my_histogram.min;tag1=111 1 1517000993\n",
        "kamon-graphiteprefix.my_histogram.max;tag1=111 6 1517000993\n",
        "kamon-graphiteprefix.my_histogram.p0.5;tag1=111 1 1517000993\n",
        "kamon-graphiteprefix.my_histogram.p0.9;tag1=111 1 1517000993\n",
        "kamon-graphiteprefix.my_histogram.p0.999;tag1=111 1 1517000993\n",
        "kamon-graphiteprefix.my_histogram.average;tag1=111 3 1517000993\n",
        "kamon-graphiteprefix.my_histogram.sum;tag1=111 13 1517000993\n")
    }

    "send timers" in {
      //arrange
      val periodSnapshot = PeriodSnapshot(
        from, to,
        counters = List.empty,
        gauges = List.empty,
        histograms = List.empty,
        timers = List(MetricSnapshotBuilder.histogram("my.timer", TagSet.Empty)(1, 2, 4, 6)),
        rangeSamplers = List.empty
      )
      val testee = new TestGraphiteSender(senderConfig)

      //act
      testee.reportPeriodSnapshot(periodSnapshot)

      //assert
      testee.messages shouldBe List(
        "kamon-graphiteprefix.my_timer.count;tag1=111 4 1517000993\n",
        "kamon-graphiteprefix.my_timer.min;tag1=111 1 1517000993\n",
        "kamon-graphiteprefix.my_timer.max;tag1=111 6 1517000993\n",
        "kamon-graphiteprefix.my_timer.p0.5;tag1=111 1 1517000993\n",
        "kamon-graphiteprefix.my_timer.p0.9;tag1=111 1 1517000993\n",
        "kamon-graphiteprefix.my_timer.p0.999;tag1=111 1 1517000993\n",
        "kamon-graphiteprefix.my_timer.average;tag1=111 3 1517000993\n",
        "kamon-graphiteprefix.my_timer.sum;tag1=111 13 1517000993\n")
    }

    "send ranges" in {
      //arrange
      val periodSnapshot = PeriodSnapshot(
        from, to,
        counters = List.empty,
        gauges = List.empty,
        histograms = List.empty,
        timers = List.empty,
        rangeSamplers = List(MetricSnapshotBuilder.histogram("my.range", TagSet.Empty)(1, 2, 4, 6))
      )
      val testee = new TestGraphiteSender(senderConfig)

      //act
      testee.reportPeriodSnapshot(periodSnapshot)

      //assert
      testee.messages shouldBe List(
        "kamon-graphiteprefix.my_range.count;tag1=111 4 1517000993\n",
        "kamon-graphiteprefix.my_range.min;tag1=111 1 1517000993\n",
        "kamon-graphiteprefix.my_range.max;tag1=111 6 1517000993\n",
        "kamon-graphiteprefix.my_range.p0.5;tag1=111 1 1517000993\n",
        "kamon-graphiteprefix.my_range.p0.9;tag1=111 1 1517000993\n",
        "kamon-graphiteprefix.my_range.p0.999;tag1=111 1 1517000993\n",
        "kamon-graphiteprefix.my_range.average;tag1=111 3 1517000993\n",
        "kamon-graphiteprefix.my_range.sum;tag1=111 13 1517000993\n")
    }

    "sanitize problematic tags" in {
      //arrange
      val periodSnapshot = PeriodSnapshot(
        from, to,
        counters = List(MetricSnapshotBuilder.counter("akka.actor.errors", TagSet.of("path", "as/user/actor").withTag("tag3", "333;3").withTag("tag2.123", 3L), 10)),
        gauges = List.empty,
        histograms = List.empty,
        timers = List.empty,
        rangeSamplers = List.empty
      )
      val testee = new TestGraphiteSender(senderConfig)

      //act
      testee.reportPeriodSnapshot(periodSnapshot)

      //assert
      testee.messages(0) should be("kamon-graphiteprefix.akka_actor_errors.count;tag2.123=3;tag1=111;tag3=333_3;path=as/user/actor 10 1517000993\n")
    }
  }

}

private[graphite] class TestGraphiteSender(senderConfig: GraphiteSenderConfig) extends GraphiteSender(senderConfig) {
  var messages: Seq[String] = List.empty
  override def write(data: Array[Byte]): Unit = messages = messages :+ new String(data, GraphiteSender.GraphiteEncoding)
  override def flush(): Unit = {}
  override def close(): Unit = {}
}