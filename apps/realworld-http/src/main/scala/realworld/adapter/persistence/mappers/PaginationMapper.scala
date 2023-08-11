package es.eriktorr
package realworld.adapter.persistence.mappers

import realworld.domain.service.Pagination.{Limit, Offset}
import realworld.shared.data.validated.ValidatedNecExtensions.validatedNecTo

import doobie.Meta

trait PaginationMapper:
  given limitDoobieMapper: Meta[Limit] = Meta[Int].tiemap(Limit.from(_).eitherMessage)(identity)

  given offsetDoobieMapper: Meta[Offset] = Meta[Int].tiemap(Offset.from(_).eitherMessage)(identity)

object PaginationMapper extends PaginationMapper
