package es.eriktorr
package realworld.users.core.db

import realworld.users.core.db.FakeUsersRepository.UsersRepositoryState
import realworld.users.core.domain.User.Username
import realworld.users.core.domain.UserWithPassword.UserWithHashPassword
import realworld.users.core.domain.{Email, User, UserId, UsersRepository}

import cats.effect.{IO, Ref}

final class FakeUsersRepository(stateRef: Ref[IO, UsersRepositoryState]) extends UsersRepository:
  override def create(newUser: UserWithHashPassword): IO[User] = for
    maybeUser <- stateRef.tryModify(currentState =>
      (currentState.userIds match
        case ::(head, next) =>
          currentState.copy(
            userIds = next,
            users = currentState.users + (head -> newUser),
          )
        case Nil => currentState
      ) -> newUser.user,
    )
    user <- IO.fromOption(maybeUser)(IllegalArgumentException("User ids exhausted"))
  yield user

  override def findUserBy(userId: UserId): IO[Option[User]] =
    stateRef.get.map(_.users.get(userId).map(_.user))

  override def findUserWithIdBy(username: Username): IO[Option[(User, UserId)]] = stateRef.get.map(
    _.users
      .find { case (_, userWithPassword) =>
        userWithPassword.user.username == username
      }
      .map { case (userId, userWithPassword) => userWithPassword.user -> userId },
  )

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
          currentState.copy(users = currentState.users + (userId -> updatedUser))
        case None => currentState
      ) -> updatedUser.user,
    )
    user <- IO.fromOption(maybeUser)(IllegalArgumentException(s"No user found with Id: $userId"))
  yield user

object FakeUsersRepository:
  final case class UsersRepositoryState(
      userIds: List[UserId],
      users: Map[UserId, UserWithHashPassword],
  ):
    def setUsers(
        newUserIds: List[UserId],
        newUsers: Map[UserId, UserWithHashPassword],
    ): UsersRepositoryState = copy(newUserIds, newUsers)

  object UsersRepositoryState:
    def empty: UsersRepositoryState = UsersRepositoryState(List.empty, Map.empty)
