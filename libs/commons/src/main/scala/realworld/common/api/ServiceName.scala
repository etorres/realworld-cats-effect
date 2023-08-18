package es.eriktorr
package realworld.common.api

import realworld.common.data.refined.Constraints.NonEmptyString

import io.github.iltotore.iron.*

opaque type ServiceName <: String :| NonEmptyString = String :| NonEmptyString

object ServiceName:
  def apply(x: String :| NonEmptyString): ServiceName = x
