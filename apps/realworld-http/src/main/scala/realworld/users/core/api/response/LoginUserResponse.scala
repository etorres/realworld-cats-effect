package es.eriktorr
package realworld.users.core.api.response

import realworld.users.core.domain.{Email, User}

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class LoginUserResponse(user: User)

object LoginUserResponse:
  given loginUserResponseJsonDecoder: Decoder[LoginUserResponse] = deriveDecoder

  given loginUserResponseJsonEncoder: Encoder[LoginUserResponse] = deriveEncoder
