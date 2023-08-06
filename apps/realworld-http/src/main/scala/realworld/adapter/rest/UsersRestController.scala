package es.eriktorr
package realworld.adapter.rest

import realworld.adapter.rest.JwtAuthMiddleware.jwtAuthMiddleware
import realworld.adapter.rest.request.{LoginUserRequest, RegisterNewUserRequest, UpdateUserRequest}
import realworld.adapter.rest.response.{
  GetCurrentUserResponse,
  LoginUserResponse,
  RegisterNewUserResponse,
  UpdateUserResponse,
}
import realworld.domain.model.Password.PlainText
import realworld.domain.model.{Credentials, User, UserWithPassword}
import realworld.domain.service.{AuthService, UsersService}

import cats.effect.IO
import cats.implicits.toSemigroupKOps
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io.*
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.typelevel.log4cats.SelfAwareStructuredLogger

final class UsersRestController(authService: AuthService, usersService: UsersService)(using
    logger: SelfAwareStructuredLogger[IO],
) extends BaseRestController:
  val routes: HttpRoutes[IO] =
    val publicRoutes: HttpRoutes[IO] = HttpRoutes.of[IO]:
      case request @ POST -> Root / "users" =>
        (for
          newUser <- validatedInputFrom[RegisterNewUserRequest, UserWithPassword[PlainText]](
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

    val secureRoutes = AuthedRoutes.of[User, IO]:
      case request @ GET -> Root / "users" as user =>
        Ok(GetCurrentUserResponse(user)).handleErrorWith(contextFrom(request.req))

      case request @ PUT -> Root / "users" as user =>
        (for
          updatedUser <- validatedInputFrom[UpdateUserRequest, UserWithPassword[PlainText]](
            request.req,
          )
          user <- usersService.update(updatedUser)
          response <- Ok(UpdateUserResponse(user))
        yield response).handleErrorWith(contextFrom(request.req))

    publicRoutes <+> jwtAuthMiddleware[User](token =>
      for
        email <- authService.verify(token)
        user <- usersService.findBy(email)
      yield user,
    )(secureRoutes)
