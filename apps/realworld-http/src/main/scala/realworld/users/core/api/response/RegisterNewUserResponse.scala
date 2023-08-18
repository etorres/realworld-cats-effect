package es.eriktorr
package realworld.users.core.api.response

import realworld.users.core.domain.User

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class RegisterNewUserResponse(user: User)

object RegisterNewUserResponse:
  given registerNewUserResponseJsonDecoder: Decoder[RegisterNewUserResponse] = deriveDecoder

  given registerNewUserResponseJsonEncoder: Encoder[RegisterNewUserResponse] = deriveEncoder
