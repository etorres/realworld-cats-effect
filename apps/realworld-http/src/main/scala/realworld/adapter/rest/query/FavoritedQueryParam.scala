package es.eriktorr
package realworld.adapter.rest.query

import realworld.adapter.rest.query.ValidatedQueryParamDecoder.decodeQueryParamWith
import realworld.domain.service.ArticlesFilters.Favorited

import org.http4s.QueryParamDecoder
import org.http4s.dsl.io.OptionalQueryParamDecoderMatcher

object FavoritedQueryParam:
  given favoritedQueryParamDecoder: QueryParamDecoder[Favorited] =
    decodeQueryParamWith(Favorited.from, "favorited")

  object OptionalFavoritedQueryParamMatcher
      extends OptionalQueryParamDecoderMatcher[Favorited]("favorited")
