package es.eriktorr
package realworld.shared.data.refined

import realworld.shared.data.validated.ValidatedNecExtensions.AllErrorsOr

import cats.implicits.catsSyntaxValidatedIdBinCompat0

import java.net.URI
import scala.util.{Failure, Success, Try}

object StringExtensions:
  extension (value: String)
    def toUri: AllErrorsOr[URI] = Try(URI(value)) match
      case Failure(exception) => exception.getMessage.nn.invalidNec
      case Success(value) => value.validNec
