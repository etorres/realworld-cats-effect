package es.eriktorr
package realworld.shared.data.error

abstract class ValidationError(message: String, cause: Option[Throwable] = Option.empty[Throwable])
    extends HandledError(message, cause)
