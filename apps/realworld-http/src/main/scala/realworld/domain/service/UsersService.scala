package es.eriktorr
package realworld.domain.service

import realworld.domain.model.{Credentials, Email, NewUser, Password, User}
import realworld.domain.service.UsersService.AccessForbidden
import realworld.shared.data.error.HandledError

import cats.effect.IO

final class UsersService(authService: AuthService, usersRepository: UsersRepository):
  def loginUserIdentifiedBy(credentials: Credentials): IO[User] = for
    maybeUserWithPassword <- usersRepository.findUserWithPasswordBy(credentials.email)
    userWithPassword <- IO.fromOption(maybeUserWithPassword)(AccessForbidden(credentials.email))
    user <-
      if Password.check(credentials.password, userWithPassword.password) then
        authService
          .tokenFor(userWithPassword.user.email)
          .map(token => userWithPassword.user.copy(token = Some(token)))
      else IO.raiseError(AccessForbidden(credentials.email))
  yield user

  def register(newUser: NewUser): IO[User] = ???

object UsersService:
  sealed abstract class UsersServiceError(message: String) extends HandledError(message)

  final case class AccessForbidden(email: Email)
      extends UsersServiceError(s"Access forbidden for user identified by: $email")
