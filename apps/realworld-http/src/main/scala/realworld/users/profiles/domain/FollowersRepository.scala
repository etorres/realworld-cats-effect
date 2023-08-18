package es.eriktorr
package realworld.users.profiles.domain

import realworld.users.core.domain.UserId

import cats.effect.IO

trait FollowersRepository:
  def follow(followed: UserId, follower: UserId): IO[Unit]

  def isFollowing(followed: UserId, follower: UserId): IO[Boolean]

  def unfollow(followed: UserId, follower: UserId): IO[Unit]
