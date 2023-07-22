package es.eriktorr
package realworld.adapter.rest

import realworld.adapter.rest.request.UserLoginRequest
import realworld.adapter.rest.response.UserResponse
import realworld.domain.service.UsersService
import realworld.domain.service.UsersService.AccessForbidden
import realworld.shared.data.validated.ValidatedNecExtensions.validatedNecTo

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
        userLoginRequest <- request.as[UserLoginRequest]
        credentials <- userLoginRequest.toCredentials.validated
        user <- usersService.loginUserIdentifiedBy(credentials)
        response <- Ok(UserResponse.from(user))
      yield response).handleErrorWith(contextFrom(request))

  private def contextFrom(request: Request[IO]): Throwable => IO[Response[IO]] =
    (error: Throwable) =>
      val requestId = request.headers.get(ci"X-Request-ID").map(_.head.value)
      val context = requestId.fold(Map.empty)(value => Map("http.request.id" -> value))
      error match
        case accessForbidden: AccessForbidden =>
          logger.error(context)("Unauthorized access") *> Forbidden()
        case other =>
          logger.error(context, other)(
            "Unhandled error raised while handling request",
          ) *> InternalServerError()
