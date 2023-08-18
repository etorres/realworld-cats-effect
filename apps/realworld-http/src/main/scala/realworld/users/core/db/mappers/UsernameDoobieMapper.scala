package es.eriktorr
package realworld.users.core.db.mappers

import realworld.common.data.validated.ValidatedNecExtensions.validatedNecTo
import realworld.users.core.domain.User.Username

import doobie.Meta

trait UsernameDoobieMapper:
  given usernameDoobieMapper: Meta[Username] =
    Meta[String].tiemap(Username.from(_).eitherMessage)(identity)

object UsernameDoobieMapper extends UsernameDoobieMapper
