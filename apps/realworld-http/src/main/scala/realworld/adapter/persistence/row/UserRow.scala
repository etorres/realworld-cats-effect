package es.eriktorr
package realworld.adapter.persistence.row

import realworld.domain.model.Password.CipherText
import realworld.domain.model.User.{Token, Username}
import realworld.domain.model.{Email, Password, User, UserWithPassword}
import realworld.shared.Secret
import realworld.shared.data.refined.StringExtensions.toUri
import realworld.shared.data.validated.ValidatedNecExtensions.AllErrorsOr

import cats.implicits.{catsSyntaxTuple2Semigroupal, catsSyntaxTuple3Semigroupal, toTraverseOps}
import io.github.arainko.ducktape.*

final case class UserRow(
    userId: Int,
    email: String,
    username: String,
    password: Secret[String],
    bio: Option[String],
    image: Option[String],
)

object UserRow:
  extension (userRow: UserRow)
    def toUser: AllErrorsOr[User] =
      (Email.from(userRow.email), Username.from(userRow.username), userRow.image.traverse(_.toUri))
        .mapN { case (email, username, image) =>
          userRow
            .into[User]
            .transform(
              Field.const(_.email, email),
              Field.const(_.token, Option.empty[Token]),
              Field.const(_.username, username),
              Field.const(_.image, image),
            )
        }

    def toUserWithPassword: AllErrorsOr[UserWithPassword] =
      (toUser, Password.from[CipherText](userRow.password.value)).mapN(UserWithPassword.apply)
