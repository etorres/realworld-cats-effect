package es.eriktorr
package realworld.shared.adapter.persistence

import realworld.shared.application.JdbcConfig

import cats.effect.{IO, Resource}
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor

import scala.concurrent.duration.DurationInt

final class JdbcTransactor(jdbcConfig: JdbcConfig):
  val transactorResource: Resource[IO, HikariTransactor[IO]] = for
    _ <- Resource.eval[IO, Unit](IO.delay(Class.forName(jdbcConfig.driverClassName.value)))
    transactor <- HikariTransactor.fromHikariConfig[IO](hikariConfigFrom(jdbcConfig))
  yield transactor

  private def hikariConfigFrom(jdbcConfig: JdbcConfig) =
    val hikariConfig = new HikariConfig()
    hikariConfig.setDriverClassName(jdbcConfig.driverClassName.value)
    hikariConfig.setJdbcUrl(jdbcConfig.connectUrl)
    hikariConfig.setUsername(jdbcConfig.username)
    hikariConfig.setPassword(jdbcConfig.password.value)
    hikariConfig.setMinimumIdle(jdbcConfig.connections.start)
    hikariConfig.setMaximumPoolSize(jdbcConfig.connections.end)
    hikariConfig.setLeakDetectionThreshold(2000L)
    hikariConfig.setConnectionTimeout(30.seconds.toMillis)
    hikariConfig
