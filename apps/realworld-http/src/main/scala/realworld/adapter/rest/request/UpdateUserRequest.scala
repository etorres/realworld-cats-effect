package es.eriktorr
package realworld.adapter.rest.request

import realworld.adapter.rest.BaseRestController.Transformer
import realworld.adapter.rest.request.UpdateUserRequest.User
import realworld.domain.model.Password.PlainText
import realworld.domain.model.User.Username
import realworld.domain.model.UserWithPassword.UserWithPlaintextPassword
import realworld.domain.model.{Email, Password, User as UserModel}
import realworld.shared.Secret
import realworld.shared.data.refined.StringExtensions.toUri

import cats.implicits.{catsSyntaxTuple3Semigroupal, toTraverseOps}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class UpdateUserRequest(user: User)

object UpdateUserRequest:
  final case class User(
      email: String,
      password: Secret[String],
      username: String,
      bio: Option[String],
      image: Option[String],
  )

  given updatedUserRequestJsonDecoder: Decoder[UpdateUserRequest] = deriveDecoder

  given updatedUserRequestJsonEncoder: Encoder[UpdateUserRequest] = deriveEncoder

  given registerNewUserRequestTransformer
      : Transformer[UpdateUserRequest, UserWithPlaintextPassword] =
    import scala.language.unsafeNulls
    (request: UpdateUserRequest) =>
      (
        Email.from(request.user.email),
        Username.from(request.user.username),
        request.user.image.traverse(_.toUri),
      ).mapN { case (email, username, uri) =>
        UserModel(email, None, username, request.user.bio, uri)
      }.andThen(user =>
        Password
          .from[PlainText](request.user.password.value)
          .map(UserWithPlaintextPassword(user, _)),
      )
