package io.scalac.core.support

import io.scalac.core.model.SupportedVersion._
import io.scalac.core.model.{ Module, SupportedVersion }

trait ModulesSupport {
  def akkaActor: SupportedVersion
  def akkaHttp: SupportedVersion
  def akkaPersistenceTyped: SupportedVersion
  def akkaClusterTyped: SupportedVersion
}

object ModulesSupport extends ModulesSupport {

  val akkaHttpModule             = Module("akka-http")
  val akkaClusterTypedModule     = Module("akka-cluster-typed")
  val akkaPersistenceTypedModule = Module("akka-persistence-typed")
  val akkaActorTypedModule       = Module("akka-actor-typed")
  val akkaActorModule            = Module("akka-actor")

  override def akkaActor: SupportedVersion = majors("2").and(minors("6")).and(patches("8"))

  override def akkaHttp: SupportedVersion =
    majors("10")
      .and(minors("1").and(patches("8")).or(minors("2").and(patches("0", "1", "2"))))

  override def akkaPersistenceTyped: SupportedVersion = majors("2").and(minors("6")).and(patches("8", "9", "10"))

  override def akkaClusterTyped: SupportedVersion = majors("2").and(minors("6")).and(patches("8", "9", "10"))
}
