package es.eriktorr
package realworld.adapter.rest.request

import realworld.adapter.rest.BaseRestController.Transformer
import realworld.adapter.rest.request.LoginUserRequest.User
import realworld.domain.model.Password.ClearText
import realworld.domain.model.{Credentials, Email, Password}
import realworld.shared.Secret

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
      (Email.from(request.user.email), Password.from[ClearText](request.user.password.value))
        .mapN(Credentials.apply)
