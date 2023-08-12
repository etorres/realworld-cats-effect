package es.eriktorr
package realworld.application

import realworld.adapter.rest.{ArticlesRestController, ProfileRestController, UsersRestController}
import realworld.domain.service.{ArticlesService, AuthService, UsersService}
import realworld.shared.adapter.rest.{HealthService, MetricsService, TraceService}

import cats.effect.IO
import cats.implicits.toSemigroupKOps
import org.http4s.dsl.io.*
import org.http4s.server.Router
import org.http4s.server.middleware.{GZip, Logger as Http4sLogger, RequestId}
import org.http4s.{HttpApp, HttpRoutes}
import org.typelevel.log4cats.SelfAwareStructuredLogger

import scala.util.chaining.scalaUtilChainingOps

final class RealWorldHttpApp(
    articlesService: ArticlesService,
    authService: AuthService,
    healthService: HealthService,
    metricsService: MetricsService,
    traceService: TraceService,
    usersService: UsersService,
    enableLogger: Boolean = false,
)(using logger: SelfAwareStructuredLogger[IO]):
  private val apiEndpoint: HttpRoutes[IO] = metricsService
    .metricsFor(
      UsersRestController(
        authService,
        usersService,
      ).routes <+> ArticlesRestController(
        articlesService,
        authService,
        usersService,
      ).routes <+> ProfileRestController(
        authService,
        usersService,
      ).routes,
    )
    .pipe: routes =>
      // Allow the compression of the Response body using GZip
      GZip(routes)
    .pipe: routes =>
      // Automatically generate a X-Request-ID header for a request, if one wasn't supplied
      RequestId.httpRoutes(routes)
    .pipe: routes =>
      // When enabled, logs all requests and responses
      if enableLogger then
        Http4sLogger.httpRoutes(
          logHeaders = true,
          logBody = true,
          redactHeadersWhen = _ => false, // TODO
          logAction = Some((msg: String) => logger.info(msg)),
        )(routes)
      else routes
    .pipe: routes =>
      // Makes a probabilistic sampling and tracing of requests and responses
      traceService.trace(routes)

  private val livenessCheckEndpoint: HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> Root =>
    Ok(s"${healthService.serviceName} is live")
  }

  private val readinessCheckEndpoint: HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> Root =>
    healthService.isReady.ifM(
      ifTrue = Ok(s"${healthService.serviceName} is ready"),
      ifFalse = ServiceUnavailable(s"${healthService.serviceName} is not ready"),
    )
  }

  val httpApp: HttpApp[IO] = Router(
    "/api" -> apiEndpoint,
    healthService.livenessPath -> livenessCheckEndpoint,
    healthService.readinessPath -> readinessCheckEndpoint,
    "/" -> metricsService.prometheusExportRoutes,
  ).orNotFound
