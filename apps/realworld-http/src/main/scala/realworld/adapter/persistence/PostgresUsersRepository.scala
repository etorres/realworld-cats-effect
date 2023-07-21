package es.eriktorr
package realworld.adapter.persistence

import realworld.domain.model.{Email, UserWithPassword}
import realworld.domain.service.UsersRepository

import cats.effect.IO

final class PostgresUsersRepository extends UsersRepository:
  override def findUserWithPasswordBy(email: Email): IO[UserWithPassword] = ???
