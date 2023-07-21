package es.eriktorr
package realworld.domain.model

import realworld.shared.refined.Types.ValidEmail

import io.github.iltotore.iron.*

opaque type Email <: String :| ValidEmail = String :| ValidEmail

object Email:
  def apply(value: String :| ValidEmail): Email = value
