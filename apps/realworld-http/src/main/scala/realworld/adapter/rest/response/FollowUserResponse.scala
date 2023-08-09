package es.eriktorr
package realworld.adapter.rest.response

import realworld.domain.model.Profile

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class FollowUserResponse(profile: Profile)

object FollowUserResponse:
  given followUserResponseJsonDecoder: Decoder[FollowUserResponse] = deriveDecoder

  given followUserResponseJsonEncoder: Encoder[FollowUserResponse] = deriveEncoder
