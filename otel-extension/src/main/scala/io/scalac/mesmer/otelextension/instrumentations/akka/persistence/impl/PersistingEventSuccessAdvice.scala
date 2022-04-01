package io.scalac.mesmer.otelextension.instrumentations.akka.persistence.impl

import akka.actor.typed.scaladsl.ActorContext
import akka.persistence.PersistentRepr
import io.scalac.mesmer.core.event.EventBus
import io.scalac.mesmer.core.event.PersistenceEvent.PersistingEventFinished
import io.scalac.mesmer.core.model.Path
import io.scalac.mesmer.core.util.Timestamp
import net.bytebuddy.asm.Advice.{ Argument, OnMethodEnter }
import io.scalac.mesmer.core.model._

object PersistingEventSuccessAdvice {

  @OnMethodEnter
  def onWriteSuccess(@Argument(0) context: ActorContext[_], @Argument(1) event: PersistentRepr): Unit = {
    val path: Path = context.self.path.toPath

    EventBus(context.system)
      .publishEvent(
        PersistingEventFinished(
          path,
          event.persistenceId,
          event.sequenceNr,
          Timestamp.create()
        )
      )
  }
}
