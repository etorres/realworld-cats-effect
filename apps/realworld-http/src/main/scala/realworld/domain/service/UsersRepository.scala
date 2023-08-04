package es.eriktorr
package realworld.domain.service

import realworld.domain.model.{Email, NewUser, User, UserWithPassword}
import realworld.shared.data.error.HandledError

import cats.effect.IO

trait UsersRepository:
  def findUserWithPasswordBy(email: Email): IO[Option[UserWithPassword]]

  def register(newUser: NewUser): IO[User]

object UsersRepository:
  sealed abstract class UsersRepositoryError(message: String, cause: Option[Throwable])
      extends HandledError(message, cause)

  final case class AlreadyInUseError(constraint: String, cause: Throwable)
      extends UsersRepositoryError(s"Given data is already in use: $constraint", Some(cause))
