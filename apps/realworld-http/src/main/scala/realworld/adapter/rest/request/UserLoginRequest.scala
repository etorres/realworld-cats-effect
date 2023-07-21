package es.eriktorr
package realworld.adapter.rest.request

import realworld.adapter.rest.request.UserLoginRequest.User
import realworld.domain.model.Password.ClearText
import realworld.domain.model.{Email, Password, Credentials}
import realworld.shared.Secret

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import io.github.arainko.ducktape.*
import io.github.iltotore.iron.refine

final case class UserLoginRequest(user: User)

object UserLoginRequest:
  final private[request] case class User(email: String, password: Secret[String])

  given userLoginRequestJsonDecoder: Decoder[UserLoginRequest] = deriveDecoder

  given userLoginRequestJsonEncoder: Encoder[UserLoginRequest] = deriveEncoder

  extension (request: UserLoginRequest)
    def toUserCredentials: Credentials =
      request
        .into[Credentials]
        .transform(
          Field.computed(_.email, x => Email(x.user.email.refine)),
          Field.computed(_.password, x => Password.unsafeFrom[ClearText](x.user.password.value)),
        )
