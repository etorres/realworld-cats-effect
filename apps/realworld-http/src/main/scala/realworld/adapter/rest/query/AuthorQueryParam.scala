package es.eriktorr
package realworld.adapter.rest.query

import realworld.adapter.rest.query.ValidatedQueryParamDecoder.decodeQueryParamWith
import realworld.domain.service.ArticlesFilters.Author

import org.http4s.QueryParamDecoder
import org.http4s.dsl.io.OptionalQueryParamDecoderMatcher

object AuthorQueryParam:
  given authorQueryParamDecoder: QueryParamDecoder[Author] =
    decodeQueryParamWith(Author.from, "author")

  object OptionalAuthorQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Author]("author")
