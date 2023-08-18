package es.eriktorr
package realworld.users.core.api.response

import realworld.users.core.domain.User

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class GetCurrentUserResponse(user: User)

object GetCurrentUserResponse:
  given getCurrentUserResponseJsonDecoder: Decoder[GetCurrentUserResponse] = deriveDecoder

  given getCurrentUserResponseJsonEncoder: Encoder[GetCurrentUserResponse] = deriveEncoder
