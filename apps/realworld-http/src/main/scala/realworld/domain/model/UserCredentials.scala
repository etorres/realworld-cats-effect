package es.eriktorr
package realworld.domain.model

import realworld.domain.model.UserCredentials.Email
import realworld.shared.Secret
import realworld.shared.refined.Types.ValidEmail

import io.github.iltotore.iron.*

final case class UserCredentials(email: Email, password: Secret[String])

object UserCredentials:
  opaque type Email <: String :| ValidEmail = String :| ValidEmail
  object Email:
    def apply(value: String :| ValidEmail): Email = value
