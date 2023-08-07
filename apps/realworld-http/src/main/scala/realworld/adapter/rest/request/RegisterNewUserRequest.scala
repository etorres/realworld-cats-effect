package es.eriktorr
package realworld.adapter.rest.request

import realworld.adapter.rest.BaseRestController.Transformer
import realworld.adapter.rest.request.RegisterNewUserRequest.User
import realworld.domain.model.Password.PlainText
import realworld.domain.model.User.Username
import realworld.domain.model.UserWithPassword.UserWithPlaintextPassword
import realworld.domain.model.{Email, Password, User as UserModel}
import realworld.shared.Secret

import cats.implicits.catsSyntaxTuple2Semigroupal
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class RegisterNewUserRequest(user: User)

object RegisterNewUserRequest:
  final case class User(email: String, password: Secret[String], username: String)

  given registerNewUserRequestJsonDecoder: Decoder[RegisterNewUserRequest] = deriveDecoder

  given registerNewUserRequestJsonEncoder: Encoder[RegisterNewUserRequest] = deriveEncoder

  given registerNewUserRequestTransformer
      : Transformer[RegisterNewUserRequest, UserWithPlaintextPassword] =
    (request: RegisterNewUserRequest) =>
      (
        Email.from(request.user.email),
        Username.from(request.user.username),
      ).mapN { case (email, username) => UserModel(email, None, username, None, None) }
        .andThen(user =>
          Password
            .from[PlainText](request.user.password.value)
            .map(password => UserWithPlaintextPassword(user, password)),
        )
