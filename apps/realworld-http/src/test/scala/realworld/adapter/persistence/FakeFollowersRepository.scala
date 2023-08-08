package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.FakeFollowersRepository.FollowersRepositoryState
import realworld.domain.model.UserId
import realworld.domain.service.FollowersRepository

import cats.effect.{IO, Ref}

final class FakeFollowersRepository(stateRef: Ref[IO, FollowersRepositoryState])
    extends FollowersRepository:
  override def isFollowing(followed: UserId, follower: UserId): IO[Boolean] =
    stateRef.get.map(_.followers.get(followed).map(_.contains(follower)).nonEmpty)

object FakeFollowersRepository:
  final case class FollowersRepositoryState(followers: Map[UserId, List[UserId]]):
    def setFollowers(newFollowers: Map[UserId, List[UserId]]): FollowersRepositoryState = copy(
      newFollowers,
    )

  object FollowersRepositoryState:
    val empty: FollowersRepositoryState = FollowersRepositoryState(Map.empty)
