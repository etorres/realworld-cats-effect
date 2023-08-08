package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.row.FollowerRow

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.*

final class PostgresFollowersTestRepository(transactor: HikariTransactor[IO]):
  def add(row: FollowerRow): IO[Unit] =
    sql"""insert into followers (
         |  user_id, follower_id
         |) values (
         |  ${row.userId},
         |  ${row.followerId}
         |)""".stripMargin.update.run.transact(transactor).void