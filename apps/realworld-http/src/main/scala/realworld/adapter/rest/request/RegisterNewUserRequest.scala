package es.eriktorr
package realworld.adapter.rest.request

import realworld.adapter.rest.BaseRestController.Transformer
import realworld.adapter.rest.request.RegisterNewUserRequest.User
import realworld.domain.model.Password.ClearText
import realworld.domain.model.User.Username
import realworld.domain.model.{Email, NewUser, Password}
import realworld.shared.Secret

import cats.implicits.catsSyntaxTuple3Semigroupal
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class RegisterNewUserRequest(user: User)

object RegisterNewUserRequest:
  final case class User(email: String, password: Secret[String], username: String)

  given registerNewUserRequestJsonDecoder: Decoder[RegisterNewUserRequest] = deriveDecoder

  given registerNewUserRequestJsonEncoder: Encoder[RegisterNewUserRequest] = deriveEncoder

  given registerNewUserRequestTransformer: Transformer[RegisterNewUserRequest, NewUser] =
    (request: RegisterNewUserRequest) =>
      (
        Email.from(request.user.email),
        Password.from[ClearText](request.user.password.value).andThen(Password.cipher),
        Username.from(request.user.username),
      ).mapN(NewUser.apply)
