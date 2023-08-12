package es.eriktorr
package realworld.application

import realworld.adapter.persistence.FakeArticlesRepository.ArticlesRepositoryState
import realworld.adapter.persistence.FakeFollowersRepository.FollowersRepositoryState
import realworld.adapter.persistence.FakeUsersRepository.UsersRepositoryState
import realworld.adapter.persistence.{
  FakeArticlesRepository,
  FakeFollowersRepository,
  FakeUsersRepository,
}
import realworld.domain.model.Password.{CipherText, PlainText}
import realworld.domain.model.User.Token
import realworld.domain.model.UserWithPassword.UserWithHashPassword
import realworld.domain.model.{Article, Email, Password, UserId}
import realworld.domain.service.FakeAuthService.AuthServiceState
import realworld.domain.service.FakeCipherService.CipherServiceState
import realworld.domain.service.*
import realworld.shared.adapter.rest.FakeHealthService.HealthServiceState
import realworld.shared.adapter.rest.{FakeHealthService, FakeMetricsService, FakeTraceService}

import cats.effect.{IO, Ref}
import io.github.iltotore.iron.constraint.string.ValidUUID
import io.github.iltotore.iron.refineOption
import org.http4s.server.middleware.RequestId
import org.http4s.{EntityDecoder, Request, Status}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object HttpAppSuiteRunner:
  final case class HttpAppState(
      articlesRepositoryState: ArticlesRepositoryState,
      authServiceState: AuthServiceState,
      cipherServiceState: CipherServiceState,
      followersRepositoryState: FollowersRepositoryState,
      healthServiceState: HealthServiceState,
      usersRepositoryState: UsersRepositoryState,
  ):
    def setArticles(articles: Map[(ArticlesFilters, Pagination), List[Article]]): HttpAppState =
      copy(articlesRepositoryState = articlesRepositoryState.setArticles(articles))
    def setFollowers(followers: Map[UserId, List[UserId]]): HttpAppState =
      copy(followersRepositoryState = followersRepositoryState.setFollowers(followers))
    def setPasswords(
        passwords: Map[Password[PlainText], Password[CipherText]],
    ): HttpAppState = copy(cipherServiceState = cipherServiceState.setPasswords(passwords))
    def setTokens(tokens: Map[Email, Token]): HttpAppState = copy(
      authServiceState = authServiceState.setTokens(tokens),
    )
    def setUsersWithPassword(
        userIds: List[UserId],
        users: Map[UserId, UserWithHashPassword],
    ): HttpAppState =
      copy(usersRepositoryState = usersRepositoryState.setUsers(userIds, users))

  object HttpAppState:
    def empty: HttpAppState = HttpAppState(
      ArticlesRepositoryState.empty,
      AuthServiceState.empty,
      CipherServiceState.empty,
      FollowersRepositoryState.empty,
      HealthServiceState.unready,
      UsersRepositoryState.empty,
    )

  def runWith[A](initialState: HttpAppState, request: Request[IO])(using
      entityDecoder: EntityDecoder[IO, A],
  ): IO[(Either[Throwable, (A, Status)], HttpAppState)] = for
    articlesRepositoryStateRef <- Ref.of[IO, ArticlesRepositoryState](
      initialState.articlesRepositoryState,
    )
    authServiceStateRef <- Ref.of[IO, AuthServiceState](initialState.authServiceState)
    cipherServiceStateRef <- Ref.of[IO, CipherServiceState](initialState.cipherServiceState)
    followersRepositoryStateRef <- Ref.of[IO, FollowersRepositoryState](
      initialState.followersRepositoryState,
    )
    healthServiceStateRef <- Ref.of[IO, HealthServiceState](initialState.healthServiceState)
    usersRepositoryStateRef <- Ref.of[IO, UsersRepositoryState](
      initialState.usersRepositoryState,
    )
    articlesRepository = FakeArticlesRepository(articlesRepositoryStateRef)
    articlesService = ArticlesService(articlesRepository)
    authService = FakeAuthService(authServiceStateRef)
    cipherService = FakeCipherService(cipherServiceStateRef)
    followersRepository = FakeFollowersRepository(followersRepositoryStateRef)
    healthService = FakeHealthService(healthServiceStateRef)
    metricsService = FakeMetricsService()
    traceService = FakeTraceService()
    usersRepository = FakeUsersRepository(usersRepositoryStateRef)
    usersService = UsersService(authService, cipherService, followersRepository, usersRepository)
    httpApp =
      given SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]
      RealWorldHttpApp(
        articlesService,
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
    finalArticlesRepositoryState <- articlesRepositoryStateRef.get
    finalAuthServiceState <- authServiceStateRef.get
    finalCipherServiceState <- cipherServiceStateRef.get
    finalFollowersRepositoryState <- followersRepositoryStateRef.get
    finalHealthServiceState <- healthServiceStateRef.get
    finalUsersRepositoryState <- usersRepositoryStateRef.get
    finalState = initialState.copy(
      articlesRepositoryState = finalArticlesRepositoryState,
      authServiceState = finalAuthServiceState,
      cipherServiceState = finalCipherServiceState,
      followersRepositoryState = finalFollowersRepositoryState,
      healthServiceState = finalHealthServiceState,
      usersRepositoryState = finalUsersRepositoryState,
    )
    _ = result match
      case Left(error) => error.printStackTrace()
      case _ => ()
  yield (result, finalState)
