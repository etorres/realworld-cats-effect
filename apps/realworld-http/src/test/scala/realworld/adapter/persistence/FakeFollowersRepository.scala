package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.FakeFollowersRepository.FollowersRepositoryState
import realworld.domain.model.UserId
import realworld.domain.service.FollowersRepository

import cats.effect.{IO, Ref}

final class FakeFollowersRepository(stateRef: Ref[IO, FollowersRepositoryState])
    extends FollowersRepository:
  override def follow(followed: UserId, follower: UserId): IO[Unit] = stateRef.update:
    currentState =>
      currentState.copy(
        currentState.followers + (followed -> (follower :: currentState.followers
          .getOrElse(followed, List.empty)
          .filterNot(_ == follower))),
      )

  override def isFollowing(followed: UserId, follower: UserId): IO[Boolean] =
    stateRef.get.map(_.followers.get(followed).exists(_.contains(follower)))

  override def unfollow(followed: UserId, follower: UserId): IO[Unit] = stateRef.update:
    currentState =>
      currentState.copy(currentState.followers.get(followed) match
        case Some(value) => currentState.followers + (followed -> value.filterNot(_ == follower))
        case None => currentState.followers,
      )

object FakeFollowersRepository:
  final case class FollowersRepositoryState(followers: Map[UserId, List[UserId]]):
    def setFollowers(newFollowers: Map[UserId, List[UserId]]): FollowersRepositoryState = copy(
      newFollowers,
    )

  object FollowersRepositoryState:
    val empty: FollowersRepositoryState = FollowersRepositoryState(Map.empty)
