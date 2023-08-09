package es.eriktorr
package realworld.domain.service

import realworld.domain.model.UserId

import cats.effect.IO

trait FollowersRepository:
  def follow(followed: UserId, follower: UserId): IO[Unit]

  def isFollowing(followed: UserId, follower: UserId): IO[Boolean]
