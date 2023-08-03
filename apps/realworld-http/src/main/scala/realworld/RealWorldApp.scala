package es.eriktorr
package realworld

import realworld.adapter.persistence.PostgresUsersRepository
import realworld.application.{HttpServer, RealWorldConfig, RealWorldHttpApp, RealWorldParams}
import realworld.domain.service.{AuthService, UsersService}
import realworld.shared.ConsoleLogger
import realworld.shared.adapter.persistence.{JdbcMigrator, JdbcTransactor}
import realworld.shared.adapter.rest.{HealthService, MetricsService, ServiceName, TraceService}

import cats.effect.{ExitCode, IO, Resource}
import cats.implicits.{catsSyntaxTuple2Semigroupal, showInterpolator}
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import io.github.iltotore.iron.autoRefine
import org.http4s.server.middleware.{MaxActiveRequests, Timeout}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object RealWorldApp extends CommandIOApp(name = "realworld-http", header = "RealWorld REST API"):
  override def main: Opts[IO[ExitCode]] =
    (RealWorldConfig.opts, RealWorldParams.opts).mapN { case (config, params) =>
      program(config, params)
    }

  private def program(config: RealWorldConfig, params: RealWorldParams) = for
    logger <- Slf4jLogger.create[IO]
    given SelfAwareStructuredLogger[IO] = logger
    _ <- logger.info(show"Starting HTTP server with configuration: $config")
    _ <- JdbcMigrator(config.jdbcConfig).migrate
    _ <- (for
      consoleLogger <- ConsoleLogger.resource
      transactor <- JdbcTransactor(config.jdbcConfig).resource
      authService = AuthService.impl(config.securityConfig)
      usersRepository = PostgresUsersRepository(transactor)
      usersService = UsersService(authService, usersRepository)
      serviceName = ServiceName("RealWorld")
      healthService <- HealthService.resourceWith(config.healthConfig, serviceName)
      metricsService <- MetricsService.resourceWith("http4s_server")
      traceService <- TraceService.resourceWith(serviceName)
      httpApp <- Resource.eval:
        MaxActiveRequests
          .forHttpApp[IO](config.httpServerConfig.maxActiveRequests)
          .map: middleware =>
            // Limit the number of active requests by rejecting requests over the limit defined
            middleware(
              RealWorldHttpApp(
                healthService,
                metricsService,
                traceService,
                usersService,
                params.verbose,
              ).httpApp,
            )
          .map: decoratedHttpApp =>
            // Limit how long the underlying service takes to respond
            Timeout.httpApp[IO](timeout = config.httpServerConfig.timeout)(decoratedHttpApp)
      _ <- HttpServer.impl(httpApp, config.httpServerConfig)
    yield healthService).use(_.markReady.flatMap(_ => IO.never))
  yield ExitCode.Success
