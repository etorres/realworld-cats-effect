package es.eriktorr
package realworld.common.domain

import realworld.common.data.refined.Constraints.NonNegative
import realworld.common.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}
import realworld.common.domain.Pagination.{Limit, Offset}

import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.*
import io.github.iltotore.iron.constraint.numeric.Positive

final case class Pagination(limit: Limit, offset: Offset)

object Pagination:
  opaque type Limit <: Int :| Positive = Int :| Positive

  object Limit:
    def from(value: Int): AllErrorsOr[Limit] = value.refineValidatedNec[Positive]

    def unsafeFrom(value: Int): Limit = from(value).orFail

  opaque type Offset <: Int :| NonNegative = Int :| NonNegative

  object Offset:
    def from(value: Int): AllErrorsOr[Offset] = value.refineValidatedNec[NonNegative]

    def unsafeFrom(value: Int): Offset = from(value).orFail

  val default: Pagination = Pagination(Limit.unsafeFrom(20), Offset.unsafeFrom(0))
