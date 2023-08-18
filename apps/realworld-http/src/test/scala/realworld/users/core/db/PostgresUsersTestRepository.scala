package es.eriktorr
package realworld.users.core.db

import realworld.common.db.mappers.SecretDoobieMapper.secretDoobieMapper
import realworld.users.core.db.row.UserRow

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.*

final class PostgresUsersTestRepository(transactor: HikariTransactor[IO]):
  def add(row: UserRow): IO[Unit] =
    sql"""INSERT INTO users (
         |  user_id, email, username, password, bio, image
         |) VALUES (
         |  ${row.userId},
         |  ${row.email},
         |  ${row.username},
         |  ${row.password},
         |  ${row.bio},
         |  ${row.image}
         |)""".stripMargin.update.run.transact(transactor).void
