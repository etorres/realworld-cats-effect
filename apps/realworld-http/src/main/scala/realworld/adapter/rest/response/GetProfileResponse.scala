package es.eriktorr
package realworld.adapter.rest.response

import realworld.domain.model.Profile

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class GetProfileResponse(profile: Profile)

object GetProfileResponse:
  given getProfileResponseJsonDecoder: Decoder[GetProfileResponse] = deriveDecoder

  given getProfileResponseJsonEncoder: Encoder[GetProfileResponse] = deriveEncoder
