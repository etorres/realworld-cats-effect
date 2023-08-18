package es.eriktorr
package realworld.common.api

import cats.effect.{IO, IOLocal, Resource}
import org.http4s.HttpRoutes
import trace4cats.*
import trace4cats.context.Provide
import trace4cats.context.iolocal.instances.ioLocalProvide
import trace4cats.http4s.server.syntax.TracedRoutes
import trace4cats.log.LogSpanCompleter

trait TraceService:
  def trace(routes: HttpRoutes[IO]): HttpRoutes[IO]

object TraceService:
  def resourceWith(serviceName: ServiceName): Resource[IO, TraceService] = (for
    completer <- Resource.eval(LogSpanCompleter.create[IO](TraceProcess(serviceName)))
    sampler = SpanSampler.probabilistic[IO](0.01)
    entryPoint = EntryPoint[IO](sampler, completer)
    span <- entryPoint.root("Root", SpanKind.Internal)
    ioLocal <- Resource.eval(IOLocal(span))
  yield (entryPoint, ioLocal)).map: (entryPoint, ioLocal) =>
    given Provide[IO, IO, Span[IO]] = ioLocalProvide(ioLocal)
    (routes: HttpRoutes[IO]) => routes.inject(entryPoint)
