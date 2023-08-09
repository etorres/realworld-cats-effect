package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.mappers.UserIdDoobieMapper.userIdDoobieMapper
import realworld.adapter.persistence.row.FollowerRow
import realworld.domain.model.UserId
import realworld.domain.service.FollowersRepository

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.*

final class PostgresFollowersRepository(transactor: HikariTransactor[IO])
    extends FollowersRepository:
  override def follow(followed: UserId, follower: UserId): IO[Unit] =
    sql"""insert into followers (
         |  user_id, follower_id
         |) values (
         |  $followed,
         |  $follower
         |) on conflict (user_id, follower_id) do nothing""".stripMargin.update.run
      .transact(transactor)
      .void

  override def isFollowing(followed: UserId, follower: UserId): IO[Boolean] = for
    maybeFollowing <- sql"""select
                           |  user_id, follower_id
                           |from followers
                           |where user_id = $followed
                           |  and follower_id = $follower""".stripMargin
      .query[FollowerRow]
      .option
      .transact(transactor)
    following = maybeFollowing.nonEmpty
  yield following
