package es.eriktorr
package realworld.domain.model

import realworld.domain.model.Password.{CipherText, Format, PlainText}
import realworld.shared.data.validated.ValidatedNecExtensions.AllErrorsOr

import cats.implicits.catsSyntaxValidatedIdBinCompat0
import io.github.arainko.ducktape.*
import org.tpolecat.typename.{typeName, TypeName}

sealed trait UserWithPassword[A <: Format]:
  val user: User
  val password: Password[A]

object UserWithPassword:
  final case class UserWithPlaintextPassword(user: User, password: Password[PlainText])
      extends UserWithPassword[PlainText]

  final case class UserWithHashPassword(user: User, password: Password[CipherText])
      extends UserWithPassword[CipherText]

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def from[A <: Format, B <: UserWithPassword[A]](user: User, password: Password[A])(using
      typeNameA: TypeName[A],
  ): AllErrorsOr[B] = typeNameA.value match
    case t if t == typeName[PlainText] =>
      val plaintextPassword = password.asInstanceOf[Password[PlainText]]
      UserWithPlaintextPassword(user, plaintextPassword).asInstanceOf[B].validNec
    case t if t == typeName[CipherText] =>
      val hashPassword = password.asInstanceOf[Password[CipherText]]
      UserWithHashPassword(user, hashPassword).asInstanceOf[B].validNec
    case other => s"Unsupported user with password type: $other".invalidNec

  extension (userWithPassword: UserWithPlaintextPassword)
    def withHash(hash: Password[CipherText]): UserWithHashPassword =
      userWithPassword.into[UserWithHashPassword].transform(Field.const(_.password, hash))
