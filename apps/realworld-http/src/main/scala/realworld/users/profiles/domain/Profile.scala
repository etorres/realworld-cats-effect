package es.eriktorr
package realworld.users.profiles.domain

import realworld.users.core.domain.User.Username

import java.net.URI

final case class Profile(
    username: Username,
    bio: Option[String],
    image: Option[URI],
    following: Boolean,
)
