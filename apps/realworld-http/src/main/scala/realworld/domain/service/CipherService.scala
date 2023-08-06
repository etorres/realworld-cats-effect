package es.eriktorr
package realworld.domain.service

import realworld.domain.model.Password
import realworld.domain.model.Password.{CipherText, ClearText}
import realworld.shared.data.validated.ValidatedNecExtensions.validatedNecTo

import cats.effect.IO

trait CipherService:
  def cipher(password: Password[ClearText]): IO[Password[CipherText]]

  def check(password: Password[ClearText], hash: Password[CipherText]): IO[Boolean]

object CipherService:
  def impl: CipherService = new CipherService:
    override def cipher(password: Password[ClearText]): IO[Password[CipherText]] =
      Password.cipher(password).validated

    override def check(password: Password[ClearText], hash: Password[CipherText]): IO[Boolean] =
      IO.pure(Password.check(password, hash))
