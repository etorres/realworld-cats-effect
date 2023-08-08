package es.eriktorr
package realworld.application

import realworld.adapter.persistence.FakeUsersRepository
import realworld.adapter.persistence.FakeUsersRepository.UsersRepositoryState
import realworld.domain.model.Password.{CipherText, PlainText}
import realworld.domain.model.User.Token
import realworld.domain.model.UserWithPassword.UserWithHashPassword
import realworld.domain.model.{Email, Password, UserId}
import realworld.domain.service.FakeAuthService.AuthServiceState
import realworld.domain.service.FakeCipherService.CipherServiceState
import realworld.domain.service.{FakeAuthService, FakeCipherService, UsersService}
import realworld.shared.adapter.rest.FakeHealthService.HealthServiceState
import realworld.shared.adapter.rest.{FakeHealthService, FakeMetricsService, FakeTraceService}

import cats.effect.{IO, Ref}
import io.github.iltotore.iron.constraint.string.ValidUUID
import io.github.iltotore.iron.refineOption
import org.http4s.server.middleware.RequestId
import org.http4s.{EntityDecoder, Request, Status}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object RealWorldHttpAppSuiteRunner:
  final case class RealWorldHttpAppState(
      authServiceState: AuthServiceState,
      cipherServiceState: CipherServiceState,
      healthServiceState: HealthServiceState,
      usersRepositoryState: UsersRepositoryState,
  ):
    def setTokens(tokens: Map[Email, Token]): RealWorldHttpAppState = copy(
      authServiceState = authServiceState.setTokens(tokens),
    )
    def setPasswords(
        passwords: Map[Password[PlainText], Password[CipherText]],
    ): RealWorldHttpAppState = copy(cipherServiceState = cipherServiceState.setPasswords(passwords))
    def setUsersWithPassword(
        userIds: List[UserId],
        users: Map[UserId, UserWithHashPassword],
    ): RealWorldHttpAppState =
      copy(usersRepositoryState = usersRepositoryState.setUsers(userIds, users))

  object RealWorldHttpAppState:
    def empty: RealWorldHttpAppState = RealWorldHttpAppState(
      AuthServiceState.empty,
      CipherServiceState.empty,
      HealthServiceState.unready,
      UsersRepositoryState.empty,
    )

  def runWith[A](initialState: RealWorldHttpAppState, request: Request[IO])(using
      entityDecoder: EntityDecoder[IO, A],
  ): IO[(Either[Throwable, (A, Status)], RealWorldHttpAppState)] = for
    authServiceStateRef <- Ref.of[IO, AuthServiceState](initialState.authServiceState)
    cipherServiceStateRef <- Ref.of[IO, CipherServiceState](initialState.cipherServiceState)
    healthServiceStateRef <- Ref.of[IO, HealthServiceState](initialState.healthServiceState)
    usersRepositoryStateRef <- Ref.of[IO, UsersRepositoryState](
      initialState.usersRepositoryState,
    )
    authService = FakeAuthService(authServiceStateRef)
    cipherService = FakeCipherService(cipherServiceStateRef)
    healthService = FakeHealthService(healthServiceStateRef)
    metricsService = FakeMetricsService()
    traceService = FakeTraceService()
    usersRepository = FakeUsersRepository(usersRepositoryStateRef)
    usersService = UsersService(authService, cipherService, usersRepository)
    httpApp =
      given SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]
      RealWorldHttpApp(
        authService,
        healthService,
        metricsService,
        traceService,
        usersService,
      ).httpApp
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
    finalCipherServiceState <- cipherServiceStateRef.get
    finalHealthServiceState <- healthServiceStateRef.get
    finalUsersRepositoryState <- usersRepositoryStateRef.get
    finalState = initialState.copy(
      authServiceState = finalAuthServiceState,
      cipherServiceState = finalCipherServiceState,
      healthServiceState = finalHealthServiceState,
      usersRepositoryState = finalUsersRepositoryState,
    )
    _ = result match
      case Left(error) => error.printStackTrace()
      case _ => ()
  yield (result, finalState)
