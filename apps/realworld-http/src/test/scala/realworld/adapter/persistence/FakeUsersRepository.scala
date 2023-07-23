package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.FakeUsersRepository.UsersRepositoryState
import realworld.domain.model.{Email, UserWithPassword}
import realworld.domain.service.UsersRepository

import cats.effect.{IO, Ref}

final class FakeUsersRepository(stateRef: Ref[IO, UsersRepositoryState]) extends UsersRepository:
  override def findUserWithPasswordBy(email: Email): IO[Option[UserWithPassword]] =
    stateRef.get.map(_.users.get(email))

object FakeUsersRepository:
  final case class UsersRepositoryState(users: Map[Email, UserWithPassword])

  object UsersRepositoryState:
    def empty: UsersRepositoryState = UsersRepositoryState(Map.empty)
