package es.eriktorr
package realworld.common.data.refined

import io.github.iltotore.iron.DescribedAs
import io.github.iltotore.iron.constraint.any.Not
import io.github.iltotore.iron.constraint.numeric.{GreaterEqual, LessEqual}
import io.github.iltotore.iron.constraint.string.{Blank, Match, StartWith}

object Constraints:
  type Between[Min, Max] = GreaterEqual[Min] & LessEqual[Max]

  type JdbcUrl = DescribedAs[StartWith["jdbc:"], "Should start with jdbc:"]

  type NonEmptyString =
    DescribedAs[Not[Blank], "Should contain at least one non-whitespace character"]

  type NonNegative =
    DescribedAs[GreaterEqual[0], "Should be a positive counting number or zero"]

  /** Regular Expression by RFC 5322 for Email Validation.
    *
    * @see
    *   [[https://www.rfc-editor.org/info/rfc5322 RFC 5322 - Internet Message Format]]
    */
  type ValidEmail = DescribedAs[Match[
    "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$",
  ], "Should be a valid email"]

  type ValidSlug = DescribedAs[Match["^[a-z][a-z0-9-]*[a-z0-9]$"], "Should be a valid slug"]

  type ValidTag = DescribedAs[Match["^[a-z][a-z0-9-]*[a-z0-9]$"], "Should be a valid tag"]

  type UrlPathSegment = DescribedAs[Match["^/[0-9a-zA-Z_-]+"], "Should be a valid URL path segment"]
