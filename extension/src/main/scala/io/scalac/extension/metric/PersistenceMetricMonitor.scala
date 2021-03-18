package io.scalac.extension.metric
import io.scalac.core.LabelSerializable
import io.scalac.core.model._

object PersistenceMetricMonitor {

  final case class Labels(node: Option[Node], path: Path, persistenceId: PersistenceId) extends LabelSerializable {
    override val serialize: RawLabels = node.serialize ++ path.serialize ++ persistenceId.serialize
  }

  trait BoundMonitor extends Bound {
    def recoveryTime: MetricRecorder[Long]
    def recoveryTotal: UpCounter[Long]
    def persistentEvent: MetricRecorder[Long]
    def persistentEventTotal: UpCounter[Long]
    def snapshot: UpCounter[Long]
  }

}
