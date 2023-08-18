package es.eriktorr
package realworld.articles.core.domain

import realworld.common.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}

import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.*
import io.github.iltotore.iron.constraint.numeric.Positive

opaque type ArticleId <: Int :| Positive = Int :| Positive

object ArticleId:
  def from(value: Int): AllErrorsOr[ArticleId] = value.refineValidatedNec[Positive]

  def unsafeFrom(value: Int): ArticleId = from(value).orFail
