package es.eriktorr
package realworld.domain.model

import realworld.shared.data.refined.Constraints.NonNegative
import realworld.shared.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}

import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.*

opaque type UserId <: Int :| NonNegative = Int :| NonNegative

object UserId:
  def from(value: Int): AllErrorsOr[UserId] = value.refineValidatedNec[NonNegative]

  def unsafeFrom(value: Int): UserId = from(value).orFail

  val anonymous: UserId = UserId.unsafeFrom(0)
