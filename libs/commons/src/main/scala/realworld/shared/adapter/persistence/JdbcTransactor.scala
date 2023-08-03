package es.eriktorr
package realworld.shared.adapter.persistence

import realworld.shared.application.JdbcConfig

import cats.effect.{IO, Resource}
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor

import scala.concurrent.duration.DurationInt

final class JdbcTransactor(jdbcConfig: JdbcConfig):
  val resource: Resource[IO, HikariTransactor[IO]] =
    HikariTransactor.fromHikariConfig[IO](hikariConfigFrom(jdbcConfig))

  private def hikariConfigFrom(jdbcConfig: JdbcConfig) =
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl(jdbcConfig.connectUrl)
    hikariConfig.setUsername(jdbcConfig.username)
    hikariConfig.setPassword(jdbcConfig.password.value)
    hikariConfig.setMinimumIdle(jdbcConfig.connections.start)
    hikariConfig.setMaximumPoolSize(jdbcConfig.connections.end)
    hikariConfig.setLeakDetectionThreshold(2000L)
    hikariConfig.setConnectionTimeout(30.seconds.toMillis)
    hikariConfig
