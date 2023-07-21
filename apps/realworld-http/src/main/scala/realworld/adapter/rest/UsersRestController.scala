package es.eriktorr
package realworld.adapter.rest

import realworld.adapter.rest.request.UserLoginRequest
import realworld.adapter.rest.response.UserResponse
import realworld.domain.model.Error.InvalidCredentials
import realworld.domain.service.UsersService

import cats.effect.IO
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.io.*
import org.http4s.{HttpRoutes, Request, Response}
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.SelfAwareStructuredLogger

final class UsersRestController(usersService: UsersService)(using
    logger: SelfAwareStructuredLogger[IO],
):
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO]:
    case request @ POST -> Root / "users" / "login" =>
      (for
        userCredentials <- request.as[UserLoginRequest].map(_.toUserCredentials)
        user <- usersService.loginUserIdentifiedBy(userCredentials)
        response <- Ok(UserResponse.from(user))
      yield response).handleErrorWith(errorHandler(request))

  private def errorHandler(request: Request[IO]): Throwable => IO[Response[IO]] =
    (error: Throwable) =>
      val requestId = request.headers.get(ci"X-Request-ID").map(_.head.value)
      val context = requestId.fold(Map.empty)(value => Map("http.request.id" -> value))
      error match
        case invalidCredentials: InvalidCredentials =>
          logger.error(context)("Unauthorized access") *> Forbidden()
        case other =>
          logger.error(context, other)(
            "Unhandled error raised while handling request",
          ) *> InternalServerError()
