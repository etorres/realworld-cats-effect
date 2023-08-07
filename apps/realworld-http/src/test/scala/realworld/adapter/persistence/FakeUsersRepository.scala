package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.FakeUsersRepository.UsersRepositoryState
import realworld.domain.model.UserWithPassword.UserWithHashPassword
import realworld.domain.model.{Email, User, UserId}
import realworld.domain.service.UsersRepository

import cats.effect.{IO, Ref}

final class FakeUsersRepository(stateRef: Ref[IO, UsersRepositoryState]) extends UsersRepository:
  override def create(newUser: UserWithHashPassword): IO[User] = for
    maybeUser <- stateRef.tryModify(currentState =>
      (currentState.userIds.headOption match
        case Some(userId) => currentState.copy(currentState.users + (userId -> newUser))
        case None => currentState
      ) -> newUser.user,
    )
    user <- IO.fromOption(maybeUser)(IllegalArgumentException("User ids exhausted"))
  yield user

  override def findUserIdBy(email: Email): IO[Option[UserId]] = stateRef.get.map(
    _.users
      .find { case (_, userWithPassword) =>
        userWithPassword.user.email == email
      }
      .map { case (userId, _) => userId },
  )

  override def findUserWithPasswordBy(email: Email): IO[Option[UserWithHashPassword]] =
    stateRef.get.map(_.users.values.toList.find(_.user.email == email))

  override def update(updatedUser: UserWithHashPassword, userId: UserId): IO[User] = for
    maybeUser <- stateRef.tryModify(currentState =>
      (currentState.users.get(userId) match
        case Some(userWithPassword) =>
          currentState.copy(currentState.users + (userId -> updatedUser))
        case None => currentState
      ) -> updatedUser.user,
    )
    user <- IO.fromOption(maybeUser)(IllegalArgumentException(s"No user found with Id: $userId"))
  yield user

object FakeUsersRepository:
  final case class UsersRepositoryState(
      users: Map[UserId, UserWithHashPassword],
      userIds: List[UserId],
  )

  object UsersRepositoryState:
    def empty: UsersRepositoryState = UsersRepositoryState(Map.empty, List.empty)
