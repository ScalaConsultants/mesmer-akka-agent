package io.scalac.extension.upstream.opentelemetry

import io.opentelemetry.api.common.Labels
import io.opentelemetry.api.metrics.{ LongCounter, LongUpDownCounter, LongValueRecorder, SynchronousInstrument }
import io.scalac.extension.metric._

trait SynchronousInstrumentFactory {
  private[upstream] def metricRecorder(
    underlying: LongValueRecorder,
    labels: Labels
  ): UnregisteredInstrument[WrappedLongValueRecorder] = {
    val instrument = WrappedLongValueRecorder(underlying, labels)
    root => {
      root.registerUnbind(instrument)
      instrument
    }
  }

  private[upstream] def counter(
    underlying: LongCounter,
    labels: Labels
  ): UnregisteredInstrument[WrappedCounter] = {
    val instrument = WrappedCounter(underlying, labels)
    root => {
      root.registerUnbind(instrument)
      instrument
    }
  }

  private[upstream] def upDownCounter(
    underlying: LongUpDownCounter,
    labels: Labels
  ): UnregisteredInstrument[WrappedUpDownCounter] = {
    val instrument = WrappedUpDownCounter(underlying, labels)
    root => {
      root.registerUnbind(instrument)
      instrument
    }
  }
}

sealed trait WrappedSynchronousInstrument[L] extends Unbind with WrappedInstrument {

  private[extension] def underlying: SynchronousInstrument[_]
  private[extension] def labels: Labels
}

final case class WrappedLongValueRecorder private[opentelemetry] (underlying: LongValueRecorder, labels: Labels)
    extends WrappedSynchronousInstrument[Long]
    with MetricRecorder[Long] {
  override type Self = WrappedLongValueRecorder
  private[this] lazy val bound = underlying.bind(labels)

  override def setValue(value: Long): Unit = bound.record(value)

  def unbind(): Unit = bound.unbind()
}

final case class WrappedUpDownCounter private[opentelemetry] (underlying: LongUpDownCounter, labels: Labels)
    extends WrappedSynchronousInstrument[Long]
    with UpDownCounter[Long] {
  override type Self = WrappedUpDownCounter

  private[this] lazy val bound = underlying.bind(labels)

  override def decValue(value: Long): Unit = bound.add(-value)

  override def incValue(value: Long): Unit = bound.add(value)

  def unbind(): Unit = bound.unbind()
}

final case class WrappedCounter private[opentelemetry] (underlying: LongCounter, labels: Labels)
    extends WrappedSynchronousInstrument[Long]
    with Counter[Long] {
  override type Self = WrappedCounter

  private[this] lazy val bound = underlying.bind(labels)

  override def incValue(value: Long): Unit = bound.add(value)

  def unbind(): Unit = bound.unbind()
}
