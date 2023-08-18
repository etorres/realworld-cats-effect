package es.eriktorr
package realworld.users.core.db.row

import realworld.common.Secret
import realworld.common.data.refined.StringExtensions.toUri
import realworld.common.data.validated.ValidatedNecExtensions.AllErrorsOr
import realworld.users.core.domain.Password.Format
import realworld.users.core.domain.User.Username
import realworld.users.core.domain.{Email, Password, User, UserWithPassword}

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
