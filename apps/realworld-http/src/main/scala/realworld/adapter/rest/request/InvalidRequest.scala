package es.eriktorr
package realworld.adapter.rest.request

import realworld.shared.data.error.HandledError

final case class InvalidRequest(cause: Throwable)
    extends HandledError("Invalid request", Some(cause))
