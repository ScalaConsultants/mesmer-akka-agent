package io.scalac.mesmer.extension.util

import akka.actor.typed.receptionist.Receptionist.Listing
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

object GenericBehaviors {

  private case object Timeout

  /**
   * Creates behavior that waits for service to be accessible on [[ serviceKey ]]
   * After that it transition to specified behavior using [[ next ]] function as factory
   * @param serviceKey
   * @param next factory function creating target behavior
   * @tparam T
   * @tparam I
   * @return
   */
  def waitForService[T, I: ClassTag](serviceKey: ServiceKey[T], bufferSize: Int = 1024)(
    next: ActorRef[T] => Behavior[I]
  ): Behavior[I] = waitForServiceInternal(serviceKey, bufferSize, None)(next, Behaviors.stopped)

  def waitForServiceWithTimeout[T, I: ClassTag](
    serviceKey: ServiceKey[T],
    timeout: FiniteDuration,
    bufferSize: Int = 1024
  )(
    next: ActorRef[T] => Behavior[I],
    onTimeout: => Behavior[I]
  ): Behavior[I] = waitForServiceInternal(serviceKey, bufferSize, Some(timeout))(next, onTimeout)

  /**
   * Creates behavior that waits for service to be accessible on [[ serviceKey ]]
   * After that it transition to specified behavior using [[ next ]] function as factory
   * @param serviceKey
   * @param next factory function creating target behavior
   * @tparam T
   * @tparam I
   * @return
   */
  private def waitForServiceInternal[T, I: ClassTag](
    serviceKey: ServiceKey[T],
    bufferSize: Int,
    timeout: Option[FiniteDuration]
  )(
    next: ActorRef[T] => Behavior[I],
    onTimeout: => Behavior[I] = Behaviors.stopped
  ): Behavior[I] =
    Behaviors
      .setup[Any] { context => // use any to mimic union types

        Behaviors.withTimers { scheduler =>
          import context._

          def start(): Behavior[Any] = {

            val adapter = context.messageAdapter[Listing](identity)

            system.receptionist ! Receptionist.Subscribe(serviceKey, adapter)

            timeout.foreach(after => scheduler.startSingleTimer(Timeout, Timeout, after))
            waitingForService()
          }

          def waitingForService(): Behavior[Any] =
            Behaviors.withStash(bufferSize) { buffer =>
              Behaviors.receiveMessagePartial {
                case listing: Listing =>
                  listing
                    .serviceInstances(serviceKey)
                    .headOption
                    .fold[Behavior[Any]] {
                      log.debug("No service found")
                      Behaviors.same
                    } { service =>
                      log.trace("Transition to inner behavior")
                      timeout.foreach(_ => scheduler.cancel(Timeout))
                      buffer.unstashAll(
                        next(service)
                          .transformMessages[Any] { // we must create interceptor that will filter all other messages that don't much inner type parameter
                            case message: I => message
                          }
                      )

                    }
                case Timeout =>
                  buffer.unstashAll(
                    onTimeout
                      .transformMessages[Any] { // we must create interceptor that will filter all other messages that don't much inner type parameter
                        case message: I => message
                      }
                  )
                case message: I =>
                  buffer.stash(message)
                  Behaviors.same
                case _ => Behaviors.unhandled
              }
            }

          start()
        }
      }
      .narrow[I]
}
