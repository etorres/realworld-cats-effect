package es.eriktorr
package realworld.application

import realworld.adapter.persistence.FakeUsersRepository
import realworld.adapter.persistence.FakeUsersRepository.UsersRepositoryState
import realworld.domain.model.User.Token
import realworld.domain.model.{Email, UserWithPassword}
import realworld.domain.service.FakeAuthService.AuthServiceState
import realworld.domain.service.{FakeAuthService, UsersService}
import realworld.shared.adapter.rest.FakeHealthService.HealthServiceState
import realworld.shared.adapter.rest.{FakeHealthService, FakeMetricsService, FakeTraceService}

import cats.effect.{IO, Ref}
import io.github.iltotore.iron.constraint.string.ValidUUID
import io.github.iltotore.iron.refineOption
import org.http4s.server.middleware.RequestId
import org.http4s.{EntityDecoder, Request, Status}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger

object RealWorldHttpAppSuiteRunner:
  final case class RealWorldHttpAppState(
      authServiceState: AuthServiceState,
      healthServiceState: HealthServiceState,
      usersRepositoryState: UsersRepositoryState,
  ):
    def setTokens(tokens: Map[Email, Token]): RealWorldHttpAppState = copy(
      authServiceState = authServiceState.setTokens(tokens),
    )
    def setUsersWithPassword(users: Map[Email, UserWithPassword]): RealWorldHttpAppState =
      copy(usersRepositoryState = usersRepositoryState.copy(users))

  object RealWorldHttpAppState:
    def empty: RealWorldHttpAppState = RealWorldHttpAppState(
      AuthServiceState.empty,
      HealthServiceState.unready,
      UsersRepositoryState.empty,
    )

  def runWith[A](initialState: RealWorldHttpAppState, request: Request[IO])(using
      entityDecoder: EntityDecoder[IO, A],
  ): IO[(Either[Throwable, (A, Status)], RealWorldHttpAppState)] = for
    authServiceStateRef <- Ref.of[IO, AuthServiceState](initialState.authServiceState)
    healthServiceStateRef <- Ref.of[IO, HealthServiceState](initialState.healthServiceState)
    usersRepositoryStateRef <- Ref.of[IO, UsersRepositoryState](
      initialState.usersRepositoryState,
    )
    authService = FakeAuthService(authServiceStateRef)
    healthService = FakeHealthService(healthServiceStateRef)
    metricsService = FakeMetricsService()
    traceService = FakeTraceService()
    usersRepository = FakeUsersRepository(usersRepositoryStateRef)
    usersService = UsersService(authService, usersRepository)
    httpApp =
      given SelfAwareStructuredLogger[IO] = NoOpLogger.impl[IO]
      RealWorldHttpApp(healthService, metricsService, traceService, usersService).httpApp
    result <- (for
      response <- httpApp.run(request)
      status = response.status
      body <- status match
        case Status.Ok => response.as[A]
        case other =>
          IO.raiseError(IllegalStateException(s"Unexpected response status: ${other.code}"))
      _ <- IO.fromOption(for
        requestId <- response.attributes.lookup(RequestId.requestIdAttrKey)
        _ <- requestId.refineOption[ValidUUID]
      yield ())(IllegalStateException("Request Id not found"))
    yield (body, status)).attempt
    finalAuthServiceState <- authServiceStateRef.get
    finalHealthServiceState <- healthServiceStateRef.get
    finalUsersRepositoryState <- usersRepositoryStateRef.get
    finalState = initialState.copy(
      authServiceState = finalAuthServiceState,
      healthServiceState = finalHealthServiceState,
      usersRepositoryState = finalUsersRepositoryState,
    )
    _ = result match
      case Left(error) => error.printStackTrace()
      case _ => ()
  yield (result, finalState)
