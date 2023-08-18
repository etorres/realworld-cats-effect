package es.eriktorr
package realworld.articles.core.api.query

import realworld.articles.core.domain.Article.Tag
import realworld.common.api.query.ValidatedQueryParamDecoder.decodeQueryParamWith

import org.http4s.QueryParamDecoder
import org.http4s.dsl.io.OptionalQueryParamDecoderMatcher

object TagQueryParam:
  given tagQueryParamDecoder: QueryParamDecoder[Tag] = decodeQueryParamWith(Tag.from, "tag")

  object OptionalTagQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Tag]("tag")
