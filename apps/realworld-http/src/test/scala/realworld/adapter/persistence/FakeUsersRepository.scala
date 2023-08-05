package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.FakeUsersRepository.UsersRepositoryState
import realworld.domain.model.{Email, NewUser, User, UserWithPassword}
import realworld.domain.service.UsersRepository

import cats.effect.{IO, Ref}

final class FakeUsersRepository(stateRef: Ref[IO, UsersRepositoryState]) extends UsersRepository:
  override def findUserBy(email: Email): IO[Option[User]] = for {
    maybeUserWithPassword <- findUserWithPasswordBy(email)
    user = maybeUserWithPassword.map(_.user)
  } yield user

  override def findUserWithPasswordBy(email: Email): IO[Option[UserWithPassword]] =
    stateRef.get.map(_.users.find(_.user.email == email))

  override def register(newUser: NewUser): IO[User] = for
    user <- IO.pure(User(newUser.email, None, newUser.username, None, None))
    userWithPassword = UserWithPassword(user, newUser.password)
    _ <- stateRef.update(currentState => currentState.copy(userWithPassword :: currentState.users))
  yield user

object FakeUsersRepository:
  final case class UsersRepositoryState(users: List[UserWithPassword])

  object UsersRepositoryState:
    def empty: UsersRepositoryState = UsersRepositoryState(List.empty)
