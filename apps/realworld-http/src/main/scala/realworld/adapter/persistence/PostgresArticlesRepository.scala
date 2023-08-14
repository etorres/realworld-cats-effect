package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.mappers.PaginationMapper.{
  limitDoobieMapper,
  offsetDoobieMapper,
}
import realworld.adapter.persistence.mappers.TagDoobieMapper.tagDoobieMapper
import realworld.adapter.persistence.mappers.UserIdDoobieMapper.userIdDoobieMapper
import realworld.adapter.persistence.mappers.UsernameDoobieMapper.usernameDoobieMapper
import realworld.adapter.persistence.row.{ArticleWithAuthorRow, FavoriteRow, FollowerRow, TagRow}
import realworld.domain.model.Article.*
import realworld.domain.model.Moment.{Created, Updated}
import realworld.domain.model.User.Username
import realworld.domain.model.{Article, Moment, UserId}
import realworld.domain.service.{ArticlesFilters, ArticlesRepository, Pagination}
import realworld.shared.data.refined.StringExtensions.toUri
import realworld.shared.data.validated.ValidatedNecExtensions.validatedNecTo

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits.{catsSyntaxTuple2Semigroupal, toTraverseOps}
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
      viewer: Option[UserId],
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
      .query[ArticleWithAuthorRow]
      .to[List]
      .transact(transactor)
    favorites <- NonEmptyList.fromList(articles.map(_.articleId)) match
      case Some(articleIds) =>
        sql"""SELECT profile_id, article_id
             |FROM favorites
             |WHERE ${in(fr"article_id", articleIds)}""".stripMargin
          .query[FavoriteRow]
          .to[List]
          .transact(transactor)
      case None => IO.pure(List.empty)
    tags <- NonEmptyList.fromList(articles.map(_.articleId)) match
      case Some(articleIds) =>
        sql"""SELECT tag, article_id
             |FROM tags
             |WHERE ${in(fr"article_id", articleIds)}""".stripMargin
          .query[TagRow]
          .to[List]
          .transact(transactor)
      case None => IO.pure(List.empty)
    followed <- (NonEmptyList.fromList(articles.map(_.authorId)), viewer).tupled match
      case Some((authorIds, userId)) =>
        sql"""SELECT user_id, follower_id
             |FROM followers
             |WHERE ${in(fr"user_id", authorIds)}
             |AND follower_id = $userId""".stripMargin
          .query[FollowerRow]
          .map(_.userId)
          .to[List]
          .transact(transactor)
      case None => IO.pure(List.empty)
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
        favorited = viewer match
          case Some(userId) =>
            favorites.filter(_.articleId == article.articleId).exists(_.profileId == userId)
          case None => false
        favoritesCount = favorites.count(_.articleId == article.articleId)
        author <- for
          username <- Username.from(article.username).validated
          bio = article.bio
          image <- article.image.traverse(_.toUri).validated
          following = followed.contains(article.authorId)
        yield Author(username, bio, image, following)
      yield Article(
        slug,
        title,
        article.description,
        body,
        tagList.sorted,
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
          filters.favorited.map(x => fr"users.username = $x"),
          filters.tag.map(x => fr"tags.tag = $x"),
        ).toList.collect { case Some(value) => value },
      )
      .getOrElse(NonEmptyList.one(fr"true"))
