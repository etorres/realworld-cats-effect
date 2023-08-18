package es.eriktorr
package realworld.articles.core.api.query

import realworld.common.api.query.ValidatedQueryParamDecoder.decodeQueryParamWith
import realworld.users.core.domain.User.Username

import org.http4s.QueryParamDecoder
import org.http4s.dsl.io.OptionalQueryParamDecoderMatcher

object AuthorQueryParam:
  given authorQueryParamDecoder: QueryParamDecoder[Username] =
    decodeQueryParamWith(Username.from, "author")

  object OptionalAuthorQueryParamMatcher
      extends OptionalQueryParamDecoderMatcher[Username]("author")
