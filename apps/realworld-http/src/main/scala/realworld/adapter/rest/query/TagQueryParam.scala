package es.eriktorr
package realworld.adapter.rest.query

import realworld.adapter.rest.query.ValidatedQueryParamDecoder.decodeQueryParamWith
import realworld.domain.model.Article.Tag

import org.http4s.QueryParamDecoder
import org.http4s.dsl.io.OptionalQueryParamDecoderMatcher

object TagQueryParam:
  given tagQueryParamDecoder: QueryParamDecoder[Tag] = decodeQueryParamWith(Tag.from, "tag")

  object OptionalTagQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Tag]("tag")
