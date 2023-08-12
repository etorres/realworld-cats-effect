package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.FakeArticlesRepository.ArticlesRepositoryState
import realworld.domain.model.{Article, UserId}
import realworld.domain.service.{ArticlesFilters, ArticlesRepository, Pagination}

import cats.effect.{IO, Ref}

final class FakeArticlesRepository(stateRef: Ref[IO, ArticlesRepositoryState])
    extends ArticlesRepository:
  override def findArticlesBy(
      filters: ArticlesFilters,
      pagination: Pagination,
      userId: UserId,
  ): IO[List[Article]] = stateRef.get.map(_.articles.getOrElse((filters, pagination), List.empty))

object FakeArticlesRepository:
  final case class ArticlesRepositoryState(
      articles: Map[(ArticlesFilters, Pagination), List[Article]],
  ):
    def setArticles(
        newArticles: Map[(ArticlesFilters, Pagination), List[Article]],
    ): ArticlesRepositoryState =
      copy(newArticles)

  object ArticlesRepositoryState:
    val empty: ArticlesRepositoryState = ArticlesRepositoryState(Map.empty)
