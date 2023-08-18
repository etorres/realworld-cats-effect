package es.eriktorr
package realworld.articles.core.db.mappers

import realworld.articles.core.domain.Article.Tag
import realworld.common.data.validated.ValidatedNecExtensions.validatedNecTo

import doobie.Meta

trait TagDoobieMapper:
  given tagDoobieMapper: Meta[Tag] = Meta[String].tiemap(Tag.from(_).eitherMessage)(identity)

object TagDoobieMapper extends TagDoobieMapper
