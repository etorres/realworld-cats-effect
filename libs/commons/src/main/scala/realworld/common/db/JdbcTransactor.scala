package es.eriktorr
package realworld.common.db

import realworld.application.JdbcConfig

import cats.effect.{IO, Resource}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.hikari.HikariTransactor
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.DurationInt

final class JdbcTransactor(jdbcConfig: JdbcConfig)(using logger: Logger[IO]):
  val resource: Resource[IO, HikariTransactor[IO]] =
    for
      dataSource <- Resource.fromAutoCloseable(hikariDataSourceFrom(jdbcConfig))
      _ <- Resource.eval(JdbcMigrator(dataSource).migrate)
      transactor <- HikariTransactor.fromHikariConfig[IO](dataSource)
    yield transactor

  private def hikariDataSourceFrom(jdbcConfig: JdbcConfig) =
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl(jdbcConfig.connectUrl)
    hikariConfig.setUsername(jdbcConfig.username)
    hikariConfig.setPassword(jdbcConfig.password.value)
    hikariConfig.setMinimumIdle(jdbcConfig.connections.start)
    hikariConfig.setMaximumPoolSize(jdbcConfig.connections.end)
    hikariConfig.setLeakDetectionThreshold(2000L)
    hikariConfig.setConnectionTimeout(30.seconds.toMillis)
    IO.delay(new HikariDataSource(hikariConfig))
