package es.eriktorr
package realworld.adapter.rest.response

import realworld.domain.model.Profile

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class UnfollowUserResponse(profile: Profile)

object UnfollowUserResponse:
  given unfollowUserResponseJsonDecoder: Decoder[UnfollowUserResponse] = deriveDecoder

  given unfollowUserResponseJsonEncoder: Encoder[UnfollowUserResponse] = deriveEncoder
