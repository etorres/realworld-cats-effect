package es.eriktorr
package realworld.domain.model

import scala.util.control.NoStackTrace

@SuppressWarnings(Array("org.wartremover.warts.Null"))
sealed abstract class Error(
    message: String,
    cause: Option[Throwable] = Option.empty[Throwable],
) extends NoStackTrace:
  import scala.language.unsafeNulls
  override def getCause: Throwable = cause.orNull
  override def getMessage: String = message

object Error:
  final case class InvalidPassword(message: String) extends Error(message)
  final case class InvalidCredentials(email: Email)
      extends Error(s"Invalid credentials provided with email: $email")
