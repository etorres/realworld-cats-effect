package es.eriktorr
package realworld.domain.model

import realworld.domain.model.Password.{CipherText, ClearText, Format}
import realworld.domain.model.User.Username

import io.github.arainko.ducktape.*

final case class NewUser[A <: Format](email: Email, password: Password[A], username: Username)

object NewUser:
  extension (newUser: NewUser[ClearText])
    def withHash(hash: Password[CipherText]): NewUser[CipherText] =
      newUser.into[NewUser[CipherText]].transform(Field.const(_.password, hash))
