package es.eriktorr
package realworld.shared.refined

import io.github.iltotore.iron.DescribedAs
import io.github.iltotore.iron.constraint.any.Not
import io.github.iltotore.iron.constraint.numeric.{GreaterEqual, LessEqual}
import io.github.iltotore.iron.constraint.string.{Blank, Match, StartWith}

object Types:
  type Between[Min, Max] = GreaterEqual[Min] & LessEqual[Max]

  type JdbcUrl = DescribedAs[StartWith["jdbc:"], "Should start with jdbc:"]

  type NonEmptyString =
    DescribedAs[Not[Blank], "Should contain at least one non-whitespace character"]

  /** Regular Expression by RFC 5322 for Email Validation.
    *
    * @see
    *   [[https://www.rfc-editor.org/info/rfc5322 RFC 5322 - Internet Message Format]]
    */
  type ValidEmail = Match["^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$"]

  type UrlPathSegment = DescribedAs[Match["^/[0-9a-zA-Z_-]+"], "Should be a valid URL path segment"]
