package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.PostgresUsersRepository.given
import realworld.adapter.persistence.SecretDoobieMapper.secretDoobieMapper
import realworld.adapter.persistence.row.UserRow
import realworld.domain.model.Password.CipherText
import realworld.domain.model.User.Username
import realworld.domain.model.UserWithPassword.UserWithHashPassword
import realworld.domain.model.{Email, Password, User}
import realworld.domain.service.UsersRepository
import realworld.domain.service.UsersRepository.AlreadyInUseError
import realworld.shared.Secret
import realworld.shared.data.validated.ValidatedNecExtensions.validatedNecTo

import cats.effect.IO
import cats.implicits.{catsSyntaxMonadError, toTraverseOps}
import doobie.Meta
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import org.postgresql.util.{PSQLException, PSQLState}

final class PostgresUsersRepository(transactor: HikariTransactor[IO]) extends UsersRepository:
  override def create(newUser: UserWithHashPassword): IO[User] = (for
    _ <- sql"""insert into users (
              |  email, username, password
              |) values (
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

  override def findUserBy(email: Email): IO[Option[User]] = for
    userRow <- findBy(email)
    user <- userRow.traverse(_.toUser.validated)
  yield user

  override def findUserWithPasswordBy(email: Email): IO[Option[UserWithHashPassword]] = for
    userRow <- findBy(email)
    userWithPassword <- userRow.traverse(
      _.toUserWithPassword[CipherText, UserWithHashPassword].validated,
    )
  yield userWithPassword

  override def update(updatedUser: UserWithHashPassword): IO[User] = for
    _ <- sql"""update users set
              |  username = ${updatedUser.user.username},
              |  password = ${updatedUser.password},
              |  bio = ${updatedUser.user.bio},
              |  image = ${updatedUser.user.image.map(_.toString)}
              |where email = ${updatedUser.user.email}""".stripMargin.update.run
      .transact(transactor)
      .void
    user = updatedUser.user
  yield user

  private def findBy(email: Email): IO[Option[UserRow]] =
    sql"""select
         |  user_id, email, username, password, bio, image
         |from users
         |where email = $email""".stripMargin
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

  given usernameDoobieMapper: Meta[Username] =
    Meta[String].tiemap(Username.from(_).eitherMessage)(identity)
