package es.eriktorr
package realworld.common.api

import realworld.common.api.BaseRestController.{InvalidRequest, Transformer}
import realworld.common.data.error.HandledError
import realworld.common.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}
import realworld.users.core.domain.{UserId, UsersService}
import realworld.users.core.domain.UsersService.AccessForbidden

import cats.effect.IO
import cats.implicits.catsSyntaxMonadError
import io.circe.Decoder
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.{AuthedRoutes, HttpRoutes, Request, Response}
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.SelfAwareStructuredLogger

abstract class BaseRestController:
  val optionalAuthRoutes: Option[AuthedRoutes[UserId, IO]] = None
  val publicRoutes: Option[HttpRoutes[IO]] = None
  val secureRoutes: Option[AuthedRoutes[UserId, IO]] = None

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
