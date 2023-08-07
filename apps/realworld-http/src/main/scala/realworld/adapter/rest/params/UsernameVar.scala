package es.eriktorr
package realworld.adapter.rest.params

import realworld.domain.model.User.Username

object UsernameVar:
  def unapply(value: String): Option[Username] = Username.from(value).toOption
