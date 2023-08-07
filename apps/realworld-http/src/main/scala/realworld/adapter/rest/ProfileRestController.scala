package es.eriktorr
package realworld.adapter.rest

import realworld.adapter.rest.params.UsernameVar
import realworld.domain.model.UserId
import realworld.domain.service.{AuthService, UsersService}

import cats.effect.IO
import org.http4s.dsl.io.*
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.typelevel.log4cats.SelfAwareStructuredLogger

final class ProfileRestController(authService: AuthService, usersService: UsersService)(using
    logger: SelfAwareStructuredLogger[IO],
) extends BaseRestController(authService, usersService):
  val routes: HttpRoutes[IO] =
    val secureRoutes = AuthedRoutes.of[UserId, IO]:
      case request @ GET -> Root / "profiles" / UsernameVar(username) as userId =>
        Ok().handleErrorWith(contextFrom(request.req))

    authMiddleware(secureRoutes)
