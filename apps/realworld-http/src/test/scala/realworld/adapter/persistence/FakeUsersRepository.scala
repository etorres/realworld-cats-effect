package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.FakeUsersRepository.UsersRepositoryState
import realworld.domain.model.*
import realworld.domain.model.Password.CipherText
import realworld.domain.service.UsersRepository

import cats.effect.{IO, Ref}

final class FakeUsersRepository(stateRef: Ref[IO, UsersRepositoryState]) extends UsersRepository:
  override def findUserBy(email: Email): IO[Option[User]] = for
    maybeUserWithPassword <- findUserWithPasswordBy(email)
    user = maybeUserWithPassword.map(_.user)
  yield user

  override def findUserWithPasswordBy(email: Email): IO[Option[UserWithPassword[CipherText]]] =
    stateRef.get.map(_.users.find(_.user.email == email))

  override def register(newUser: UserWithPassword[CipherText]): IO[User] =
    stateRef
      .update(currentState => currentState.copy(newUser :: currentState.users))
      .map(_ => newUser.user)

object FakeUsersRepository:
  final case class UsersRepositoryState(users: List[UserWithPassword[CipherText]])

  object UsersRepositoryState:
    def empty: UsersRepositoryState = UsersRepositoryState(List.empty)
