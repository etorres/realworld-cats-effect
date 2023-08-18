package es.eriktorr
package realworld.users.core.api

import realworld.common.api.BaseRestController
import realworld.users.core.api.request.{
  LoginUserRequest,
  RegisterNewUserRequest,
  UpdateUserRequest,
}
import realworld.users.core.api.response.{
  GetCurrentUserResponse,
  LoginUserResponse,
  RegisterNewUserResponse,
  UpdateUserResponse,
}
import realworld.users.core.domain.UserWithPassword.UserWithPlaintextPassword
import realworld.users.core.domain.{Credentials, UserId, UsersService}

import cats.effect.IO
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io.*
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.typelevel.log4cats.SelfAwareStructuredLogger

final class UsersRestController(usersService: UsersService)(using
    logger: SelfAwareStructuredLogger[IO],
) extends BaseRestController:
  override val publicRoutes: Option[HttpRoutes[IO]] = Some(HttpRoutes.of[IO]:
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
      yield response).handleErrorWith(contextFrom(request)),
  )

  override val secureRoutes: Option[AuthedRoutes[UserId, IO]] = Some(AuthedRoutes.of[UserId, IO]:
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
      yield response).handleErrorWith(contextFrom(request.req)),
  )
