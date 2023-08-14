package es.eriktorr
package realworld.application

import realworld.adapter.rest.{ArticlesRestController, ProfileRestController, UsersRestController}
import realworld.application.JwtAuthMiddleware.jwtAuthMiddleware
import realworld.domain.model.UserId
import realworld.domain.service.{ArticlesService, AuthService, UsersService}
import realworld.shared.adapter.rest.{HealthService, MetricsService, TraceService}

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits.{catsSyntaxTuple3Semigroupal, toSemigroupKOps}
import org.http4s.dsl.io.*
import org.http4s.server.middleware.{GZip, Logger as Http4sLogger, RequestId}
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpApp, HttpRoutes, Response, Status}
import org.typelevel.log4cats.SelfAwareStructuredLogger

import scala.annotation.tailrec
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
  private val maybeApiEndpoint: Option[HttpRoutes[IO]] =
    val endpoints = List(
      ArticlesRestController(articlesService),
      ProfileRestController(usersService),
      UsersRestController(usersService),
    )

    @tailrec
    def composedAuthedRoutes(
        aggregated: AuthedRoutes[UserId, IO],
        routes: List[AuthedRoutes[UserId, IO]],
    ): AuthedRoutes[UserId, IO] = routes match
      case Nil => aggregated
      case ::(head, next) => composedAuthedRoutes(head <+> aggregated, next)

    @tailrec
    def composedHttpRoutes(
        aggregated: HttpRoutes[IO],
        routes: List[HttpRoutes[IO]],
    ): HttpRoutes[IO] = routes match
      case Nil => aggregated
      case ::(head, next) => composedHttpRoutes(head <+> aggregated, next)

    (
      NonEmptyList
        .fromList(
          endpoints.map(_.optionalAuthRoutes).collect { case Some(value) => value },
        )
        .map(nel => composedAuthedRoutes(nel.head, nel.tail)),
      NonEmptyList
        .fromList(endpoints.map(_.publicRoutes).collect { case Some(value) => value })
        .map(nel => composedHttpRoutes(nel.head, nel.tail)),
      NonEmptyList
        .fromList(endpoints.map(_.secureRoutes).collect { case Some(value) => value })
        .map(nel => composedAuthedRoutes(nel.head, nel.tail)),
    ).mapN { case (optionalAuthRoutes, publicRoutes, secureRoutes) =>
      metricsService
        .metricsFor(
          publicRoutes <+> authMiddleware(optionalAuthRoutes) <+> authMiddleware(secureRoutes),
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
    }

  private lazy val authMiddleware: AuthMiddleware[IO, UserId] = jwtAuthMiddleware[UserId](token =>
    for
      email <- authService.verify(token)
      userId <- usersService.userIdFor(email)
    yield userId,
  )

  private val livenessCheckEndpoint: HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> Root =>
    Ok(s"${healthService.serviceName} is live")
  }

  private val readinessCheckEndpoint: HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> Root =>
    healthService.isReady.ifM(
      ifTrue = Ok(s"${healthService.serviceName} is ready"),
      ifFalse = ServiceUnavailable(s"${healthService.serviceName} is not ready"),
    )
  }

  val httpApp: HttpApp[IO] = (maybeApiEndpoint match
    case Some(apiEndpoint) =>
      Router(
        "/api" -> apiEndpoint,
        healthService.livenessPath -> livenessCheckEndpoint,
        healthService.readinessPath -> readinessCheckEndpoint,
        "/" -> metricsService.prometheusExportRoutes,
      )
    case None =>
      Router("/" -> HttpRoutes.of[IO] { case _ => IO(Response(Status.InternalServerError)) })
  ).orNotFound
