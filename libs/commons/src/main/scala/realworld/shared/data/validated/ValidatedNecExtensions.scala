package es.eriktorr
package realworld.shared.data.validated

import realworld.shared.data.error.ValidationErrors
import realworld.shared.data.validated.ValidatedNecExtensions.AllErrorsOr

import cats.data.{Validated, ValidatedNec}
import cats.effect.IO
import cats.implicits.catsSyntaxEither

trait ValidatedNecExtensions[A]:
  extension (maybeA: AllErrorsOr[A])
    def either: Either[? <: Throwable, A]
    def eitherMessage: Either[String, A]
    def orFail: A
    def validated: IO[A]

object ValidatedNecExtensions:
  type AllErrorsOr[A] = ValidatedNec[String, A]

  given validatedNecTo[A]: ValidatedNecExtensions[A] with
    extension (maybeA: AllErrorsOr[A])
      def either: Either[? <: Throwable, A] = maybeA match
        case Validated.Valid(value) => Right(value)
        case Validated.Invalid(errors) => Left(ValidationErrors(errors))

      def eitherMessage: Either[String, A] = maybeA.either.leftMap(_.getMessage.nn)

      @SuppressWarnings(Array("org.wartremover.warts.Throw"))
      def orFail: A = maybeA match
        case Validated.Valid(value) => value
        case Validated.Invalid(errors) => throw ValidationErrors(errors)

      def validated: IO[A] = maybeA match
        case Validated.Valid(value) => IO.pure(value)
        case Validated.Invalid(errors) => IO.raiseError(ValidationErrors(errors))
