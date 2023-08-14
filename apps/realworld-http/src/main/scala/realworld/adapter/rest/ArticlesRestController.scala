package es.eriktorr
package realworld.adapter.rest

import realworld.adapter.rest.query.AuthorQueryParam.OptionalAuthorQueryParamMatcher
import realworld.adapter.rest.query.FavoritedQueryParam.OptionalFavoritedQueryParamMatcher
import realworld.adapter.rest.query.LimitQueryParam.OptionalLimitQueryParamMatcher
import realworld.adapter.rest.query.OffsetQueryParam.OptionalOffsetQueryParamMatcher
import realworld.adapter.rest.query.TagQueryParam.OptionalTagQueryParamMatcher
import realworld.adapter.rest.response.ListArticlesResponse
import realworld.domain.model.UserId
import realworld.domain.service.*

import cats.effect.IO
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io.*
import org.typelevel.log4cats.SelfAwareStructuredLogger

final class ArticlesRestController(articlesService: ArticlesService)(using
    logger: SelfAwareStructuredLogger[IO],
) extends BaseRestController:
  override val optionalAuthRoutes: Option[AuthedRoutes[UserId, IO]] = Some(
    AuthedRoutes.of[UserId, IO]:
      case request @ GET -> Root / "articles"
          :? OptionalAuthorQueryParamMatcher(author)
          +& OptionalFavoritedQueryParamMatcher(favorited)
          +& OptionalLimitQueryParamMatcher(limit)
          +& OptionalOffsetQueryParamMatcher(offset)
          +& OptionalTagQueryParamMatcher(tag) as userId =>
        (for
          articles <- articlesService.findArticlesBy(
            ArticlesFilters(author, favorited, tag),
            Pagination(
              limit.getOrElse(Pagination.default.limit),
              offset.getOrElse(Pagination.default.offset),
            ),
            if userId != UserId.anonymous then Some(userId) else None,
          )
          response <- Ok(ListArticlesResponse(articles, articles.length))
        yield response).handleErrorWith(contextFrom(request.req)),
  )
