package es.eriktorr
package realworld.users.core.domain

import realworld.common.data.refined.Constraints.NonNegative
import realworld.common.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}

import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.*

opaque type UserId <: Int :| NonNegative = Int :| NonNegative

object UserId:
  def from(value: Int): AllErrorsOr[UserId] = value.refineValidatedNec[NonNegative]

  def unsafeFrom(value: Int): UserId = from(value).orFail

  val anonymous: UserId = UserId.unsafeFrom(0)
