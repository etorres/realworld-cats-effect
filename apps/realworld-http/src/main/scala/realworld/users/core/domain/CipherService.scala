package es.eriktorr
package realworld.users.core.domain

import realworld.common.data.validated.ValidatedNecExtensions.validatedNecTo
import realworld.users.core.domain.Password.{CipherText, PlainText}

import cats.effect.IO

trait CipherService:
  def cipher(password: Password[PlainText]): IO[Password[CipherText]]

  def check(password: Password[PlainText], hash: Password[CipherText]): IO[Boolean]

object CipherService:
  def impl: CipherService = new CipherService:
    override def cipher(password: Password[PlainText]): IO[Password[CipherText]] =
      Password.cipher(password).validated

    override def check(password: Password[PlainText], hash: Password[CipherText]): IO[Boolean] =
      IO.pure(Password.check(password, hash))
