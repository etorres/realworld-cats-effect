package es.eriktorr
package realworld.domain.model

import realworld.domain.model.User.Username
import realworld.shared.Secret
import realworld.shared.data.refined.Constraints.NonEmptyString
import realworld.shared.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}

import io.circe.{Decoder, Encoder}
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
    def from(value: String): AllErrorsOr[Username] = value.refineValidatedNec[NonEmptyString]

    def unsafeFrom(value: String): Username = from(value).orFail

    given usernameDecoder: Decoder[Username] =
      Decoder.decodeString.emap(Username.from(_).eitherMessage)

    given usernameEncoder: Encoder[Username] = Encoder.encodeString.contramap[Username](identity)
