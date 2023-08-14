package es.eriktorr
package realworld.adapter.rest

import realworld.adapter.rest.params.UsernameVar
import realworld.adapter.rest.response.{
  FollowUserResponse,
  GetProfileResponse,
  UnfollowUserResponse,
}
import realworld.domain.model.UserId
import realworld.domain.service.UsersService

import cats.effect.IO
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io.*
import org.typelevel.log4cats.SelfAwareStructuredLogger

final class ProfileRestController(usersService: UsersService)(using
    logger: SelfAwareStructuredLogger[IO],
) extends BaseRestController:
  override val secureRoutes: Option[AuthedRoutes[UserId, IO]] = Some(AuthedRoutes.of[UserId, IO]:
    case request @ GET -> Root / "profiles" / UsernameVar(username) as userId =>
      (for
        profile <- usersService.profileFor(username, userId)
        response <- Ok(GetProfileResponse(profile))
      yield response).handleErrorWith(contextFrom(request.req))

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
