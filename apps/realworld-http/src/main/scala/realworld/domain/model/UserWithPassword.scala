package es.eriktorr
package realworld.domain.model

import realworld.domain.model.Password.{CipherText, Format, PlainText}

import io.github.arainko.ducktape.*

final case class UserWithPassword[A <: Format](user: User, password: Password[A])

object UserWithPassword:
  extension (userWithPassword: UserWithPassword[PlainText])
    def withHash(hash: Password[CipherText]): UserWithPassword[CipherText] =
      userWithPassword.into[UserWithPassword[CipherText]].transform(Field.const(_.password, hash))
