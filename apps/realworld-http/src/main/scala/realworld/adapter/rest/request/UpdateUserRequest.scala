package es.eriktorr
package realworld.adapter.rest.request

import realworld.adapter.rest.BaseRestController.Transformer
import realworld.adapter.rest.request.UpdateUserRequest.User
import realworld.domain.model.Password.PlainText
import realworld.domain.model.User.Username
import realworld.domain.model.UserWithPassword.UserWithPlaintextPassword
import realworld.domain.model.{Email, Password, User as UserModel}
import realworld.shared.Secret

import cats.implicits.{catsSyntaxTuple3Semigroupal, catsSyntaxValidatedIdBinCompat0}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import java.net.URI
import scala.util.{Failure, Success, Try}

final case class UpdateUserRequest(user: User)

object UpdateUserRequest:
  final case class User(
      email: String,
      password: Secret[String],
      username: String,
      bio: Option[String],
      image: Option[String],
  )

  given updateUserRequestJsonDecoder: Decoder[UpdateUserRequest] = deriveDecoder

  given updateUserRequestJsonEncoder: Encoder[UpdateUserRequest] = deriveEncoder

  given registerNewUserRequestTransformer
      : Transformer[UpdateUserRequest, UserWithPlaintextPassword] =
    import scala.language.unsafeNulls
    (request: UpdateUserRequest) =>
      (
        Email.from(request.user.email),
        Username.from(request.user.username),
        Try(request.user.image.map(URI(_))) match
          case Failure(exception) => exception.getMessage.invalidNec
          case Success(value) => value.validNec,
      ).mapN { case (email, username, uri) =>
        UserModel(email, None, username, request.user.bio, uri)
      }.andThen(user =>
        Password
          .from[PlainText](request.user.password.value)
          .map(password => UserWithPlaintextPassword(user, password)),
      )
