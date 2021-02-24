package io.scalac.extension

import akka.actor.ActorRef
import akka.actor.typed.receptionist.Receptionist.Register
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.{ AbstractBehavior, ActorContext, Behaviors, TimerScheduler }
import akka.actor.typed.{ Behavior, Terminated }
import io.scalac.core.akka.model.PushMetrics
import io.scalac.core.model.ConnectionStats
import io.scalac.extension.AkkaStreamMonitoring.{ CollectionTimeout, Command, StartStreamCollection, StatsReceived }
import io.scalac.extension.event.ActorInterpreterStats
import io.scalac.extension.metric.StreamMetricMonitor.{ Labels => GlobalLabels }
import io.scalac.extension.metric.StreamOperatorMetricsMonitor.Labels
import io.scalac.extension.metric.{ StreamMetricMonitor, StreamOperatorMetricsMonitor }
import io.scalac.extension.model.Direction._
import io.scalac.extension.model._

import scala.concurrent.duration.{ FiniteDuration, _ }

object AkkaStreamMonitoring {

  sealed trait Command

  private case class StatsReceived(actorInterpreterStats: ActorInterpreterStats) extends Command

  case class StartStreamCollection(refs: Set[ActorRef]) extends Command

  private[AkkaStreamMonitoring] case object CollectionTimeout extends Command

  def apply(
    streamOperatorMonitor: StreamOperatorMetricsMonitor,
    streamMonitor: StreamMetricMonitor,
    node: Option[Node]
  ): Behavior[Command] =
    Behaviors.setup(ctx =>
      Behaviors.withTimers(scheduler =>
        new AkkaStreamMonitoring(ctx, streamOperatorMonitor, streamMonitor, scheduler, node)
      )
    )

}

class AkkaStreamMonitoring(
  ctx: ActorContext[Command],
  streamOperatorMonitor: StreamOperatorMetricsMonitor,
  streamMonitor: StreamMetricMonitor,
  scheduler: TimerScheduler[Command],
  node: Option[Node],
  timeout: FiniteDuration = 2.seconds
) extends AbstractBehavior[Command](ctx) {

  import ctx._

  private[this] var connectionStats: Option[Set[ConnectionStats]] = None
  private[this] val globalStreamMonitor                           = streamMonitor.bind(GlobalLabels(node))

  system.receptionist ! Register(streamServiceKey, messageAdapter(StatsReceived.apply))

  override def onMessage(msg: Command): Behavior[Command] = msg match {
    case StartStreamCollection(refs) if refs.nonEmpty =>
      log.info("Start stream stats collection")
      scheduler.startSingleTimer(CollectionTimeout, CollectionTimeout, timeout)

      refs.foreach { ref =>
        watch(ref)
        ref ! PushMetrics
      }
      collecting(refs)
    case StartStreamCollection(_) =>
      log.error(s"StartStreamCollection with empty refs")
      this
    case StatsReceived(_) =>
      log.warn("Received stream running statistics after timeout")
      this
  }

  def updateRunningStreams(refs: Set[ActorRef]): Unit = {

    globalStreamMonitor.streamActors.setValue(refs.size)
    val streams = refs.map { ref =>
      val Array(name, id, _ @_*) = ref.path.name.split('-')
      s"${name}-${id}"
    }.size
    globalStreamMonitor.runningStreams.setValue(streams)
  }

  // TODO optimize this!
  def recordAll(): Unit =
    connectionStats.foreach { stats =>
      stats.groupBy(_.inName).foreach {
        case (inName, stats) =>
          stats
            .groupBy(_.outName)
            .view
            .mapValues(_.foldLeft(0L)(_ + _.push))
            .foreach {
              case (outName, count) =>
                val labels = Labels(None, None, inName.name, In, outName.name)

                streamOperatorMonitor.bind(labels).processedMessages.incValue(count)
            }
      }

      stats.groupBy(_.outName).foreach {
        case (outName, stats) =>
          stats
            .groupBy(_.inName)
            .view
            .mapValues(_.foldLeft(0L)(_ + _.pull))
            .foreach {
              case (inName, count) =>
                val labels = Labels(None, None, outName.name, Out, inName.name)

                streamOperatorMonitor.bind(labels).processedMessages.incValue(count)
            }
      }
    }

  def collecting(refs: Set[ActorRef]): Behavior[Command] =
    Behaviors
      .receiveMessage[Command] {
        case StatsReceived(ActorInterpreterStats(ref, info, connections, _)) =>
          val refsLeft = refs - ref
          log.trace("Received stats from {}", ref)

          unwatch(ref)

          connectionStats.fold[Unit] {
            connectionStats = Some(connections)
          }(prev => connectionStats = Some(prev ++ connections))

          if (refsLeft.isEmpty) {
            log.debug("Finished collecting stats")
            scheduler.cancel(CollectionTimeout)
            recordAll()
            this
          } else {
            collecting(refsLeft)
          }

        case CollectionTimeout =>
          log.warn("Collecting stats from running streams timeout")
          recordAll() // we record data gathered so far nevertheless
          this
        // TODO handle this case better
        case StartStreamCollection(_) =>
          log.debug("Another collection started but previous didn't finish")
          Behaviors.same

      }
      .receiveSignal {
        case (_, Terminated(ref)) =>
          log.debug("Stream ref {} terminated", ref)
          collecting(refs - ref.toClassic)

      }
}
