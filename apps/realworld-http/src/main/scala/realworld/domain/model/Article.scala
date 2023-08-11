package es.eriktorr
package realworld.domain.model

import realworld.domain.model.Article.*
import realworld.domain.model.Moment.{Created, Updated}
import realworld.domain.model.User.Username
import realworld.shared.data.refined.Constraints.{NonEmptyString, ValidSlug, ValidTag}
import realworld.shared.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.*

import java.net.URI

final case class Article(
    slug: Slug,
    title: Title,
    description: Option[String],
    body: Body,
    tagList: List[Tag],
    createdAt: Moment[Created],
    updatedAt: Moment[Updated],
    favorited: Boolean,
    favoritesCount: Int,
    author: Author,
)

object Article:
  opaque type Slug <: String :| ValidSlug = String :| ValidSlug
  object Slug:
    def from(value: String): AllErrorsOr[Slug] = value.refineValidatedNec[ValidSlug]

    def unsafeFrom(value: String): Slug = from(value).orFail

    given slugDecoder: Decoder[Slug] = Decoder.decodeString.emap(Slug.from(_).eitherMessage)

    given slugEncoder: Encoder[Slug] = Encoder.encodeString.contramap[Slug](identity)

  opaque type Title <: String :| NonEmptyString = String :| NonEmptyString
  object Title:
    def from(value: String): AllErrorsOr[Title] = value.refineValidatedNec[NonEmptyString]

    def unsafeFrom(value: String): Title = from(value).orFail

    given slugDecoder: Decoder[Title] = Decoder.decodeString.emap(Title.from(_).eitherMessage)

    given slugEncoder: Encoder[Title] = Encoder.encodeString.contramap[Title](identity)

  opaque type Body <: String :| NonEmptyString = String :| NonEmptyString
  object Body:
    def from(value: String): AllErrorsOr[Body] = value.refineValidatedNec[NonEmptyString]

    def unsafeFrom(value: String): Body = from(value).orFail

    given slugDecoder: Decoder[Body] = Decoder.decodeString.emap(Body.from(_).eitherMessage)

    given slugEncoder: Encoder[Body] = Encoder.encodeString.contramap[Body](identity)

  opaque type Tag <: String :| ValidTag = String :| ValidTag

  object Tag:
    def from(value: String): AllErrorsOr[Tag] = value.refineValidatedNec[ValidTag]

    def unsafeFrom(value: String): Tag = from(value).orFail

    given slugDecoder: Decoder[Tag] = Decoder.decodeString.emap(Tag.from(_).eitherMessage)

    given slugEncoder: Encoder[Tag] = Encoder.encodeString.contramap[Tag](identity)

  final case class Author(
      username: Username,
      bio: Option[String],
      image: Option[URI],
      following: Boolean,
  )

  given articleDecoder: Decoder[Article] = deriveDecoder

  given articleEncoder: Encoder[Article] = deriveEncoder
