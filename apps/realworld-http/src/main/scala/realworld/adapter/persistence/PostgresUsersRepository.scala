package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.row.UserRow
import realworld.adapter.persistence.row.UserRow.emailDoobieMapper
import realworld.domain.model.{Email, UserWithPassword}
import realworld.domain.service.UsersRepository
import realworld.shared.data.validated.ValidatedNecExtensions.validatedNecTo

import cats.effect.IO
import cats.implicits.toTraverseOps
import doobie.hikari.HikariTransactor
import doobie.implicits.*

final class PostgresUsersRepository(transactor: HikariTransactor[IO]) extends UsersRepository:
  override def findUserWithPasswordBy(email: Email): IO[Option[UserWithPassword]] = for
    userRow <- sql"""select
                    |  user_id, email, username, password, bio, image
                    |from users
                    |where email = $email""".stripMargin
      .query[UserRow]
      .option
      .transact(transactor)
    userWithPassword <- userRow.traverse(_.toUserWithPassword.validated)
  yield userWithPassword
