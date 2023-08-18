package es.eriktorr
package realworld.application

import realworld.application.HealthConfig.{LivenessPath, ReadinessPath}
import realworld.application.HttpServerConfig.MaxActiveRequests
import realworld.application.JdbcConfig.{ConnectUrl, Password, Username}
import realworld.application.SecurityConfig.{JwtIssuer, JwtSecret}
import realworld.common.Secret

import cats.collections.Range
import cats.implicits.catsSyntaxEitherId
import com.comcast.ip4s.{host, port}
import com.monovore.decline.{Command, Help}
import io.github.iltotore.iron.autoRefine
import munit.FunSuite

import scala.concurrent.duration.DurationInt
import scala.util.Properties

final class RealWorldConfigSuite extends FunSuite:
  test("should load configuration from environment variables"):
    assume(Properties.envOrNone("SBT_TEST_ENV_VARS").nonEmpty, "this test runs only on sbt")
    assertEquals(
      Command(name = "name", header = "header")(RealWorldConfig.opts)
        .parse(List.empty, sys.env),
      RealWorldConfig(
        HealthConfig(
          LivenessPath("/liveness-path"),
          ReadinessPath("/readiness-path"),
        ),
        HttpServerConfig(host"localhost", MaxActiveRequests(1024L), port"8000", 11.seconds),
        JdbcConfig.postgresql(
          Range(2, 4),
          ConnectUrl("jdbc:postgresql://localhost:3306/database_name"),
          Secret(Password("database_password")),
          Username("database_username"),
        ),
        SecurityConfig(JwtIssuer("jwt_issuer"), Secret(JwtSecret("jwt_secret"))),
      ).asRight[Help],
    )
