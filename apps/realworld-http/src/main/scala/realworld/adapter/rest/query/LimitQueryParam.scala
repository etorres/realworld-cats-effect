package es.eriktorr
package realworld.adapter.rest.query

import realworld.adapter.rest.query.ValidatedQueryParamDecoder.decodeQueryParamWith
import realworld.domain.service.Pagination.Limit

import org.http4s.QueryParamDecoder
import org.http4s.dsl.io.OptionalQueryParamDecoderMatcher

object LimitQueryParam:
  given limitQueryParamDecoder: QueryParamDecoder[Limit] = decodeQueryParamWith(Limit.from, "limit")

  object OptionalLimitQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Limit]("limit")
