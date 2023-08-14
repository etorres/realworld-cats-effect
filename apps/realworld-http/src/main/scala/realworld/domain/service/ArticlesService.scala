package es.eriktorr
package realworld.domain.service

import realworld.domain.model.{Article, UserId}

import cats.effect.IO

final class ArticlesService(articlesRepository: ArticlesRepository):
  def findArticlesBy(
      filters: ArticlesFilters,
      pagination: Pagination,
      viewer: Option[UserId],
  ): IO[List[Article]] = articlesRepository.findArticlesBy(filters, pagination, viewer)
