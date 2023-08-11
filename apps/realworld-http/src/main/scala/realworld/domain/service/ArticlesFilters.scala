package es.eriktorr
package realworld.domain.service

import realworld.shared.data.refined.Constraints.NonEmptyString
import realworld.shared.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}

import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.*

final case class ArticlesFilters(
    author: Option[String],
    favorited: Option[String],
    tag: Option[String],
)

object ArticlesFilters:
  opaque type Author <: String :| NonEmptyString = String :| NonEmptyString

  object Author:
    def from(value: String): AllErrorsOr[Author] = value.refineValidatedNec[NonEmptyString]

    def unsafeFrom(value: String): Author = from(value).orFail

  opaque type Favorited <: String :| NonEmptyString = String :| NonEmptyString

  object Favorited:
    def from(value: String): AllErrorsOr[Favorited] = value.refineValidatedNec[NonEmptyString]

    def unsafeFrom(value: String): Favorited = from(value).orFail

  opaque type Tag <: String :| NonEmptyString = String :| NonEmptyString

  object Tag:
    def from(value: String): AllErrorsOr[Tag] = value.refineValidatedNec[NonEmptyString]

    def unsafeFrom(value: String): Tag = from(value).orFail
