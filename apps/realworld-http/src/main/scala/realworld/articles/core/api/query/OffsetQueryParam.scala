package es.eriktorr
package realworld.articles.core.api.query

import realworld.common.api.query.ValidatedQueryParamDecoder.decodeQueryParamWith
import realworld.common.domain.Pagination.Offset

import org.http4s.QueryParamDecoder
import org.http4s.dsl.io.OptionalQueryParamDecoderMatcher

object OffsetQueryParam:
  given offsetQueryParamDecoder: QueryParamDecoder[Offset] =
    decodeQueryParamWith(Offset.from, "offset")

  object OptionalOffsetQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Offset]("offset")
