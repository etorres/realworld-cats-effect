package es.eriktorr
package realworld.adapter.persistence.mappers

import realworld.domain.model.UserId
import realworld.shared.data.validated.ValidatedNecExtensions.validatedNecTo

import doobie.Meta

trait UserIdDoobieMapper:
  given userIdDoobieMapper: Meta[UserId] = Meta[Int].tiemap(UserId.from(_).eitherMessage)(identity)

object UserIdDoobieMapper extends UserIdDoobieMapper
