package es.eriktorr
package realworld.adapter.persistence.row

import realworld.domain.model.Password.Format
import realworld.domain.model.User.Username
import realworld.domain.model.{Email, Password, User, UserWithPassword}
import realworld.shared.Secret
import realworld.shared.data.refined.StringExtensions.toUri
import realworld.shared.data.validated.ValidatedNecExtensions.AllErrorsOr

import cats.Semigroup
import cats.data.NonEmptyChain
import cats.data.Validated.{Invalid, Valid}
import cats.implicits.{catsSyntaxTuple3Semigroupal, toTraverseOps}
import org.tpolecat.typename.TypeName

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
    private def toUser: AllErrorsOr[User] =
      (Email.from(userRow.email), Username.from(userRow.username), userRow.image.traverse(_.toUri))
        .mapN { case (email, username, image) => User(email, None, username, userRow.bio, image) }

    def toUserWithPassword[A <: Format, B <: UserWithPassword[A]](using
        typeNameA: TypeName[A],
    ): AllErrorsOr[B] =
      (toUser, Password.from[A](userRow.password.value)) match
        case (Valid(user), Valid(password)) => UserWithPassword.from[A, B](user, password)
        case (Valid(_), error @ Invalid(_)) => error
        case (error @ Invalid(_), Valid(_)) => error
        case (Invalid(userError), Invalid(passwordError)) =>
          Invalid(Semigroup[NonEmptyChain[String]].combine(userError, passwordError))
