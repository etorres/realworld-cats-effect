package es.eriktorr
package realworld.articles.core.domain

import realworld.common.domain.Pagination
import realworld.users.core.domain.UserId

import cats.effect.IO

final class ArticlesService(articlesRepository: ArticlesRepository):
  def findArticlesBy(
      filters: ArticlesFilters,
      pagination: Pagination,
      viewer: Option[UserId],
  ): IO[List[Article]] = articlesRepository.findArticlesBy(filters, pagination, viewer)
