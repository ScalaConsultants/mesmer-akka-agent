package io.scalac.extension.upstream.opentelemetry
import io.opentelemetry.api.common.Labels
import io.opentelemetry.api.metrics.{ BatchRecorder, Meter }
import io.scalac.extension.metric.{ Synchronized => BaseSynchronized }

import scala.collection.mutable.ListBuffer

abstract class Synchronized(private val meter: Meter) extends BaseSynchronized {
  import Synchronized._
  override type Instrument[X] = WrappedSynchronousInstrument[X]

  override def atomically[A, B](first: Instrument[A], second: Instrument[B]): (A, B) => Unit = { (a, b) =>
    meter
      .newBatchRecorder(extractLabels(first.labels): _*)
      .putValue(first, a)
      .putValue(second, b)
      .record()
  }

  private def extractLabels(labels: Labels): List[String] = {
    val buffer: ListBuffer[String] = ListBuffer.empty
    labels.forEach { case (key, value) =>
      buffer ++= List(key, value)
    }
    buffer.toList
  }
}

object Synchronized {
  private[opentelemetry] implicit class RecorderExt(val recorder: BatchRecorder) extends AnyVal {
    def putValue[L](instrument: WrappedSynchronousInstrument[L], value: L): BatchRecorder = {
      instrument match {
        case WrappedLongValueRecorder(underlying, _) => recorder.put(underlying, value)
        case WrappedCounter(underlying, _)           => recorder.put(underlying, value)
        case WrappedUpDownCounter(underlying, _)     => recorder.put(underlying, value)
      }
      recorder
    }
  }
}
