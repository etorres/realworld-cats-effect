package es.eriktorr
package realworld.adapter.rest

import realworld.adapter.rest.request.{InvalidRequest, LoginUserRequest, RegisterNewUserRequest}
import realworld.adapter.rest.response.LoginUserResponse
import realworld.domain.service.UsersService
import realworld.domain.service.UsersService.AccessForbidden
import realworld.shared.data.validated.ValidatedNecExtensions.validatedNecTo

import cats.effect.IO
import cats.implicits.catsSyntaxMonadError
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.io.*
import org.http4s.{HttpRoutes, Request, Response}
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.SelfAwareStructuredLogger

final class UsersRestController(usersService: UsersService)(using
    logger: SelfAwareStructuredLogger[IO],
):
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO]:
    case request @ POST -> Root / "users" =>
      (for
        _ <- request.as[RegisterNewUserRequest]
        // TODO
        response <- Ok()
      yield response).handleErrorWith(contextFrom(request))

    case request @ POST -> Root / "users" / "login" =>
      (for
        credentials <- request
          .as[LoginUserRequest]
          .flatMap(_.toCredentials.validated)
          .adaptError:
            case error => InvalidRequest(error)
        user <- usersService.loginUserIdentifiedBy(credentials)
        response <- Ok(LoginUserResponse(user))
      yield response).handleErrorWith(contextFrom(request))

  private def contextFrom(request: Request[IO]): Throwable => IO[Response[IO]] =
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
