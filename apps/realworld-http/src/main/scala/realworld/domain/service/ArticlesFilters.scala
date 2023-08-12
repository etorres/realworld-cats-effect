package es.eriktorr
package realworld.domain.service

import realworld.domain.model.Article.Tag
import realworld.domain.model.User.Username

final case class ArticlesFilters(
    author: Option[Username],
    favorited: Option[Username],
    tag: Option[Tag],
)
