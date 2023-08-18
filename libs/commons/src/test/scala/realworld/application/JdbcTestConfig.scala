package es.eriktorr
package realworld.application

import realworld.application.JdbcConfig.{ConnectUrl, Password, Username}
import realworld.common.Secret

import cats.collections.Range
import io.github.iltotore.iron.{autoRefine, refine}

enum JdbcTestConfig(val config: JdbcConfig, val database: String):
  case LocalContainer
      extends JdbcTestConfig(
        JdbcConfig.postgresql(
          Range(1, 3),
          ConnectUrl(
            s"jdbc:postgresql://${JdbcTestConfig.host}/${JdbcTestConfig.database}".refine,
          ),
          Secret(Password("changeMe")),
          Username("test"),
        ),
        JdbcTestConfig.database,
      )

object JdbcTestConfig:
  final private val host = "postgres.test:5432"
  final private val database = "realworld"
