package es.eriktorr
package realworld.domain.service

import realworld.domain.model.{Article, UserId}

import cats.effect.IO

trait ArticlesRepository:
  def findArticlesBy(
      filters: ArticlesFilters,
      pagination: Pagination,
      viewer: Option[UserId],
  ): IO[List[Article]]
