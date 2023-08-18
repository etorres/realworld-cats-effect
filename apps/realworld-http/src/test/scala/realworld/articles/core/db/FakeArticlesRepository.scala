package es.eriktorr
package realworld.articles.core.db

import realworld.articles.core.db.FakeArticlesRepository.ArticlesRepositoryState
import realworld.articles.core.domain.{Article, ArticlesFilters, ArticlesRepository}
import realworld.common.domain.Pagination
import realworld.users.core.domain.UserId

import cats.effect.{IO, Ref}

final class FakeArticlesRepository(stateRef: Ref[IO, ArticlesRepositoryState])
    extends ArticlesRepository:
  override def findArticlesBy(
      filters: ArticlesFilters,
      pagination: Pagination,
      viewer: Option[UserId],
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
