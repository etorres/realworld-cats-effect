package es.eriktorr
package realworld.shared.spec

import realworld.shared.adapter.persistence.PostgresTestTransactor
import realworld.shared.application.JdbcTestConfig

import cats.effect.IO
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.Test
import org.typelevel.log4cats.slf4j.Slf4jLogger

trait PostgresSuite extends CatsEffectSuite with ScalaCheckEffectSuite:
  override def scalaCheckTestParameters: Test.Parameters =
    super.scalaCheckTestParameters.withMinSuccessfulTests(1).withWorkers(1)

  val testTransactor: PostgresTestTransactor =
    PostgresTestTransactor(JdbcTestConfig.LocalContainer)(using Slf4jLogger.getLogger[IO])
