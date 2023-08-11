package es.eriktorr
package realworld.adapter.persistence

import realworld.adapter.persistence.row.{ArticleRow, FavoriteRow, TagRow}

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.postgres.implicits.given

final class PostgresArticlesTestRepository(transactor: HikariTransactor[IO]):
  def add(row: ArticleRow): IO[Unit] =
    sql"""INSERT INTO articles (
         |  article_id,
         |  slug,
         |  title,
         |  description,
         |  body,
         |  created_at,
         |  updated_at,
         |  author_id
         |) VALUES (
         |  ${row.articleId},
         |  ${row.slug},
         |  ${row.title},
         |  ${row.description},
         |  ${row.body},
         |  ${row.createdAt},
         |  ${row.updatedAt},
         |  ${row.authorId}
         |)""".stripMargin.update.run.transact(transactor).void

  def add(row: FavoriteRow): IO[Unit] =
    sql"""INSERT INTO favorites (
         |  profile_id,
         |  article_id
         |) VALUES (
         |  ${row.profileId},
         |  ${row.articleId}
         |)""".stripMargin.update.run.transact(transactor).void

  def add(row: TagRow): IO[Unit] =
    sql"""INSERT INTO tags (
         |  tag,
         |  article_id
         |) VALUES (
         |  ${row.tag},
         |  ${row.articleId}
         |)""".stripMargin.update.run.transact(transactor).void
