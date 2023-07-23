package es.eriktorr
package realworld.adapter.rest.response

import realworld.domain.model.{Email, User}

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class UserLoginResponse(user: User)

object UserLoginResponse:
  given userLoginResponseDecoder: Decoder[UserLoginResponse] = deriveDecoder

  given userLoginResponseEncoder: Encoder[UserLoginResponse] = deriveEncoder
