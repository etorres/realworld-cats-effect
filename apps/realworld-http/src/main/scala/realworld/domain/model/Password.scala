package es.eriktorr
package realworld.domain.model

import realworld.domain.model.Password.Format
import realworld.shared.Secret
import realworld.shared.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}

import cats.Show
import cats.implicits.catsSyntaxValidatedIdBinCompat0
import com.password4j.types.Argon2
import com.password4j.{Argon2Function, Password as Password4j}
import org.tpolecat.typename.{typeName, TypeName}

final case class Password[A <: Format] private (value: Secret[String])

object Password:
  import scala.language.unsafeNulls

  sealed trait Format
  sealed trait ClearText extends Format
  sealed trait CipherText extends Format

  def from[A <: Format](value: String)(using
      typeNameA: TypeName[A],
  ): AllErrorsOr[Password[A]] = typeNameA.value match
    case t if t == typeName[ClearText] => Password(Secret(value)).validNec
    case t if t == typeName[CipherText] =>
      if value.length == 159 && value.startsWith(argon2Signature) then
        Password(Secret(value)).validNec
      else "Invalid password format".invalidNec
    case _ => "Unsupported password type".invalidNec

  def unsafeFrom[A <: Format](value: String)(using typeNameA: TypeName[A]): Password[A] =
    from[A](value).orFail

  given passwordShow[A <: Format]: Show[Password[A]] = Show.fromToString

  def cipher(password: Password[ClearText]): AllErrorsOr[Password[CipherText]] =
    Password.from[CipherText](
      Password4j.hash(password.value.value).addRandomSalt().`with`(argon2Function).getResult,
    )

  def check(password: Password[ClearText], hash: Password[CipherText]): Boolean =
    Password4j.check(password.value.value, hash.value.value).`with`(argon2Function)

  private inline val memoryInKib = 12
  private inline val numberOfIterations = 20
  private inline val numberOfThreads = 2
  private inline val outputLength = 32
  private inline def algorithmType = Argon2.ID
  private inline val version = 19

  private inline def argon2Signature =
    s"""$$argon2${algorithmType.toString.toLowerCase}
       |$$v=$version
       |$$m=$memoryInKib,
       |t=$numberOfIterations,
       |p=$numberOfThreads$$""".stripMargin.replaceAll("\\R", "")

  private inline def argon2Function =
    Argon2Function.getInstance(
      memoryInKib,
      numberOfIterations,
      numberOfThreads,
      outputLength,
      algorithmType,
      version,
    )
