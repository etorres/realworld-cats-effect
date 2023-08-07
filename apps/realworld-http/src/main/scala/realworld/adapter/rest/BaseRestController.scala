package es.eriktorr
package realworld.adapter.rest

import realworld.adapter.rest.BaseRestController.{InvalidRequest, Transformer}
import realworld.adapter.rest.JwtAuthMiddleware.jwtAuthMiddleware
import realworld.domain.model.UserId
import realworld.domain.service.UsersService.AccessForbidden
import realworld.domain.service.{AuthService, UsersService}
import realworld.shared.data.error.HandledError
import realworld.shared.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}

import cats.effect.IO
import cats.implicits.catsSyntaxMonadError
import io.circe.Decoder
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.server.AuthMiddleware
import org.http4s.{Request, Response}
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.SelfAwareStructuredLogger

abstract class BaseRestController(authService: AuthService, usersService: UsersService):
  protected val authMiddleware: AuthMiddleware[IO, UserId] = jwtAuthMiddleware[UserId](token =>
    for
      email <- authService.verify(token)
      userId <- usersService.userIdFor(email)
    yield userId,
  )

  protected def contextFrom(request: Request[IO])(using
      logger: SelfAwareStructuredLogger[IO],
  ): Throwable => IO[Response[IO]] =
    (error: Throwable) =>
      val requestId = request.headers.get(ci"X-Request-ID").map(_.head.value)
      val context = requestId.fold(Map.empty)(value => Map("http.request.id" -> value))
      error match
        case accessForbidden: AccessForbidden =>
          logger.error(context, accessForbidden)("Access forbidden") *> Forbidden()
        case _: InvalidRequest => BadRequest()
        case other =>
          logger.error(context, other)(
            "Unhandled error raised while handling request",
          ) *> InternalServerError()

  protected def validatedInputFrom[A, B](
      request: Request[IO],
  )(using decoder: Decoder[A], transformer: Transformer[A, B]): IO[B] =
    request
      .as[A]
      .flatMap(transformer.transform(_).validated)
      .adaptError:
        case error => InvalidRequest(error)

object BaseRestController:
  trait Transformer[A, B]:
    def transform(value: A): AllErrorsOr[B]

  final case class InvalidRequest(cause: Throwable)
      extends HandledError("Invalid request", Some(cause))
