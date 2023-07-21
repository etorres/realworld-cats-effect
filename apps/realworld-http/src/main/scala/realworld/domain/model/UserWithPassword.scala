package es.eriktorr
package realworld.domain.model

import realworld.domain.model.Password.CipherText

final case class UserWithPassword(user: User, password: Password[CipherText])
