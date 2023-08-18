package es.eriktorr
package realworld.users.profiles.api.params

import realworld.users.core.domain.User.Username

object UsernameVar:
  def unapply(value: String): Option[Username] = Username.from(value).toOption
