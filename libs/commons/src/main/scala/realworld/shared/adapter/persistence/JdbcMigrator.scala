package es.eriktorr
package realworld.shared.adapter.persistence

import realworld.shared.adapter.persistence.JdbcMigrator.MigrationFailed
import realworld.shared.data.error.HandledError

import cats.effect.IO
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateErrorResult
import org.typelevel.log4cats.Logger

import javax.sql.DataSource

final class JdbcMigrator(dataSource: DataSource)(using logger: Logger[IO]):
  import scala.language.unsafeNulls

  def migrate: IO[Unit] =
    IO.blocking(
      Flyway
        .configure()
        .dataSource(dataSource)
        .failOnMissingLocations(true)
        .load()
        .migrate(),
    ).flatMap:
      case errorResult: MigrateErrorResult => IO.raiseError(MigrationFailed(errorResult))
      case other => IO.unit
    .handleErrorWith: error =>
        logger.error(error)("Database migration failed")

object JdbcMigrator:
  import scala.language.unsafeNulls

  sealed abstract class JdbcMigrationError(message: String) extends HandledError(message)

  final case class MigrationFailed(errorResult: MigrateErrorResult)
      extends JdbcMigrationError(
        s"message: ${errorResult.error.message}, stackTrace: ${errorResult.error.stackTrace}",
      )
