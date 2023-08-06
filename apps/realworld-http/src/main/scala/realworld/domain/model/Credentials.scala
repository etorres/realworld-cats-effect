package es.eriktorr
package realworld.domain.model

import realworld.domain.model.Password.PlainText

final case class Credentials(email: Email, password: Password[PlainText])
