package es.eriktorr
package realworld.articles.core.api.query

import realworld.common.api.query.ValidatedQueryParamDecoder.decodeQueryParamWith
import realworld.common.domain.Pagination.Limit

import org.http4s.QueryParamDecoder
import org.http4s.dsl.io.OptionalQueryParamDecoderMatcher

object LimitQueryParam:
  given limitQueryParamDecoder: QueryParamDecoder[Limit] = decodeQueryParamWith(Limit.from, "limit")

  object OptionalLimitQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Limit]("limit")
