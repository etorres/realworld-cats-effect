package es.eriktorr
package realworld.common.db.mappers

import realworld.common.Secret

import cats.Show
import doobie.Meta

trait SecretDoobieMapper:
  given secretDoobieMapper[T](using meta: Meta[T], show: Show[T]): Meta[Secret[T]] =
    meta.imap(Secret.apply)(_.value)

object SecretDoobieMapper extends SecretDoobieMapper
