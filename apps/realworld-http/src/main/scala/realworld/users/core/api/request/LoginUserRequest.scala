package es.eriktorr
package realworld.users.core.api.request

import realworld.common.Secret
import realworld.common.api.BaseRestController.Transformer
import realworld.users.core.api.request.LoginUserRequest.User
import realworld.users.core.domain.Password.PlainText
import realworld.users.core.domain.{Credentials, Email, Password}

import cats.implicits.catsSyntaxTuple2Semigroupal
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class LoginUserRequest(user: User)

object LoginUserRequest:
  final case class User(email: String, password: Secret[String])

  given loginUserRequestJsonDecoder: Decoder[LoginUserRequest] = deriveDecoder

  given loginUserRequestJsonEncoder: Encoder[LoginUserRequest] = deriveEncoder

  given loginUserRequestTransformer: Transformer[LoginUserRequest, Credentials] =
    (request: LoginUserRequest) =>
      (Email.from(request.user.email), Password.from[PlainText](request.user.password.value))
        .mapN(Credentials.apply)
