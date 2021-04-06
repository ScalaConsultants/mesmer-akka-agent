package io.scalac.extension.service

import akka.actor.typed.receptionist.Receptionist.{ Deregister, Register }
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, Behavior, PreRestart, Signal }
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Keep, Sink, Source, SourceQueueWithComplete }
import akka.{ actor => classic }
import com.typesafe.config.Config
import io.scalac.core.actorServiceKey
import io.scalac.core.event.ActorEvent
import io.scalac.core.model._
import io.scalac.extension.config.Configuration
import io.scalac.extension.metric.ActorSystemMonitor
import io.scalac.extension.metric.ActorSystemMonitor.Labels
import io.scalac.extension.service.DeltaActorTree._

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.jdk.DurationConverters._

case class DeltaActorTreeConfig(
  bulkWithin: FiniteDuration,
  bulkMaxSize: Int,
  eventQueueSize: Int
)

object DeltaActorTreeConfig extends Configuration[DeltaActorTreeConfig] {
  override def default: DeltaActorTreeConfig = DeltaActorTreeConfig(1.second, 1000, 1000)

  override protected val configurationBase: String = "io.scalac.akka-monitoring.actor"

  override protected def extractFromConfig(config: Config): DeltaActorTreeConfig =
    DeltaActorTreeConfig(
      config
        .tryValue("bulk-within")(_.getDuration)
        .map(_.toScala)
        .getOrElse(default.bulkWithin),
      config
        .tryValue("bulk-max-size")(_.getInt)
        .getOrElse(default.bulkMaxSize),
      config
        .tryValue("event-queue-size")(_.getInt)
        .getOrElse(default.eventQueueSize)
    )
}

object DeltaActorTree {

  sealed trait Api

  private final case object ServiceRegistered extends Api

  sealed trait Event extends Api

  private final case class ActorTerminated(actorRef: classic.ActorRef) extends Event with DeltaBuilderCommand
  private final case class ActorCreated(actorRef: classic.ActorRef)    extends Event with DeltaBuilderCommand

  sealed trait Command extends Api

  final case class Connect(ref: ActorRef[Delta]) extends Command

  final case class Delta(created: Seq[classic.ActorRef], terminated: Seq[classic.ActorRef]) {

    lazy val nonEmpty: Boolean = created.nonEmpty || terminated.nonEmpty
  }

  sealed trait DeltaBuilderCommand

  private final case class BulkStateChange(delta: Delta) extends DeltaBuilderCommand

  private[DeltaActorTree] final class DeltaBuilder private () {

    private[this] val created    = ListBuffer.empty[classic.ActorRef]
    private[this] val terminated = ListBuffer.empty[classic.ActorRef]

    def process(command: DeltaBuilderCommand): this.type = {
      command match {
        case BulkStateChange(delta) =>
          created ++= delta.created
          terminated ++= delta.terminated
        case ActorCreated(ref)    => created += ref
        case ActorTerminated(ref) => terminated += ref
      }
      this
    }

    def result: Delta =
      Delta(created.toSeq, terminated.toSeq)

  }

  object DeltaBuilder {
    def apply(): DeltaBuilder = new DeltaBuilder()
  }

  private[service] def apply(
    config: DeltaActorTreeConfig,
    monitor: ActorSystemMonitor,
    node: Option[Node],
    restart: Boolean,
    backoffTraverser: ActorTreeTraverser
  ): Behavior[Command] = Behaviors
    .setup[Api](context => new DeltaActorTree(context, monitor, config, node, backoffTraverser).start(restart))
    .narrow[Command]

  private[service] def apply(
    monitor: ActorSystemMonitor,
    node: Option[Node],
    restart: Boolean = false,
    backoffTraverser: ActorTreeTraverser = ReflectiveActorTreeTraverser
  ): Behavior[Command] =
    Behaviors
      .setup[Api](context =>
        new DeltaActorTree(
          context,
          monitor,
          DeltaActorTreeConfig.fromConfig(context.system.settings.config),
          node,
          backoffTraverser
        ).start(restart)
      )
      .narrow[Command]
}

final class DeltaActorTree private (
  context: ActorContext[Api],
  monitor: ActorSystemMonitor,
  config: DeltaActorTreeConfig,
  node: Option[Node],
  backoffTraverser: ActorTreeTraverser
) {
  import context._

  private[this] val boundMonitor = monitor.bind(Labels(node))

  private val registeredAdapter = context.messageAdapter[ActorEvent] { case ActorEvent.ActorCreated(ref, _) =>
    ActorCreated(ref)
  }

  def start(restart: Boolean = false): Behavior[Api] = {
    system.receptionist ! Register(
      actorServiceKey,
      registeredAdapter,
      context.messageAdapter(_ => ServiceRegistered)
    )
    waitForRegistration(restart)
  }

  private def waitForRegistration(restart: Boolean): Behavior[Api] = Behaviors.withStash(config.eventQueueSize) {
    stash =>
      Behaviors.receiveMessagePartial { case ServiceRegistered =>
        val snapshot = if (restart) backoffTraverser.getActorTreeFromRootGuardian(system.classicSystem) else Nil
        stash.unstashAll(waitingForConnection(snapshot))
      }
  }

  private def connected(connected: SourceQueueWithComplete[DeltaBuilderCommand]): Behavior[Api] =
    Behaviors.receiveMessagePartial[Api] {
      case created @ ActorCreated(ref) =>
        onCreated(ref)
        connected.offer(created)
        Behaviors.same
      case terminated @ ActorTerminated(ref) =>
        onTerminated(ref)
        connected.offer(terminated)
        Behaviors.same
    }

  private def waitingForConnection(snapshot: List[classic.ActorRef]): Behavior[Api] = Behaviors
    .receiveMessagePartial[Api] {
      case ActorCreated(ref) =>
        onCreated(ref)
        waitingForConnection(ref :: snapshot)
      case ActorTerminated(ref) =>
        onTerminated(ref)
        waitingForConnection(snapshot.filter(_ != ref))
      case Connect(ref) =>
        implicit val system = context.system

        log.debug("Connect DeltaActorTree to {}", ref)

        val initial = Delta(created = snapshot, terminated = Seq.empty)
        log.debug("Propagate initial state {}", initial)

        val source =
          subscriberStream(initial)
            .to(Sink.foreach(ref.tell))
            .run()

        connected(source)
    }
    .receiveSignal(deregister)

  private def onCreated(ref: classic.ActorRef): Unit = {
    log.trace("Actor created {}", ref)
    context.watchWith(ref.toTyped, ActorTerminated(ref))
    boundMonitor.createdActors.incValue(1L)
  }

  private def onTerminated(ref: classic.ActorRef): Unit = {
    log.trace("Actor {} terminated", ref)
    boundMonitor.terminatedActors.incValue(1L)
  }

  private def subscriberStream(init: Delta): Source[Delta, SourceQueueWithComplete[DeltaBuilderCommand]] =
    Source
      .single(init)
      .filter(_.nonEmpty)
      .map(BulkStateChange)
      .concatMat(
        Source
          .queue[DeltaBuilderCommand](config.eventQueueSize, OverflowStrategy.fail)
      )(Keep.right)
      .groupedWithin(config.bulkMaxSize, config.bulkWithin)
      .map(_.foldLeft(DeltaBuilder()) { case (builder, event) =>
        builder.process(event)
      }.result)
      .filter(_.nonEmpty)

  private val deregister: PartialFunction[(ActorContext[Api], Signal), Behavior[Api]] = { case (_, PreRestart) =>
    system.receptionist ! Deregister(actorServiceKey, registeredAdapter)
    Behaviors.same
  }
}
