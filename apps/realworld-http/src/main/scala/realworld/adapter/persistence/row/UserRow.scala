package es.eriktorr
package realworld.adapter.persistence.row

import realworld.domain.model.Password.CipherText
import realworld.domain.model.User.{Token, Username}
import realworld.domain.model.{Email, Password, User, UserWithPassword}
import realworld.shared.data.refined.StringExtensions.toUri
import realworld.shared.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}

import cats.implicits.{catsSyntaxTuple4Semigroupal, toTraverseOps}
import doobie.Meta
import io.github.arainko.ducktape.*

final case class UserRow(
    userId: Int,
    email: String,
    username: String,
    password: String,
    bio: Option[String],
    image: Option[String],
)

object UserRow:
  given emailDoobieMapper: Meta[Email] = Meta[String].tiemap(Email.from(_).eitherMessage)(identity)

  extension (userRow: UserRow)
    def toUserWithPassword: AllErrorsOr[UserWithPassword] =
      (
        Email.from(userRow.email),
        Username.from(userRow.username),
        Password.from[CipherText](userRow.password),
        userRow.image.traverse(_.toUri),
      ).mapN { case (email, username, password, image) =>
        val user = userRow
          .into[User]
          .transform(
            Field.const(_.email, email),
            Field.const(_.token, Option.empty[Token]),
            Field.const(_.username, username),
            Field.const(_.image, image),
          )
        UserWithPassword(user, password)
      }
