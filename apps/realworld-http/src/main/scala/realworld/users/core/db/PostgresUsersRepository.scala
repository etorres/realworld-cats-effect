package es.eriktorr
package realworld.users.core.db

import realworld.common.Secret
import realworld.common.data.validated.ValidatedNecExtensions.validatedNecTo
import realworld.common.db.mappers.SecretDoobieMapper.secretDoobieMapper
import realworld.users.core.db.PostgresUsersRepository.given
import realworld.users.core.db.mappers.UserIdDoobieMapper.userIdDoobieMapper
import realworld.users.core.db.mappers.UsernameDoobieMapper.usernameDoobieMapper
import realworld.users.core.db.row.UserRow
import realworld.users.core.domain.Password.CipherText
import realworld.users.core.domain.User.Username
import realworld.users.core.domain.UserWithPassword.UserWithHashPassword
import realworld.users.core.domain.UsersRepository.AlreadyInUseError
import realworld.users.core.domain.{Email, Password, User, UserId, UsersRepository}

import cats.effect.IO
import cats.implicits.{catsSyntaxMonadError, toTraverseOps}
import doobie.Meta
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import org.postgresql.util.{PSQLException, PSQLState}

final class PostgresUsersRepository(transactor: HikariTransactor[IO]) extends UsersRepository:
  override def create(newUser: UserWithHashPassword): IO[User] = (for
    _ <- sql"""INSERT INTO USERS (
              |  email,
              |  username,
              |  password
              |) VALUES (
              |  ${newUser.user.email},
              |  ${newUser.user.username},
              |  ${newUser.password}
              |)""".stripMargin.update.run
      .transact(transactor)
      .void
    user = newUser.user
  yield user).adaptError {
    case error: PSQLException if isUniqueViolationError(error) =>
      val serverErrorMessage = error.getServerErrorMessage.nn
      AlreadyInUseError(serverErrorMessage.getConstraint.nn, error)
  }

  override def findUserBy(userId: UserId): IO[Option[User]] = for
    userRow <- sql"""SELECT
                    |  user_id,
                    |  email,
                    |  username,
                    |  password,
                    |  bio,
                    |  image
                    |FROM users
                    |WHERE user_id = $userId""".stripMargin
      .query[UserRow]
      .option
      .transact(transactor)
    userWithPassword <- userRow.traverse(
      _.toUserWithPassword[CipherText, UserWithHashPassword].validated,
    )
    user = userWithPassword.map(_.user)
  yield user

  override def findUserWithIdBy(username: Username): IO[Option[(User, UserId)]] = for
    maybeUserRow <- sql"""SELECT
                         |  user_id,
                         |  email,
                         |  username,
                         |  password,
                         |  bio,
                         |  image
                         |FROM users
                         |WHERE username = $username""".stripMargin
      .query[UserRow]
      .option
      .transact(transactor)
    userWithId <- maybeUserRow.traverse: userRow =>
      for
        userId <- UserId.from(userRow.userId).validated
        userWithPassword <- userRow.toUserWithPassword[CipherText, UserWithHashPassword].validated
      yield userWithPassword.user -> userId
  yield userWithId

  override def findUserIdBy(email: Email): IO[Option[UserId]] = for
    userRow <- findBy(email)
    userId <- userRow.traverse(x => UserId.from(x.userId).validated)
  yield userId

  override def findUserWithPasswordBy(email: Email): IO[Option[UserWithHashPassword]] = for
    userRow <- findBy(email)
    userWithPassword <- userRow.traverse(
      _.toUserWithPassword[CipherText, UserWithHashPassword].validated,
    )
  yield userWithPassword

  override def update(updatedUser: UserWithHashPassword, userId: UserId): IO[User] = for
    _ <- sql"""UPDATE USERS SET
              |  email = ${updatedUser.user.email},
              |  username = ${updatedUser.user.username},
              |  password = ${updatedUser.password},
              |  bio = ${updatedUser.user.bio},
              |  image = ${updatedUser.user.image.map(_.toString)}
              |WHERE user_id = $userId""".stripMargin.update.run
      .transact(transactor)
      .void
    user = updatedUser.user
  yield user

  private def findBy(email: Email): IO[Option[UserRow]] =
    sql"""SELECT
         |  user_id,
         |  email,
         |  username,
         |  password,
         |  bio,
         |  image
         |FROM users
         |WHERE email = $email""".stripMargin
      .query[UserRow]
      .option
      .transact(transactor)

  private def isUniqueViolationError(error: PSQLException) =
    val sqlState = error.getServerErrorMessage.nn.getSQLState
    PSQLState.UNIQUE_VIOLATION.getState == sqlState

object PostgresUsersRepository:
  given emailDoobieMapper: Meta[Email] = Meta[String].tiemap(Email.from(_).eitherMessage)(identity)

  given passwordDoobieMapper: Meta[Password[CipherText]] =
    Meta[Secret[String]]
      .tiemap(secret => Password.from[CipherText](secret.value).eitherMessage)(_.value)
