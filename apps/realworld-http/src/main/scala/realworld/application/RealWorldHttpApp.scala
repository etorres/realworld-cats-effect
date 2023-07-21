package es.eriktorr
package realworld.application

import cats.data.OptionT
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.io.*
import org.http4s.server.middleware.ErrorHandling

final class RealWorldHttpApp:
  private val routes: HttpRoutes[IO] = ???
  ErrorHandling.Custom.recoverWith(routes) { error =>
    OptionT.liftF(InternalServerError())
  }
