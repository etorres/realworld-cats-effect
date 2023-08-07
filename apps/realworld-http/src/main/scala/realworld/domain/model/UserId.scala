package es.eriktorr
package realworld.domain.model

import realworld.shared.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}

import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.*
import io.github.iltotore.iron.constraint.numeric.Positive

opaque type UserId <: Int :| Positive = Int :| Positive

object UserId:
  def from(value: Int): AllErrorsOr[UserId] = value.refineValidatedNec[Positive]

  def unsafeFrom(value: Int): UserId = from(value).orFail
