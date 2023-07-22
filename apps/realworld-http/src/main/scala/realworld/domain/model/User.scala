package es.eriktorr
package realworld.domain.model

import realworld.domain.model.User.Username
import realworld.shared.Secret
import realworld.shared.data.error.ValidationError
import realworld.shared.data.refined.Constraints.NonEmptyString
import realworld.shared.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}

import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.*

import java.net.URI

final case class User(
    email: Email,
    token: Option[Secret[String]],
    username: Username,
    bio: String,
    image: Option[URI],
)

object User:
  opaque type Username <: String :| NonEmptyString = String :| NonEmptyString
  object Username:
    def from(value: String): AllErrorsOr[Username] =
      value.refineValidatedNec[NonEmptyString].leftMap(_.map(InvalidUsername.apply))

    def unsafeFrom(value: String): Username = from(value).orFail

    sealed abstract class UsernameValidationError(message: String) extends ValidationError(message)

    final case class InvalidUsername(message: String) extends UsernameValidationError(message)
