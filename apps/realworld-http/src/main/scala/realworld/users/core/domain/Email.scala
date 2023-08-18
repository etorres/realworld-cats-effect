package es.eriktorr
package realworld.users.core.domain

import realworld.common.data.refined.Constraints.ValidEmail
import realworld.common.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}

import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.*

opaque type Email <: String :| ValidEmail = String :| ValidEmail

object Email:
  def from(value: String): AllErrorsOr[Email] = value.refineValidatedNec[ValidEmail]

  def unsafeFrom(value: String): Email = from(value).orFail

  given emailDecoder: Decoder[Email] = Decoder.decodeString.emap(Email.from(_).eitherMessage)

  given emailEncoder: Encoder[Email] = Encoder.encodeString.contramap[Email](identity)
