package es.eriktorr
package realworld.adapter.rest

import realworld.adapter.rest.request.{LoginUserRequest, RegisterNewUserRequest}
import realworld.adapter.rest.response.{LoginUserResponse, RegisterNewUserResponse}
import realworld.domain.model.{Credentials, NewUser}
import realworld.domain.service.UsersService

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io.*
import org.typelevel.log4cats.SelfAwareStructuredLogger

final class UsersRestController(usersService: UsersService)(using
    logger: SelfAwareStructuredLogger[IO],
) extends BaseRestController:
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO]:
    case request @ POST -> Root / "users" =>
      (for
        newUser <- validatedInputFrom[RegisterNewUserRequest, NewUser](request)
        user <- usersService.register(newUser)
        response <- Ok(RegisterNewUserResponse(user))
      yield response).handleErrorWith(contextFrom(request))

    case request @ POST -> Root / "users" / "login" =>
      (for
        credentials <- validatedInputFrom[LoginUserRequest, Credentials](request)
        user <- usersService.loginUserIdentifiedBy(credentials)
        response <- Ok(LoginUserResponse(user))
      yield response).handleErrorWith(contextFrom(request))
