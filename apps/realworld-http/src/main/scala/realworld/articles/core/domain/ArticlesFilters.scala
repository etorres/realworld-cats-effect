package es.eriktorr
package realworld.articles.core.domain

import realworld.articles.core.domain.Article.Tag
import realworld.users.core.domain.User.Username

final case class ArticlesFilters(
    author: Option[Username],
    favorited: Option[Username],
    tag: Option[Tag],
)
