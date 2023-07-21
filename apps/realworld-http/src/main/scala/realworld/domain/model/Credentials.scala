package es.eriktorr
package realworld.domain.model

import realworld.domain.model.Password.ClearText

final case class Credentials(email: Email, password: Password[ClearText])
