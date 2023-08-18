package es.eriktorr
package realworld.common.db

import realworld.application.JdbcTestConfig

import cats.effect.{IO, Resource}
import cats.implicits.toFoldableOps
import doobie.Fragment
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import org.typelevel.log4cats.Logger

final class PostgresTestTransactor(jdbcTestConfig: JdbcTestConfig)(using logger: Logger[IO]):
  val resource: Resource[IO, HikariTransactor[IO]] = for
    transactor <- JdbcTransactor(jdbcTestConfig.config).resource
    _ <- Resource.eval((for
      tableNames <-
        sql"""select table_name
             |from information_schema.tables
             |where table_schema='public'
             | and table_name not like 'flyway_%'""".stripMargin.query[String].to[List]
      _ <- tableNames
        .map(tableName => Fragment.const(s"truncate table $tableName cascade"))
        .traverse_(_.update.run)
    yield ()).transact(transactor))
  yield transactor
