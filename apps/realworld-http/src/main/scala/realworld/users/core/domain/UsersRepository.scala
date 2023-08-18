package es.eriktorr
package realworld.users.core.domain

import realworld.common.data.error.HandledError
import realworld.users.core.domain.User.Username
import realworld.users.core.domain.UserWithPassword.UserWithHashPassword
import realworld.users.core.domain.{Email, UserId}

import cats.effect.IO

trait UsersRepository:
  def create(newUser: UserWithHashPassword): IO[User]

  def findUserIdBy(email: Email): IO[Option[UserId]]

  def findUserBy(userId: UserId): IO[Option[User]]

  def findUserWithIdBy(username: Username): IO[Option[(User, UserId)]]

  def findUserWithPasswordBy(email: Email): IO[Option[UserWithHashPassword]]

  def update(updatedUser: UserWithHashPassword, userId: UserId): IO[User]

object UsersRepository:
  sealed abstract class UsersRepositoryError(message: String, cause: Option[Throwable])
      extends HandledError(message, cause)

  final case class AlreadyInUseError(constraint: String, cause: Throwable)
      extends UsersRepositoryError(s"Given data is already in use: $constraint", Some(cause))
