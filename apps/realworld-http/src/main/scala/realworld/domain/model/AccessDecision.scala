package es.eriktorr
package realworld.domain.model

import realworld.domain.model.AccessDecision.Access
import realworld.domain.model.UserCredentials.Email

final case class AccessDecision(email: Email, access: Access)

object AccessDecision:
  enum Access:
    case Granted, Forbidden
