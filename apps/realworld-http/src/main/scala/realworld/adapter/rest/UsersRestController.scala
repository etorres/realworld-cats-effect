package es.eriktorr
package realworld.adapter.rest

import realworld.adapter.rest.request.{LoginUserRequest, RegisterNewUserRequest, UpdateUserRequest}
import realworld.adapter.rest.response.{
  GetCurrentUserResponse,
  LoginUserResponse,
  RegisterNewUserResponse,
  UpdateUserResponse,
}
import realworld.domain.model.UserWithPassword.UserWithPlaintextPassword
import realworld.domain.model.{Credentials, UserId}
import realworld.domain.service.{AuthService, UsersService}

import cats.effect.IO
import cats.implicits.toSemigroupKOps
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io.*
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.typelevel.log4cats.SelfAwareStructuredLogger

final class UsersRestController(authService: AuthService, usersService: UsersService)(using
    logger: SelfAwareStructuredLogger[IO],
) extends BaseRestController(authService, usersService):
  val routes: HttpRoutes[IO] =
    val publicRoutes = HttpRoutes.of[IO]:
      case request @ POST -> Root / "users" =>
        (for
          newUser <- validatedInputFrom[RegisterNewUserRequest, UserWithPlaintextPassword](
            request,
          )
          user <- usersService.register(newUser)
          response <- Ok(RegisterNewUserResponse(user))
        yield response).handleErrorWith(contextFrom(request))

      case request @ POST -> Root / "users" / "login" =>
        (for
          credentials <- validatedInputFrom[LoginUserRequest, Credentials](request)
          user <- usersService.loginUserIdentifiedBy(credentials)
          response <- Ok(LoginUserResponse(user))
        yield response).handleErrorWith(contextFrom(request))

    val secureRoutes = AuthedRoutes.of[UserId, IO]:
      case request @ GET -> Root / "users" as userId =>
        (for
          user <- usersService.userFor(userId)
          response <- Ok(GetCurrentUserResponse(user))
        yield response).handleErrorWith(contextFrom(request.req))

      case request @ PUT -> Root / "users" as userId =>
        (for
          updatedUser <- validatedInputFrom[UpdateUserRequest, UserWithPlaintextPassword](
            request.req,
          )
          user <- usersService.update(updatedUser, userId)
          response <- Ok(UpdateUserResponse(user))
        yield response).handleErrorWith(contextFrom(request.req))

    publicRoutes <+> authMiddleware(secureRoutes)
