package io.scalac.agent.akka.actor
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.adapter._
import io.scalac.core.actor.ActorCellDecorator
import net.bytebuddy.asm.Advice

class StashBufferAdvice
object StashBufferAdvice {

  @Advice.OnMethodExit
  def stash(
    @Advice.FieldValue("akka$actor$typed$internal$StashBufferImpl$$ctx") ctx: ActorContext[_]
  ): Unit =
    ActorCellDecorator.get(ctx.toClassic).foreach(_.stashSize.inc())

}