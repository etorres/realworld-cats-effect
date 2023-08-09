package es.eriktorr
package realworld.domain.service

import realworld.domain.model.*
import realworld.domain.model.User.Username
import realworld.domain.model.UserWithPassword.UserWithPlaintextPassword
import realworld.domain.service.UsersService.{AccessForbidden, UserNotFound}
import realworld.shared.data.error.HandledError

import cats.effect.IO

final class UsersService(
    authService: AuthService,
    cipherService: CipherService,
    followersRepository: FollowersRepository,
    usersRepository: UsersRepository,
):
  def follow(username: Username, followerId: UserId): IO[Profile] = for
    maybeUserWithId <- usersRepository.findUserWithIdBy(username)
    (followedUser, followedId) <- IO.fromOption(maybeUserWithId)(UserNotFound("username", username))
    _ <- followersRepository.follow(followedId, followerId)
  yield Profile(followedUser.username, followedUser.bio, followedUser.image, true)

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

  def profileFor(username: Username, userId: UserId): IO[Profile] = for
    maybeUserWithId <- usersRepository.findUserWithIdBy(username)
    (followedUser, followedId) <- IO.fromOption(maybeUserWithId)(UserNotFound("username", username))
    following <- followersRepository.isFollowing(followedId, userId)
  yield Profile(followedUser.username, followedUser.bio, followedUser.image, following)

  def register(newUser: UserWithPlaintextPassword): IO[User] = for
    hash <- cipherService.cipher(newUser.password)
    user <- usersRepository.create(newUser.withHash(hash))
  yield user

  def update(updatedUser: UserWithPlaintextPassword, userId: UserId): IO[User] = for
    hash <- cipherService.cipher(updatedUser.password)
    user <- usersRepository.update(updatedUser.withHash(hash), userId)
  yield user

  def userFor(userId: UserId): IO[User] = for
    maybeUser <- usersRepository.findUserBy(userId)
    user <- IO.fromOption(maybeUser)(UserNotFound("Id", userId))
  yield user

  def userIdFor(email: Email): IO[UserId] = for
    maybeUserId <- usersRepository.findUserIdBy(email)
    userId <- IO.fromOption(maybeUserId)(UserNotFound("email", email))
  yield userId

object UsersService:
  sealed abstract class UsersServiceError(message: String) extends HandledError(message)

  final case class AccessForbidden(email: Email)
      extends UsersServiceError(s"Access forbidden for user identified by: $email")

  final case class UserNotFound[A](name: String, value: A)
      extends UsersServiceError(s"No user found with $name: ${value.toString}")
