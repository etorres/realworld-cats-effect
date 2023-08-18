package es.eriktorr
package realworld.common.db.mappers

import realworld.common.data.validated.ValidatedNecExtensions.validatedNecTo
import realworld.common.domain.Pagination.{Limit, Offset}

import doobie.Meta

trait PaginationMapper:
  given limitDoobieMapper: Meta[Limit] = Meta[Int].tiemap(Limit.from(_).eitherMessage)(identity)

  given offsetDoobieMapper: Meta[Offset] = Meta[Int].tiemap(Offset.from(_).eitherMessage)(identity)

object PaginationMapper extends PaginationMapper
