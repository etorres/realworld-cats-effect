package es.eriktorr
package realworld.users.core.domain

import realworld.common.Secret
import realworld.common.data.refined.Constraints.NonEmptyString
import realworld.common.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}
import realworld.users.core.domain.User.{Token, Username}

import cats.Show
import cats.implicits.catsSyntaxValidatedIdBinCompat0
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.*

import java.net.URI

final case class User(
    email: Email,
    token: Option[Token],
    username: Username,
    bio: Option[String],
    image: Option[URI],
)

object User:
  final case class Token private (value: Secret[String])
  object Token:
    def from(value: String): AllErrorsOr[Token] =
      if value.nonEmpty then Token(Secret(value)).validNec
      else "Should contain any character".invalidNec

    def unsafeFrom(value: String): Token = from(value).orFail

    given tokenShow: Show[Token] = Show.fromToString

    given tokenDecoder: Decoder[Token] = Decoder.decodeString.emap(Token.from(_).eitherMessage)

    given tokenEncoder: Encoder[Token] = Encoder.encodeString.contramap[Token](_.value.value)

  opaque type Username <: String :| NonEmptyString = String :| NonEmptyString
  object Username:
    def from(value: String): AllErrorsOr[Username] = value.refineValidatedNec[NonEmptyString]

    def unsafeFrom(value: String): Username = from(value).orFail

    given usernameDecoder: Decoder[Username] =
      Decoder.decodeString.emap(Username.from(_).eitherMessage)

    given usernameEncoder: Encoder[Username] = Encoder.encodeString.contramap[Username](identity)
