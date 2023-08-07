package es.eriktorr
package realworld.domain.service

import realworld.domain.model.*
import realworld.domain.model.UserWithPassword.UserWithPlaintextPassword
import realworld.domain.service.UsersService.{AccessForbidden, UserNotFound}
import realworld.shared.data.error.HandledError

import cats.effect.IO

final class UsersService(
    authService: AuthService,
    cipherService: CipherService,
    usersRepository: UsersRepository,
):
  def loginUserIdentifiedBy(credentials: Credentials): IO[User] = for
    maybeUserWithPassword <- usersRepository.findUserWithPasswordBy(credentials.email)
    userWithPassword <- IO.fromOption(maybeUserWithPassword)(AccessForbidden(credentials.email))
    user <- cipherService
      .check(credentials.password, userWithPassword.password)
      .ifM(
        ifTrue = authService
          .tokenFor(userWithPassword.user.email)
          .map(token => userWithPassword.user.copy(token = Some(token))),
        ifFalse = IO.raiseError(AccessForbidden(credentials.email)),
      )
  yield user

  def profileFor(userId: UserId): IO[Profile] =
    IO.raiseError(IllegalArgumentException("Not implemented")) // TODO

  def register(newUser: UserWithPlaintextPassword): IO[User] = for
    hash <- cipherService.cipher(newUser.password)
    user <- usersRepository.create(newUser.withHash(hash))
  yield user

  def update(updatedUser: UserWithPlaintextPassword, userId: UserId): IO[User] = for
    hash <- cipherService.cipher(updatedUser.password)
    user <- usersRepository.update(updatedUser.withHash(hash), userId)
    _ <- IO.raiseError(IllegalArgumentException(s"Not implemented $userId")) // TODO
  yield user

  def userIdFor(email: Email): IO[UserId] = for
    maybeUserId <- usersRepository.findUserIdBy(email)
    userId <- IO.fromOption(maybeUserId)(UserNotFound(email))
  yield userId

object UsersService:
  sealed abstract class UsersServiceError(message: String) extends HandledError(message)

  final case class AccessForbidden(email: Email)
      extends UsersServiceError(s"Access forbidden for user identified by: $email")

  final case class UserNotFound(email: Email)
      extends UsersServiceError(s"No user found with email: $email")
