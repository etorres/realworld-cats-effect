package es.eriktorr
package realworld.domain.model

import realworld.shared.data.error.ValidationError
import realworld.shared.data.refined.Constraints.ValidEmail
import realworld.shared.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}

import cats.data.Validated
import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.*

opaque type Email <: String :| ValidEmail = String :| ValidEmail

object Email:
  def from(value: String): AllErrorsOr[Email] =
    value.refineValidatedNec[ValidEmail].leftMap(_.map(InvalidEmail.apply))

  def unsafeFrom(value: String): Email = from(value).orFail

  sealed abstract class EmailValidationError(message: String) extends ValidationError(message)

  final case class InvalidEmail(message: String) extends EmailValidationError(message)
