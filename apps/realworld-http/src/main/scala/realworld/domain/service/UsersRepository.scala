package es.eriktorr
package realworld.domain.service

import realworld.domain.model.Password.CipherText
import realworld.domain.model.{Email, User, UserWithPassword}
import realworld.shared.data.error.HandledError

import cats.effect.IO

trait UsersRepository:
  def create(newUser: UserWithPassword[CipherText]): IO[User]

  def findUserBy(email: Email): IO[Option[User]]

  def findUserWithPasswordBy(email: Email): IO[Option[UserWithPassword[CipherText]]]

  def update(updatedUser: UserWithPassword[CipherText]): IO[User]

object UsersRepository:
  sealed abstract class UsersRepositoryError(message: String, cause: Option[Throwable])
      extends HandledError(message, cause)

  final case class AlreadyInUseError(constraint: String, cause: Throwable)
      extends UsersRepositoryError(s"Given data is already in use: $constraint", Some(cause))
