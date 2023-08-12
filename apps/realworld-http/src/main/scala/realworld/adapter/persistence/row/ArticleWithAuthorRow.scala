package es.eriktorr
package realworld.adapter.persistence.row

import java.time.LocalDateTime

final case class ArticleWithAuthorRow(
    articleId: Int,
    slug: String,
    title: String,
    description: Option[String],
    body: String,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    authorId: Int,
    username: String,
    bio: Option[String],
    image: Option[String],
)
