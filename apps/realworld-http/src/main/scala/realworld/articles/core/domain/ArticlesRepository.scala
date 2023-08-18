package es.eriktorr
package realworld.articles.core.domain

import realworld.common.domain.Pagination
import realworld.users.core.domain.UserId

import cats.effect.IO

trait ArticlesRepository:
  def findArticlesBy(
      filters: ArticlesFilters,
      pagination: Pagination,
      viewer: Option[UserId],
  ): IO[List[Article]]
