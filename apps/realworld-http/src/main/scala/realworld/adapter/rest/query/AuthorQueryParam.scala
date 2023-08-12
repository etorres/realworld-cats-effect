package es.eriktorr
package realworld.adapter.rest.query

import realworld.adapter.rest.query.ValidatedQueryParamDecoder.decodeQueryParamWith
import realworld.domain.model.User.Username

import org.http4s.QueryParamDecoder
import org.http4s.dsl.io.OptionalQueryParamDecoderMatcher

object AuthorQueryParam:
  given authorQueryParamDecoder: QueryParamDecoder[Username] =
    decodeQueryParamWith(Username.from, "author")

  object OptionalAuthorQueryParamMatcher
      extends OptionalQueryParamDecoderMatcher[Username]("author")
