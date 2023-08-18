package es.eriktorr
package realworld.application

import realworld.common.data.validated.ValidatedNecExtensions.validatedNecTo
import realworld.users.core.domain.User.Token

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import org.http4s.Credentials.Token as Http4sToken
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRequest, AuthedRoutes, AuthScheme, Request, Response, Status}

object JwtAuthMiddleware:
  private enum AuthError:
    case Forbidden
    case Unauthorized

  private def jwtAuth[A](
      verifier: Token => IO[A],
      defaultValue: Option[A],
  ): Kleisli[IO, Request[IO], Either[AuthError, A]] = Kleisli: request =>
    val authHeader: Option[Authorization] = request.headers.get[Authorization]
    authHeader match
      case Some(value) =>
        value match
          case Authorization(Http4sToken(AuthScheme.Bearer, token)) =>
            for
              token <- Token.from(token).validated
              result <- verifier(token)
                .map(_.asRight)
                .handleErrorWith(_ => IO.pure(AuthError.Forbidden.asLeft))
            yield result
          case _ => IO.pure(AuthError.Unauthorized.asLeft)
      case None =>
        IO.pure(defaultValue match
          case Some(value) => value.asRight
          case None => AuthError.Unauthorized.asLeft,
        )

  private val onFailure: AuthedRoutes[AuthError, IO] = Kleisli {
    (request: AuthedRequest[IO, AuthError]) =>
      request.context match
        case AuthError.Forbidden => OptionT.pure[IO](Response[IO](status = Status.Forbidden))
        case _ => OptionT.pure[IO](Response[IO](status = Status.Unauthorized))
  }

  def jwtAuthMiddleware[A](
      verifier: Token => IO[A],
      defaultValue: Option[A] = None,
  ): AuthMiddleware[IO, A] =
    AuthMiddleware(jwtAuth(verifier, defaultValue), onFailure)
