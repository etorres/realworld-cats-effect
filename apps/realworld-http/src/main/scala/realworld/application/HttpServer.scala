package es.eriktorr
package realworld.application

import realworld.shared.application.HttpServerConfig

import cats.effect.{IO, Resource}
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server

object HttpServer:
  def impl(httpApp: HttpApp[IO], httpServerConfig: HttpServerConfig): Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      .withHost(httpServerConfig.host)
      .withPort(httpServerConfig.port)
      .withHttpApp(httpApp)
      .build
