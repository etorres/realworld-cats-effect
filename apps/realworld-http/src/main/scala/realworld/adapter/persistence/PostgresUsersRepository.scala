package es.eriktorr
package realworld.adapter.persistence

import realworld.domain.model.Password.CipherText
import realworld.domain.model.{Email, Password, User, UserWithPassword}
import realworld.domain.service.UsersRepository

import cats.effect.IO
import doobie.hikari.HikariTransactor

final class PostgresUsersRepository(transactor: HikariTransactor[IO]) extends UsersRepository:
  override def findUserWithPasswordBy(email: Email): IO[UserWithPassword] =
    assert(transactor == transactor) // TODO
    IO.pure(
      UserWithPassword(
        User(email, None, "username", "bio", None),
        Password.unsafeFrom[CipherText]("123"),
      ),
    ) // TODO
