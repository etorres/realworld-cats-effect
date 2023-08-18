package es.eriktorr
package realworld.articles.core.api

import realworld.articles.core.api.query.AuthorQueryParam.OptionalAuthorQueryParamMatcher
import realworld.articles.core.api.query.FavoritedQueryParam.OptionalFavoritedQueryParamMatcher
import realworld.articles.core.api.query.LimitQueryParam.OptionalLimitQueryParamMatcher
import realworld.articles.core.api.query.OffsetQueryParam.OptionalOffsetQueryParamMatcher
import realworld.articles.core.api.query.TagQueryParam.OptionalTagQueryParamMatcher
import realworld.articles.core.api.response.ListArticlesResponse
import realworld.articles.core.domain.{ArticlesFilters, ArticlesService}
import realworld.common.api.BaseRestController
import realworld.common.domain.Pagination
import realworld.users.core.domain.UserId

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
