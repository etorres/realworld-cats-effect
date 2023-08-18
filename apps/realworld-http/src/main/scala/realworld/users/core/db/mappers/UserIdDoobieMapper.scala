package es.eriktorr
package realworld.users.core.db.mappers

import realworld.common.data.validated.ValidatedNecExtensions.validatedNecTo
import realworld.users.core.domain.UserId

import doobie.Meta

trait UserIdDoobieMapper:
  given userIdDoobieMapper: Meta[UserId] = Meta[Int].tiemap(UserId.from(_).eitherMessage)(identity)

object UserIdDoobieMapper extends UserIdDoobieMapper
