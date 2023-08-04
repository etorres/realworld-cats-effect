package es.eriktorr
package realworld.domain.model

import realworld.domain.model.Password.CipherText
import realworld.domain.model.User.Username

final case class NewUser(email: Email, password: Password[CipherText], username: Username)
