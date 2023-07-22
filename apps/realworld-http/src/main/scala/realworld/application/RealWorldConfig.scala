package es.eriktorr
package realworld.application

import realworld.shared.application.HealthConfig.{LivenessPath, ReadinessPath, given}
import realworld.shared.application.HttpServerConfig.{MaxActiveRequests, given}
import realworld.shared.application.JdbcConfig.{ConnectUrl, Password, Username, given}
import realworld.shared.application.SecurityConfig.{JwtIssuer, JwtSecret}
import realworld.shared.application.{HealthConfig, HttpServerConfig, JdbcConfig, SecurityConfig}

import cats.Show
import cats.collections.Range
import cats.implicits.{catsSyntaxTuple2Semigroupal, catsSyntaxTuple4Semigroupal, showInterpolator}
import com.comcast.ip4s.{Host, Port}
import com.monovore.decline.{Argument, Opts}
import es.eriktorr.realworld.shared.Secret
import io.github.iltotore.iron.refine

import scala.concurrent.duration.FiniteDuration

final case class RealWorldConfig(
    healthConfig: HealthConfig,
    httpServerConfig: HttpServerConfig,
    jdbcConfig: JdbcConfig,
    securityConfig: SecurityConfig,
)

object RealWorldConfig:

  given Show[RealWorldConfig] =
    import scala.language.unsafeNulls
    Show.show(config => show"""[${config.healthConfig},
                              | ${config.httpServerConfig},
                              | ${config.jdbcConfig},
                              | ${config.securityConfig}]""".stripMargin.replaceAll("\\R", ""))

  def opts: Opts[RealWorldConfig] =
    val securityConfig =
      (
        Opts
          .env[JwtIssuer](name = "REALWORLD_JWT_ISSUER", help = "Set JWT issuer.")
          .withDefault(JwtIssuer("RealWorld Service".refine)),
        Opts
          .env[JwtSecret](name = "REALWORLD_JWT_SECRET", help = "Set JWT secret.")
          .map(Secret.apply[JwtSecret]),
      ).mapN(SecurityConfig.apply)

    val healthConfig = (
      Opts
        .env[LivenessPath](
          name = "REALWORLD_HEALTH_LIVENESS_PATH",
          help = "Set liveness path.",
        )
        .withDefault(HealthConfig.defaultLivenessPath),
      Opts
        .env[ReadinessPath](
          name = "REALWORLD_HEALTH_READINESS_PATH",
          help = "Set readiness path.",
        )
        .withDefault(HealthConfig.defaultReadinessPath),
    ).mapN(HealthConfig.apply)

    val httpServerConfig = (
      Opts
        .env[Host](name = "REALWORLD_HTTP_HOST", help = "Set HTTP host.")
        .withDefault(HttpServerConfig.defaultHost),
      Opts
        .env[MaxActiveRequests](
          name = "REALWORLD_HTTP_MAX_ACTIVE_REQUESTS",
          help = "Set HTTP max active requests.",
        )
        .withDefault(HttpServerConfig.defaultMaxActiveRequests),
      Opts
        .env[Port](name = "REALWORLD_HTTP_PORT", help = "Set HTTP port.")
        .withDefault(HttpServerConfig.defaultPort),
      Opts
        .env[FiniteDuration]("REALWORLD_HTTP_TIMEOUT", help = "Set HTTP timeout.")
        .withDefault(HttpServerConfig.defaultTimeout),
    ).mapN(HttpServerConfig.apply)

    val jdbcConfig =
      (
        Opts
          .env[Range[Int]](
            name = "REALWORLD_JDBC_CONNECTIONS",
            help = "Set JDBC Connections.",
          )
          .validate("Must be between 1 and 16")(_.overlaps(Range(1, 16)))
          .withDefault(Range(1, 3)),
        Opts.env[ConnectUrl](
          name = "REALWORLD_JDBC_CONNECT_URL",
          help = "Set JDBC Connect URL.",
        ),
        Opts
          .env[Password](
            name = "REALWORLD_JDBC_PASSWORD",
            help = "Set JDBC Password.",
          )
          .map(Secret.apply[Password]),
        Opts.env[Username](
          name = "REALWORLD_JDBC_USERNAME",
          help = "Set JDBC Username.",
        ),
      ).mapN(JdbcConfig.mysql)

    (healthConfig, httpServerConfig, jdbcConfig, securityConfig).mapN(RealWorldConfig.apply)
