package es.eriktorr
package realworld.domain.model

import realworld.shared.Secret

import java.net.URI

final case class User(
    email: Email,
    token: Secret[String],
    username: String,
    bio: String,
    image: Option[URI],
)
