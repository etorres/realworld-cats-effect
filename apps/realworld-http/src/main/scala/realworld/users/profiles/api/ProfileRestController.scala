package es.eriktorr
package realworld.users.profiles.api

import realworld.common.api.BaseRestController
import realworld.users.core.domain.{UserId, UsersService}
import realworld.users.profiles.api.params.UsernameVar
import realworld.users.profiles.api.response.{
  FollowUserResponse,
  GetProfileResponse,
  UnfollowUserResponse,
}

import cats.effect.IO
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io.*
import org.typelevel.log4cats.SelfAwareStructuredLogger

final class ProfileRestController(usersService: UsersService)(using
    logger: SelfAwareStructuredLogger[IO],
) extends BaseRestController:
  override val optionalAuthRoutes: Option[AuthedRoutes[UserId, IO]] = Some(
    AuthedRoutes.of[UserId, IO]:
      case request @ GET -> Root / "profiles" / UsernameVar(username) as userId =>
        (for
          profile <- usersService.profileFor(username, userId)
          response <- Ok(GetProfileResponse(profile))
        yield response).handleErrorWith(contextFrom(request.req)),
  )

  override val secureRoutes: Option[AuthedRoutes[UserId, IO]] = Some(AuthedRoutes.of[UserId, IO]:
    case request @ DELETE -> Root / "profiles" / UsernameVar(username) / "follow" as userId =>
      (for
        profile <- usersService.unfollow(username, userId)
        response <- Ok(UnfollowUserResponse(profile))
      yield response).handleErrorWith(contextFrom(request.req))

    case request @ POST -> Root / "profiles" / UsernameVar(username) / "follow" as userId =>
      (for
        profile <- usersService.follow(username, userId)
        response <- Ok(FollowUserResponse(profile))
      yield response).handleErrorWith(contextFrom(request.req)),
  )
