package io.scalac.agent.akka.actor

import net.bytebuddy.asm.Advice._

class ActorCellSendMessageInstrumentation
object ActorCellSendMessageInstrumentation {

  @OnMethodEnter
  def onEnter(@Argument(0) envelope: Object): Unit =
    Option(envelope).foreach(EnvelopeOps.setTimestamp)

}