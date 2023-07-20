package es.eriktorr
package realworld.adapter.rest

import realworld.domain.service.UsersService

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.io.*
import org.typelevel.log4cats.SelfAwareStructuredLogger

final class UsersRestController(usersService: UsersService)(using
    logger: SelfAwareStructuredLogger[IO],
):
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO]:
    case request @ POST -> Root / "users" / "login" => ???

  assert(usersService == usersService) // TODO
  assert(logger == logger) // TODO

  // TODO
  // https://http4s.org/v0.23/docs/server-middleware.html#errorhandling
