package es.eriktorr
package realworld.users.core.api.request

import realworld.common.Secret
import realworld.common.api.BaseRestController.Transformer
import realworld.users.core.api.request.RegisterNewUserRequest.User
import realworld.users.core.domain.Password.PlainText
import realworld.users.core.domain.User.Username
import realworld.users.core.domain.UserWithPassword.UserWithPlaintextPassword
import realworld.users.core.domain.{Email, Password, User as UserModel}

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
