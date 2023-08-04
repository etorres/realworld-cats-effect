package es.eriktorr
package realworld.adapter.rest.request

import realworld.adapter.rest.request.RegisterNewUserRequest.User
import realworld.shared.Secret

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class RegisterNewUserRequest(user: User)

object RegisterNewUserRequest:
  final case class User(email: String, password: Secret[String], username: String)

  given registerNewUserRequestJsonDecoder: Decoder[RegisterNewUserRequest] = deriveDecoder

  given registerNewUserRequestJsonEncoder: Encoder[RegisterNewUserRequest] = deriveEncoder
