package es.eriktorr
package realworld.adapter.persistence.mappers

import realworld.domain.model.User.Username
import realworld.shared.data.validated.ValidatedNecExtensions.validatedNecTo

import doobie.Meta

trait UsernameDoobieMapper:
  given usernameDoobieMapper: Meta[Username] =
    Meta[String].tiemap(Username.from(_).eitherMessage)(identity)

object UsernameDoobieMapper extends UsernameDoobieMapper
