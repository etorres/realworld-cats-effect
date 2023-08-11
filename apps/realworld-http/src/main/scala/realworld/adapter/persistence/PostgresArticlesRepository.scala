package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.mappers.PaginationMapper.{
  limitDoobieMapper,
  offsetDoobieMapper,
}
import realworld.adapter.persistence.row.{ArticleRow, FavoriteRow, TagRow}
import realworld.domain.model.Article.*
import realworld.domain.model.Moment.{Created, Updated}
import realworld.domain.model.User.Username
import realworld.domain.model.{Article, Moment, UserId}
import realworld.domain.service.{ArticlesFilters, ArticlesRepository, Pagination}
import realworld.shared.data.validated.ValidatedNecExtensions.validatedNecTo

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits.toTraverseOps
import doobie.Fragments.whereAnd
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.postgres.implicits.JavaTimeLocalDateTimeMeta
import doobie.util.fragment.Fragment
import doobie.util.fragments.in

final class PostgresArticlesRepository(transactor: HikariTransactor[IO]) extends ArticlesRepository:
  override def findArticlesBy(
      filters: ArticlesFilters,
      pagination: Pagination,
      userId: UserId,
  ): IO[List[Article]] = for
    articles <- fr"""SELECT
                    |  articles.article_id,
                    |  articles.slug,
                    |  articles.title,
                    |  articles.description,
                    |  articles.body,
                    |  articles.created_at,
                    |  articles.updated_at,
                    |  articles.author_id,
                    |  authors.username,
                    |  authors.bio,
                    |  authors.image
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
                    |  articles.author_id,
                    |  authors.username,
                    |  authors.bio,
                    |  authors.image
                    |ORDER BY articles.created_at DESC
                    |LIMIT ${pagination.limit}
                    |OFFSET ${pagination.offset}""".stripMargin
      .query[ArticleRow]
      .to[List]
      .transact(transactor)
    articleIds = NonEmptyList.fromListUnsafe(articles.map(_.articleId))
    favorites <- sql"""SELECT profile_id, article_id
                      |FROM favorites
                      |WHERE ${in(fr"article_id", articleIds)}""".stripMargin
      .query[FavoriteRow]
      .to[List]
      .transact(transactor)
    tags <- sql"""SELECT tag, article_id
                 |FROM tags
                 |WHERE ${in(fr"article_id", articleIds)}""".stripMargin
      .query[TagRow]
      .to[List]
      .transact(transactor)
    result <- articles.traverse: article =>
      for
        slug <- Slug.from(article.slug).validated
        title <- Title.from(article.title).validated
        body <- Body.from(article.body).validated
        tagList <- tags
          .filter(_.articleId == article.articleId)
          .traverse: tag =>
            Tag.from(tag.tag).validated
        createdAt <- Moment.from[Created](article.createdAt).validated
        updatedAt <- Moment.from[Updated](article.updatedAt).validated
        favorited = favorites.exists(_.profileId == userId)
        favoritesCount = favorites.count(_.articleId == article.articleId)
        author <- for
          username <- Username.from("username").validated
          bio = None
          image = None
          following = false
        yield Author(username, bio, image, following)
      yield Article(
        slug,
        title,
        article.description,
        body,
        tagList,
        createdAt,
        updatedAt,
        favorited,
        favoritesCount,
        author,
      )
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
