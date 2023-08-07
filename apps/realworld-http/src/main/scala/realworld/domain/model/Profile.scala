package es.eriktorr
package realworld.domain.model

import realworld.domain.model.User.Username

import java.net.URI

final case class Profile(
    username: Username,
    bio: Option[String],
    image: Option[URI],
    following: Boolean,
)
