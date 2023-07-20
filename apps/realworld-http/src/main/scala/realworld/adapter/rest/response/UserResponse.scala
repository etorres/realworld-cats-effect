package es.eriktorr
package realworld.adapter.rest.response

import realworld.adapter.rest.response.UserResponse.User
import realworld.shared.Secret

import cats.implicits.catsSyntaxTuple5Semigroupal
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor, Json}

import java.net.URI

final case class UserResponse(user: User)

object UserResponse:
  final private[response] case class User(
      email: String,
      token: Secret[String],
      username: String,
      bio: String,
      image: URI,
  )

  given userDecoder: Decoder[User] = (cursor: HCursor) =>
    (
      cursor.downField("email").as[String],
      cursor.downField("token").as[String].map(Secret.apply),
      cursor.downField("username").as[String],
      cursor.downField("bio").as[String],
      cursor.downField("image").as[URI],
    ).mapN(User.apply)

  given userEncoder: Encoder[User] = (user: User) =>
    Json.obj(
      ("email", Json.fromString(user.email)),
      ("token", Json.fromString(user.email)),
      ("username", Json.fromString(user.email)),
      ("bio", Json.fromString(user.email)),
      ("image", Json.fromString(user.email)),
    )

  given userResponseDecoder: Decoder[UserResponse] = (cursor: HCursor) =>
    cursor.downField("user").as[User].map(UserResponse.apply)

  given userResponseEncoder: Encoder[UserResponse] = (response: UserResponse) =>
    Json.obj(("user", response.user.asJson))
