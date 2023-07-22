package es.eriktorr
package realworld.adapter.rest.response

import realworld.adapter.rest.response.UserResponse.User
import realworld.domain.model.User as UserModel
import realworld.shared.Secret

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import io.github.arainko.ducktape.*

import java.net.URI

final case class UserResponse(user: User)

object UserResponse:
  final private[response] case class User(
      email: String,
      token: Option[Secret[String]],
      username: String,
      bio: String,
      image: Option[URI],
  )

  given userResponseDecoder: Decoder[UserResponse] = deriveDecoder

  given userResponseEncoder: Encoder[UserResponse] = deriveEncoder

  def from(user: UserModel): UserResponse = UserResponse(user.into[User].transform())
