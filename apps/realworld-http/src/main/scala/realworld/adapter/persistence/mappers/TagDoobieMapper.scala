package es.eriktorr
package realworld.adapter.persistence.mappers

import realworld.domain.model.Article.Tag
import realworld.shared.data.validated.ValidatedNecExtensions.validatedNecTo

import doobie.Meta

trait TagDoobieMapper:
  given tagDoobieMapper: Meta[Tag] = Meta[String].tiemap(Tag.from(_).eitherMessage)(identity)

object TagDoobieMapper extends TagDoobieMapper
