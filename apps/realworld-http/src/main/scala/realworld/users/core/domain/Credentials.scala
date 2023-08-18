package es.eriktorr
package realworld.users.core.domain

import realworld.users.core.domain.Password.PlainText

final case class Credentials(email: Email, password: Password[PlainText])
