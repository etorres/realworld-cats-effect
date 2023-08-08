package es.eriktorr
package realworld.domain.service

import realworld.domain.model.UserId

import cats.effect.IO

trait FollowersRepository:
  def isFollowing(followed: UserId, follower: UserId): IO[Boolean]
