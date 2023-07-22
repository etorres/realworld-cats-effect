package es.eriktorr
package realworld.shared.data.error

import cats.data.NonEmptyChain

final case class ValidationErrors(validationErrors: NonEmptyChain[ValidationError])
    extends ValidationError(
      validationErrors
        .map(_.getMessage)
        .toNonEmptyList
        .toList
        .mkString("(", ";", ")"),
    )
