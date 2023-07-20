package es.eriktorr
package realworld.adapter.rest.request

import realworld.adapter.rest.request.UserLoginRequest.User
import realworld.domain.model.UserCredentials
import realworld.domain.model.UserCredentials.Email
import realworld.shared.Secret

import cats.implicits.catsSyntaxTuple2Semigroupal
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.github.arainko.ducktape.*
import io.github.iltotore.iron.refine

final case class UserLoginRequest(user: User)

object UserLoginRequest:
  final private[request] case class User(email: String, password: Secret[String])

  given userDecoder: Decoder[User] = (cursor: HCursor) =>
    (
      cursor.downField("email").as[String],
      cursor.downField("password").as[String].map(Secret.apply),
    ).mapN(User.apply)

  given userEncoder: Encoder[User] = (user: User) =>
    Json.obj(
      ("email", Json.fromString(user.email)),
      ("password", Json.fromString(user.password.value)),
    )

  given userLoginRequestDecoder: Decoder[UserLoginRequest] = (cursor: HCursor) =>
    cursor.downField("user").as[User].map(UserLoginRequest.apply)

  given userLoginRequestEncoder: Encoder[UserLoginRequest] = (request: UserLoginRequest) =>
    Json.obj(("user", request.user.asJson))

  extension (request: UserLoginRequest)
    def toUserCredentials: UserCredentials =
      request
        .into[UserCredentials]
        .transform(
          Field.computed(_.email, x => Email(x.user.email.refine)),
          Field.computed(_.password, _.user.password),
        )
