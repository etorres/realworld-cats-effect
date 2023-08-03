package es.eriktorr
package realworld.shared.spec

import realworld.shared.adapter.persistence.{JdbcMigrator, PostgresTestTransactor}
import realworld.shared.application.JdbcTestConfig

import cats.effect.IO
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.Test
import org.typelevel.log4cats.slf4j.Slf4jLogger

trait PostgresSuite extends CatsEffectSuite with ScalaCheckEffectSuite:
  override def scalaCheckTestParameters: Test.Parameters =
    super.scalaCheckTestParameters.withMinSuccessfulTests(1).withWorkers(1)

  override def beforeAll(): Unit =
    JdbcMigrator(JdbcTestConfig.LocalContainer.config)(using Slf4jLogger.getLogger[IO]).migrate
      .unsafeRunSync()

  val testTransactor: PostgresTestTransactor = PostgresTestTransactor(JdbcTestConfig.LocalContainer)
