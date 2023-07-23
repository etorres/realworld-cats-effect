package es.eriktorr
package realworld.domain.service

import realworld.domain.model.Email
import realworld.domain.model.User.Token
import realworld.domain.service.FakeAuthService.AuthServiceState

import cats.effect.{IO, Ref}

final class FakeAuthService(stateRef: Ref[IO, AuthServiceState]) extends AuthService:
  override def tokenFor(email: Email): IO[Token] =
    stateRef.get.flatMap(currentState =>
      IO.fromOption(currentState.tokens.get(email))(
        IllegalArgumentException(s"No token found for email: $email"),
      ),
    )

  override def verify(token: Token): IO[Email] =
    stateRef.get.flatMap { currentState =>
      val emails = currentState.tokens
        .filter { case (_, currentToken) =>
          currentToken == token
        }
        .keys
        .toList
      emails match
        case email :: Nil => IO.pure(email)
        case _ => IO.raiseError(IllegalArgumentException("Invalid token"))
    }

object FakeAuthService:
  final case class AuthServiceState(tokens: Map[Email, Token]):
    def setTokens(newTokens: Map[Email, Token]): AuthServiceState = copy(newTokens)

  object AuthServiceState:
    def empty: AuthServiceState = AuthServiceState(Map.empty)
