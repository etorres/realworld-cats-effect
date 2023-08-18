package es.eriktorr
package realworld.users.core.api.request

import realworld.common.Secret
import realworld.common.api.BaseRestController.Transformer
import realworld.common.data.refined.StringExtensions.toUri
import realworld.users.core.api.request.UpdateUserRequest.User
import realworld.users.core.domain.Password.PlainText
import realworld.users.core.domain.User.Username
import realworld.users.core.domain.UserWithPassword.UserWithPlaintextPassword
import realworld.users.core.domain.{Email, Password, User as UserModel}

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
