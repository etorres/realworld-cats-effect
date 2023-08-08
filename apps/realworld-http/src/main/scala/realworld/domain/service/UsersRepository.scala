package es.eriktorr
package realworld.domain.service

import realworld.domain.model.User.Username
import realworld.domain.model.UserWithPassword.UserWithHashPassword
import realworld.domain.model.{Email, User, UserId, UserWithPassword}
import realworld.shared.data.error.HandledError

import cats.effect.IO

trait UsersRepository:
  def create(newUser: UserWithHashPassword): IO[User]

  def findUserIdBy(email: Email): IO[Option[UserId]]

  def findUserBy(userId: UserId): IO[Option[User]]

  def findUserBy(username: Username): IO[Option[User]]

  def findUserWithPasswordBy(email: Email): IO[Option[UserWithHashPassword]]

  def update(updatedUser: UserWithHashPassword, userId: UserId): IO[User]

object UsersRepository:
  sealed abstract class UsersRepositoryError(message: String, cause: Option[Throwable])
      extends HandledError(message, cause)

  final case class AlreadyInUseError(constraint: String, cause: Throwable)
      extends UsersRepositoryError(s"Given data is already in use: $constraint", Some(cause))
