package es.eriktorr
package realworld.adapter.rest.query

import realworld.adapter.rest.query.ValidatedQueryParamDecoder.decodeQueryParamWith
import realworld.domain.service.Pagination.Offset

import org.http4s.QueryParamDecoder
import org.http4s.dsl.io.OptionalQueryParamDecoderMatcher

object OffsetQueryParam:
  given offsetQueryParamDecoder: QueryParamDecoder[Offset] =
    decodeQueryParamWith(Offset.from, "offset")

  object OptionalOffsetQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Offset]("offset")
