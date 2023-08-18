package es.eriktorr
package realworld.articles.core.domain

import realworld.articles.core.domain.Moment.When
import realworld.common.data.validated.ValidatedNecExtensions.{validatedNecTo, AllErrorsOr}

import cats.implicits.catsSyntaxValidatedIdBinCompat0
import org.tpolecat.typename.{typeName, TypeName}

import java.time.LocalDateTime

final case class Moment[A <: When] private (value: LocalDateTime)

object Moment:
  sealed trait When
  sealed trait Created extends When
  sealed trait Updated extends When

  def from[A <: When](value: LocalDateTime)(using
      typeNameA: TypeName[A],
  ): AllErrorsOr[Moment[A]] = typeNameA.value match
    case t if t == typeName[Created] => Moment(value).validNec
    case t if t == typeName[Updated] => Moment(value).validNec
    case other => s"Unsupported moment type: $other".invalidNec

  def unsafeFrom[A <: When](value: LocalDateTime)(using typeNameA: TypeName[A]): Moment[A] =
    from[A](value).orFail
