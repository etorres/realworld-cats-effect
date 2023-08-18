package es.eriktorr
package realworld.users.profiles.db

import realworld.users.core.db.mappers.UserIdDoobieMapper.userIdDoobieMapper
import realworld.users.core.domain.UserId
import realworld.users.profiles.db.row.FollowerRow
import realworld.users.profiles.domain.FollowersRepository

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.*

final class PostgresFollowersRepository(transactor: HikariTransactor[IO])
    extends FollowersRepository:
  override def follow(followed: UserId, follower: UserId): IO[Unit] =
    sql"""INSERT INTO followers (
         |  user_id,
         |  follower_id
         |) VALUES (
         |  $followed,
         |  $follower
         |) ON CONFLICT (user_id, follower_id) DO NOTHING""".stripMargin.update.run
      .transact(transactor)
      .void

  override def isFollowing(followed: UserId, follower: UserId): IO[Boolean] = for
    maybeFollowing <- sql"""SELECT user_id, follower_id
                           |FROM followers
                           |WHERE user_id = $followed
                           |AND follower_id = $follower""".stripMargin
      .query[FollowerRow]
      .option
      .transact(transactor)
    following = maybeFollowing.nonEmpty
  yield following

  override def unfollow(followed: UserId, follower: UserId): IO[Unit] =
    sql"""DELETE FROM followers
         |WHERE user_id = $followed
         |AND follower_id = $follower""".stripMargin.update.run
      .transact(transactor)
      .void
