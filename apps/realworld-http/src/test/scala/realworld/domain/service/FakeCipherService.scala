package es.eriktorr
package realworld.domain.service

import realworld.domain.model.Password
import realworld.domain.model.Password.{CipherText, PlainText}
import realworld.domain.service.FakeCipherService.CipherServiceState

import cats.effect.{IO, Ref}

final class FakeCipherService(stateRef: Ref[IO, CipherServiceState]) extends CipherService:
  override def cipher(password: Password[PlainText]): IO[Password[CipherText]] =
    stateRef.get.flatMap(currentState =>
      IO.fromOption(currentState.passwords.get(password))(
        IllegalArgumentException(s"No hash found for password: $password"),
      ),
    )

  override def check(password: Password[PlainText], hash: Password[CipherText]): IO[Boolean] =
    stateRef.get.map: currentState =>
      val maybePassword = currentState.passwords.get(password)
      maybePassword.contains(hash)

object FakeCipherService:
  final case class CipherServiceState(passwords: Map[Password[PlainText], Password[CipherText]]):
    def setPasswords(
                      newPasswords: Map[Password[PlainText], Password[CipherText]],
    ): CipherServiceState = copy(newPasswords)

  object CipherServiceState:
    val empty: CipherServiceState = CipherServiceState(Map.empty)
