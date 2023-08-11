package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.mappers.SecretDoobieMapper.secretDoobieMapper
import realworld.adapter.persistence.row.UserRow

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
