package es.eriktorr
package realworld.domain.service

import realworld.domain.model.Error.InvalidCredentials
import realworld.domain.model.{Credentials, Password, User}

import cats.effect.IO

final class UsersService(authService: AuthService, usersRepository: UsersRepository):
  def loginUserIdentifiedBy(credentials: Credentials): IO[User] = for
    userWithPassword <- usersRepository.findUserWithPasswordBy(credentials.email)
    user <-
      if Password.check(credentials.password, userWithPassword.password) then
        authService
          .tokenFor(userWithPassword.user.email)
          .map(token => userWithPassword.user.copy(token = token))
      else IO.raiseError(InvalidCredentials(credentials.email))
  yield user
