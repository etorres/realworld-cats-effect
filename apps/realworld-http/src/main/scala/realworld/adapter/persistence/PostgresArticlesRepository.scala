package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.mappers.PaginationMapper.{
  limitDoobieMapper,
  offsetDoobieMapper,
}
import realworld.adapter.persistence.row.ArticleRow
import realworld.domain.model.{Article, UserId}
import realworld.domain.service.{ArticlesFilters, ArticlesRepository, Pagination}

import cats.data.NonEmptyList
import cats.effect.IO
import doobie.Fragments.whereAnd
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.postgres.implicits.JavaTimeLocalDateTimeMeta
import doobie.util.fragment.Fragment

final class PostgresArticlesRepository(transactor: HikariTransactor[IO]) extends ArticlesRepository:
  override def findArticlesBy(
      filters: ArticlesFilters,
      pagination: Pagination,
      userId: UserId,
  ): IO[List[Article]] = for
    articleRows <- fr"""SELECT
                       |  articles.article_id,
                       |  articles.slug,
                       |  articles.title,
                       |  articles.description,
                       |  articles.body,
                       |  articles.created_at,
                       |  articles.updated_at,
                       |  articles.author_id
                       |FROM articles
                       |LEFT JOIN users authors ON authors.user_id = articles.author_id
                       |LEFT JOIN favorites ON favorites.article_id = articles.article_id
                       |LEFT JOIN users ON users.user_id = favorites.profile_id
                       |LEFT JOIN tags ON tags.article_id = articles.article_id
                       |${whereAnd(whereFrom(filters))}
                       |GROUP BY
                       |  articles.article_id,
                       |  articles.slug,
                       |  articles.title,
                       |  articles.description,
                       |  articles.body,
                       |  articles.created_at,
                       |  articles.updated_at,
                       |  articles.author_id
                       |ORDER BY articles.created_at DESC
                       |LIMIT ${pagination.limit}
                       |OFFSET ${pagination.offset}""".stripMargin
      .query[ArticleRow]
      .to[List]
      .transact(transactor)
    _ = articleRows.foreach(println) // TODO
    result <- IO.pure(List.empty) // TODO
  yield result

  private def whereFrom(filters: ArticlesFilters): NonEmptyList[Fragment] =
    NonEmptyList
      .fromList(
        (
          filters.author.map(x => fr"authors.username = $x"),
          filters.favorited.map(x => fr"favorites.username = $x"),
          filters.tag.map(x => fr"tags.tag = $x"),
        ).toList.collect { case Some(value) => value },
      )
      .getOrElse(NonEmptyList.one(fr"true"))
