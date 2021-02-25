package io.scalac.agent.akka.actor

import scala.concurrent.duration._

import net.bytebuddy.asm.Advice._

import io.scalac.core.util.Timestamp
import io.scalac.extension.actor.MailboxTimeHolder

class MailboxDequeueInstrumentation
object MailboxDequeueInstrumentation {

  @OnMethodExit
  def onExit(@Return envelope: Object, @This mailbox: Object): Unit =
    Option(envelope).map(computeTime).foreach(add(mailbox))

  @inline def computeTime(envelope: Object): FiniteDuration =
    new FiniteDuration(Timestamp.create().interval(EnvelopeOps.getTimestamp(envelope)), MILLISECONDS)

  @inline def add(mailbox: Object)(time: FiniteDuration): Unit =
    MailboxTimeHolder.setTime(MailboxOps.getActor(mailbox), time)

}