package es.eriktorr
package realworld.shared.adapter.rest

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.io.*

final class FakeMetricsService extends MetricsService:
  override def metricsFor(routes: HttpRoutes[IO]): HttpRoutes[IO] = routes

  def prometheusExportRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> Root =>
    Ok()
  }
