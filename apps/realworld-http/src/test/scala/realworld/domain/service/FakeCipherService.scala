package es.eriktorr
package realworld.domain.service

import realworld.domain.model.Password
import realworld.domain.model.Password.{CipherText, ClearText}
import realworld.domain.service.FakeCipherService.CipherServiceState

import cats.effect.{IO, Ref}

final class FakeCipherService(stateRef: Ref[IO, CipherServiceState]) extends CipherService:
  override def cipher(password: Password[ClearText]): IO[Password[CipherText]] =
    stateRef.get.flatMap(currentState =>
      IO.fromOption(currentState.passwords.get(password))(
        IllegalArgumentException(s"No hash found for password: $password"),
      ),
    )

  override def check(password: Password[ClearText], hash: Password[CipherText]): IO[Boolean] =
    stateRef.get.map: currentState =>
      val maybePassword = currentState.passwords.get(password)
      maybePassword.contains(hash)

object FakeCipherService:
  final case class CipherServiceState(passwords: Map[Password[ClearText], Password[CipherText]]):
    def setPasswords(
        newPasswords: Map[Password[ClearText], Password[CipherText]],
    ): CipherServiceState = copy(newPasswords)

  object CipherServiceState:
    val empty: CipherServiceState = CipherServiceState(Map.empty)
