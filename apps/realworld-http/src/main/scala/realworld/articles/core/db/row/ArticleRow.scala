package es.eriktorr
package realworld.articles.core.db.row

import java.time.LocalDateTime

final case class ArticleRow(
    articleId: Int,
    slug: String,
    title: String,
    description: Option[String],
    body: String,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    authorId: Int,
)
