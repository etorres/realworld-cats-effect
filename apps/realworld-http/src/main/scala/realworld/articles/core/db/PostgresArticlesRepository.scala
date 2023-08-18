package es.eriktorr
package realworld.articles.core.db

import realworld.articles.core.db.mappers.TagDoobieMapper.tagDoobieMapper
import realworld.articles.core.db.row.{ArticleWithAuthorRow, FavoriteRow, TagRow}
import realworld.articles.core.domain.Article.*
import realworld.articles.core.domain.Moment.{Created, Updated}
import realworld.articles.core.domain.{Article, ArticlesFilters, ArticlesRepository, Moment}
import realworld.common.data.refined.StringExtensions.toUri
import realworld.common.data.validated.ValidatedNecExtensions.validatedNecTo
import realworld.common.db.mappers.PaginationMapper.{limitDoobieMapper, offsetDoobieMapper}
import realworld.common.domain.Pagination
import realworld.users.core.db.mappers.UserIdDoobieMapper.userIdDoobieMapper
import realworld.users.core.db.mappers.UsernameDoobieMapper.usernameDoobieMapper
import realworld.users.core.domain.User.Username
import realworld.users.core.domain.UserId
import realworld.users.profiles.db.row.FollowerRow

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits.{catsSyntaxTuple2Semigroupal, catsSyntaxTuple3Parallel, toTraverseOps}
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
    articleRows <- findArticlesWithAuthorBy(filters, pagination)
    articles <- (
      favoritesFor(articleRows),
      tagsFor(articleRows),
      followedFor(articleRows, viewer),
    ).parFlatMapN { (favoriteRows, tagRows, followed) =>
      articleRows.traverse: articleRow =>
        for
          slug <- Slug.from(articleRow.slug).validated
          title <- Title.from(articleRow.title).validated
          body <- Body.from(articleRow.body).validated
          tagList <- tagRows
            .filter(_.articleId == articleRow.articleId)
            .traverse(tagRow => Tag.from(tagRow.tag).validated)
          createdAt <- Moment.from[Created](articleRow.createdAt).validated
          updatedAt <- Moment.from[Updated](articleRow.updatedAt).validated
          favorited = viewer match
            case Some(userId) =>
              favoriteRows.filter(_.articleId == articleRow.articleId).exists(_.profileId == userId)
            case None => false
          favoritesCount = favoriteRows.count(_.articleId == articleRow.articleId)
          author <- for
            username <- Username.from(articleRow.username).validated
            bio = articleRow.bio
            image <- articleRow.image.traverse(_.toUri).validated
            following = followed.contains(articleRow.authorId)
          yield Author(username, bio, image, following)
        yield Article(
          slug,
          title,
          articleRow.description,
          body,
          tagList.sorted,
          createdAt,
          updatedAt,
          favorited,
          favoritesCount,
          author,
        )
    }
  yield articles

  private def findArticlesWithAuthorBy(filters: ArticlesFilters, pagination: Pagination) =
    fr"""SELECT
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

  private def favoritesFor(articles: List[ArticleWithAuthorRow]) =
    NonEmptyList.fromList(articles.map(_.articleId)) match
      case Some(articleIds) =>
        sql"""SELECT profile_id, article_id
             |FROM favorites
             |WHERE ${in(fr"article_id", articleIds)}""".stripMargin
          .query[FavoriteRow]
          .to[List]
          .transact(transactor)
      case None => IO.pure(List.empty)

  private def tagsFor(articles: List[ArticleWithAuthorRow]) =
    NonEmptyList.fromList(articles.map(_.articleId)) match
      case Some(articleIds) =>
        sql"""SELECT tag, article_id
             |FROM tags
             |WHERE ${in(fr"article_id", articleIds)}""".stripMargin
          .query[TagRow]
          .to[List]
          .transact(transactor)
      case None => IO.pure(List.empty)

  private def followedFor(articles: List[ArticleWithAuthorRow], viewer: Option[UserId]) =
    (NonEmptyList.fromList(articles.map(_.authorId)), viewer).tupled match
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
