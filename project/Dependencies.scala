import sbt._

object Dependencies {

  val AirframeVersion       = "23.3.4"
  val AkkaHttpVersion       = "10.4.0"
  val AkkaManagementVersion = "1.2.0"
  val AkkaVersion           = "2.7.0"
  val ByteBuddyVersion      = "1.14.2"
  val CirceVersion          = "0.14.5"
  val CirceYamlVersion      = "0.14.2"
  val Http4sVersion         = "0.23.30"

  val GoogleAutoServiceVersion        = "1.0.1"
  val LogbackVersion                  = "1.4.6"
  val OpentelemetryVersion            = "1.24.0"
  val OpentelemetryMinor0Version      = "1.24.0"
  val OpentelemetryAlphaVersion       = "1.24.0-alpha"
  val OpentelemetryAlphaMinor0Version = "1.24.0-alpha"
  val PostgresVersion                 = "42.6.0"
  val ScalatestVersion                = "3.2.15"
  val TestcontainersScalaVersion      = "0.40.14"
  val SlickVersion                    = "3.4.1"

  val akka = Seq(
    "com.typesafe.akka" %% "akka-http"                   % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json"        % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-stream"                 % AkkaVersion,
    "com.typesafe.akka" %% "akka-cluster-typed"          % AkkaVersion,
    "com.typesafe.akka" %% "akka-persistence-typed"      % AkkaVersion,
    "com.typesafe.akka" %% "akka-actor-typed"            % AkkaVersion,
    "com.typesafe.akka" %% "akka-actor"                  % AkkaVersion,
    "com.typesafe.akka" %% "akka-serialization-jackson"  % AkkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion
  )

  val akkaPersistence = Seq(
    "com.typesafe.akka"  %% "akka-persistence-typed" % AkkaVersion,
    "com.lightbend.akka" %% "akka-persistence-jdbc"  % "5.2.0",
    "com.typesafe.akka"  %% "akka-persistence-query" % AkkaVersion
  )

  val zio = Seq(
    "dev.zio" %% "zio" % "2.0.10"
  )

  val http4s = Seq(
    "org.http4s" %% "http4s-ember-server" % Http4sVersion,
    "org.http4s" %% "http4s-dsl"          % Http4sVersion
  )

  val http4sClient = Seq(
    "org.http4s" %% "http4s-ember-client" % Http4sVersion
  )

  val byteBuddy = Seq(
    "net.bytebuddy" % "byte-buddy"       % ByteBuddyVersion,
    "net.bytebuddy" % "byte-buddy-agent" % ByteBuddyVersion
  )

  val logback = Seq("ch.qos.logback" % "logback-classic" % LogbackVersion)

  val openTelemetryInstrumentationApiSemanticConventions = Seq(
    "io.opentelemetry.instrumentation" % "opentelemetry-instrumentation-api-semconv" % OpentelemetryAlphaMinor0Version
  )

  val opentelemetryExtensionApi = Seq(
    "io.opentelemetry.javaagent" % "opentelemetry-javaagent-extension-api" % OpentelemetryAlphaMinor0Version
  )

  val openTelemetryExtension = Seq(
    "com.google.auto.service"    % "auto-service"                    % GoogleAutoServiceVersion,
    "io.opentelemetry.javaagent" % "opentelemetry-javaagent-tooling" % OpentelemetryAlphaMinor0Version
  )

  val openTelemetryMuzzle = Seq(
    "io.opentelemetry.javaagent" % "opentelemetry-muzzle"              % OpentelemetryAlphaMinor0Version,
    "io.opentelemetry.javaagent" % "opentelemetry-javaagent-bootstrap" % OpentelemetryAlphaVersion
  )

  val openTelemetryTesting = Seq(
    "io.opentelemetry.javaagent" % "opentelemetry-testing-common" % OpentelemetryAlphaMinor0Version
  )

  val akkaTestkit = Seq(
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion,
    "com.typesafe.akka" %% "akka-stream-testkit"      % AkkaVersion,
    "com.typesafe.akka" %% "akka-http-testkit"        % AkkaHttpVersion
  )

  val scalatest = Seq("org.scalatest" %% "scalatest" % ScalatestVersion)

  val testcontainersScala = Seq("com.dimafeng" %% "testcontainers-scala" % TestcontainersScalaVersion)

  val circe = Seq(
    "io.circe" %% "circe-core"    % CirceVersion,
    "io.circe" %% "circe-generic" % CirceVersion,
    "io.circe" %% "circe-parser"  % CirceVersion,
    "io.circe" %% "circe-yaml"    % CirceYamlVersion
  )
}
