package es.eriktorr
package realworld.adapter.rest.response

import realworld.domain.model.{Email, User}

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class UserResponse(user: User)

object UserResponse:
  given userResponseDecoder: Decoder[UserResponse] = deriveDecoder

  given userResponseEncoder: Encoder[UserResponse] = deriveEncoder
