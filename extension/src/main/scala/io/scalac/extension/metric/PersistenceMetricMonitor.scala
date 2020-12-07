package io.scalac.extension.metric
import io.opentelemetry.api.common.{ Labels => OpenTelemetryLabels }
import io.scalac.extension.metric.PersistenceMetricMonitor.Labels
import io.scalac.extension.model._

object PersistenceMetricMonitor {

  final case class Labels(node: Node, path: Path) {
    def toOpenTelemetry: OpenTelemetryLabels = OpenTelemetryLabels.of("node", node, "path", path)
  }
}

trait PersistenceMetricMonitor extends Bindable[Labels] { self =>

  override type Bound <: BoundMonitor

  def transactionally[A, B, C <: self.type](
    one: TrackingMetricRecorder.Aux[A, C],
    two: TrackingMetricRecorder.Aux[B, C]
  ): Option[(A, B) => Unit]

  trait BoundMonitor {
    def recoveryTime: TrackingMetricRecorder.Aux[Long, self.type]
  }
}
