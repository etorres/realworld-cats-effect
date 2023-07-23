package es.eriktorr
package realworld.adapter.rest.request

import realworld.adapter.rest.request.UserLoginRequest.User
import realworld.domain.model.Password.ClearText
import realworld.domain.model.{Credentials, Email, Password}
import realworld.shared.Secret
import realworld.shared.data.validated.ValidatedNecExtensions.AllErrorsOr

import cats.implicits.catsSyntaxTuple2Semigroupal
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import io.github.arainko.ducktape.*

final case class UserLoginRequest(user: User)

object UserLoginRequest:
  final private[request] case class User(email: String, password: Secret[String])

  given userLoginRequestJsonDecoder: Decoder[UserLoginRequest] = deriveDecoder

  given userLoginRequestJsonEncoder: Encoder[UserLoginRequest] = deriveEncoder

  extension (request: UserLoginRequest)
    def toCredentials: AllErrorsOr[Credentials] =
      (Email.from(request.user.email), Password.from[ClearText](request.user.password.value))
        .mapN { case (email, password) =>
          request
            .into[Credentials]
            .transform(Field.const(_.email, email), Field.const(_.password, password))
        }
