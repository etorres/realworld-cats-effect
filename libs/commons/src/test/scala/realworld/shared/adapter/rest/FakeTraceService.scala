package es.eriktorr
package realworld.shared.adapter.rest

import cats.effect.IO
import org.http4s.HttpRoutes

final class FakeTraceService extends TraceService:
  override def trace(routes: HttpRoutes[IO]): HttpRoutes[IO] = routes
