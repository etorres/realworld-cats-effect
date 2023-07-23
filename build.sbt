ThisBuild / organization := "es.eriktorr"
ThisBuild / version := "1.0.0"

ThisBuild / scalaVersion := "3.3.0"

ThisBuild / scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-source:future", // https://github.com/oleg-py/better-monadic-for
  "-Yexplicit-nulls", // https://docs.scala-lang.org/scala3/reference/other-new-features/explicit-nulls.html
  "-Ysafe-init", // https://docs.scala-lang.org/scala3/reference/other-new-features/safe-initialization.html
  "-Wunused:all",
)

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / semanticdbEnabled := true
ThisBuild / javacOptions ++= Seq("-source", "17", "-target", "17")

addCommandAlias(
  "check",
  "; unusedCompileDependenciesTest; scalafixAll; scalafmtSbtCheck; scalafmtCheckAll",
)

lazy val commons = project
  .in(file("libs/commons"))
  .configure(baseSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.monovore" %% "decline" % "2.4.1" % Optional,
      "com.zaxxer" % "HikariCP" % "5.0.1" % Optional exclude ("org.slf4j", "slf4j-api"),
      "io.circe" %% "circe-core" % "0.14.5" % Optional,
      "io.github.iltotore" %% "iron-cats" % "2.2.0-RC3" % Optional,
      "io.janstenpickle" %% "trace4cats-http4s-server" % "0.14.1" % Optional,
      "io.janstenpickle" %% "trace4cats-iolocal" % "0.14.4" % Optional,
      "org.http4s" %% "http4s-prometheus-metrics" % "0.24.4" % Optional,
      "org.http4s" %% "http4s-server" % "0.23.23" % Optional,
      "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC4" % Optional,
      "org.typelevel" %% "cats-collections-core" % "0.9.7" % Optional,
      "org.typelevel" %% "cats-core" % "2.9.0" % Optional,
      "org.typelevel" %% "cats-effect" % "3.5.0" % Optional,
      "org.typelevel" %% "log4cats-core" % "2.6.0" % Optional,
    ),
  )
  .disablePlugins(ScalafixPlugin) // disabling until all dependencies support Scala 3.3.x

lazy val `realworld-http` = project
  .in(file("apps/realworld-http"))
  .configure(baseSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.auth0" % "java-jwt" % "4.4.0",
      "com.lmax" % "disruptor" % "3.4.4" % Runtime,
      "com.monovore" %% "decline-effect" % "2.4.1",
      "com.password4j" % "password4j" % "1.7.1" exclude ("org.slf4j", "slf4j-api"),
      "com.zaxxer" % "HikariCP" % "5.0.1" exclude ("org.slf4j", "slf4j-api"),
      "io.circe" %% "circe-generic" % "0.14.5",
      "io.github.arainko" %% "ducktape" % "0.1.9",
      "io.github.iltotore" %% "iron-cats" % "2.2.0-RC3",
      "org.apache.logging.log4j" % "log4j-core" % "2.20.0" % Runtime,
      "org.apache.logging.log4j" % "log4j-layout-template-json" % "2.20.0" % Runtime,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.20.0" % Runtime,
      "org.http4s" %% "http4s-circe" % "0.23.23",
      "org.http4s" %% "http4s-dsl" % "0.23.23",
      "org.http4s" %% "http4s-ember-server" % "0.23.23",
      "org.http4s" %% "http4s-prometheus-metrics" % "0.24.4" % Runtime,
      "org.postgresql" % "postgresql" % "42.6.0" % Runtime,
      "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC4",
      "org.typelevel" %% "cats-collections-core" % "0.9.7",
      "org.typelevel" %% "cats-core" % "2.9.0",
      "org.typelevel" %% "cats-effect" % "3.5.1",
      "org.typelevel" %% "cats-effect-std" % "3.5.1",
      "org.typelevel" %% "log4cats-core" % "2.6.0",
      "org.typelevel" %% "log4cats-noop" % "2.6.0" % Test,
      "org.typelevel" %% "log4cats-slf4j" % "2.6.0",
    ),
    dockerApiVersion := com.typesafe.sbt.packager.docker.DockerApiVersion.parse("1.43"),
    dockerBaseImage := "eclipse-temurin:17-jre",
    dockerExposedPorts ++= Seq(8080),
    Universal / maintainer := "https://github.com/etorres/realworld-cats-effect",
  )
  .dependsOn(commons % "test->test;compile->compile")
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .disablePlugins(ScalafixPlugin) // disabling until all dependencies support Scala 3.3.x

lazy val MUnitFramework = new TestFramework("munit.Framework")
lazy val warts = Warts.unsafe.filter(_ != Wart.DefaultArguments)

lazy val baseSettings: Project => Project = _.settings(
  idePackagePrefix := Some("es.eriktorr"),
  Global / excludeLintKeys += idePackagePrefix,
  Compile / doc / sources := Seq(),
  Compile / compile / wartremoverErrors ++= warts,
  Test / compile / wartremoverErrors ++= warts,
  libraryDependencies ++= Seq(
    "com.47deg" %% "scalacheck-toolbox-datetime" % "0.7.0" % Test,
    "io.chrisdavenport" %% "cats-scalacheck" % "0.3.2" % Test,
    "org.scalameta" %% "munit" % "0.7.29" % Test,
    "org.scalameta" %% "munit-scalacheck" % "0.7.29" % Test,
    "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test,
    "org.typelevel" %% "scalacheck-effect" % "1.0.4" % Test,
    "org.typelevel" %% "scalacheck-effect-munit" % "1.0.4" % Test,
  ),
  Test / fork := true,
  Test / envVars := Map(
    "SBT_TEST_ENV_VARS" -> "true",
    "REALWORLD_HEALTH_LIVENESS_PATH" -> "/liveness-path",
    "REALWORLD_HEALTH_READINESS_PATH" -> "/readiness-path",
    "REALWORLD_HTTP_HOST" -> "localhost",
    "REALWORLD_HTTP_MAX_ACTIVE_REQUESTS" -> "1024",
    "REALWORLD_HTTP_PORT" -> "8000",
    "REALWORLD_HTTP_TIMEOUT" -> "11s",
    "REALWORLD_JDBC_CONNECTIONS" -> "2:4",
    "REALWORLD_JDBC_CONNECT_URL" -> "jdbc:postgresql://localhost:3306/database_name",
    "REALWORLD_JDBC_PASSWORD" -> "database_password",
    "REALWORLD_JDBC_USERNAME" -> "database_username",
    "REALWORLD_JWT_ISSUER" -> "jwt_issuer",
    "REALWORLD_JWT_SECRET" -> "jwt_secret",
  ),
  Test / testFrameworks += MUnitFramework,
  Test / testOptions += Tests.Argument(MUnitFramework, "--exclude-tags=online"),
)

lazy val root = project
  .in(file("."))
  .aggregate(commons, `realworld-http`)
  .settings(
    name := "realworld-cats-effect",
    Compile / doc / sources := Seq(),
    publish := {},
    publishLocal := {},
  )
