package es.eriktorr
package realworld.adapter.rest

import realworld.adapter.rest.query.AuthorQueryParam.OptionalAuthorQueryParamMatcher
import realworld.adapter.rest.query.FavoritedQueryParam.OptionalFavoritedQueryParamMatcher
import realworld.adapter.rest.query.LimitQueryParam.OptionalLimitQueryParamMatcher
import realworld.adapter.rest.query.OffsetQueryParam.OptionalOffsetQueryParamMatcher
import realworld.adapter.rest.query.TagQueryParam.OptionalTagQueryParamMatcher
import realworld.adapter.rest.response.GetArticlesResponse
import realworld.domain.model.UserId
import realworld.domain.service.*

import cats.effect.IO
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io.*
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.typelevel.log4cats.SelfAwareStructuredLogger

final class ArticlesRestController(
    articlesService: ArticlesService,
    authService: AuthService,
    usersService: UsersService,
)(using
    logger: SelfAwareStructuredLogger[IO],
) extends BaseRestController(authService, usersService):
  val routes: HttpRoutes[IO] =
    val secureRoutes = AuthedRoutes.of[UserId, IO]:
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
            userId,
          )
          response <- Ok(GetArticlesResponse(articles, articles.length))
        yield response).handleErrorWith(contextFrom(request.req))

    authMiddleware(secureRoutes)
