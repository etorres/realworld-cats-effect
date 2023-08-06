package es.eriktorr
package realworld.adapter.rest.response

import realworld.domain.model.User

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class UpdateUserResponse(user: User)

object UpdateUserResponse:
  given updateUserResponseJsonDecoder: Decoder[UpdateUserResponse] = deriveDecoder

  given updateUserResponseJsonEncoder: Encoder[UpdateUserResponse] = deriveEncoder
