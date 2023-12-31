package es.eriktorr
package realworld.common.db

import realworld.common.db.JdbcMigrator.MigrationFailed
import realworld.common.data.error.HandledError

import cats.effect.IO
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateErrorResult
import org.typelevel.log4cats.Logger

import javax.sql.DataSource

final class JdbcMigrator(dataSource: DataSource)(using logger: Logger[IO]):
  import scala.language.unsafeNulls

  def migrate: IO[Unit] =
    IO.blocking {
      val flyway = Flyway
        .configure()
        .dataSource(dataSource)
        .failOnMissingLocations(true)
        .load()
      flyway.migrate()
    }.flatMap:
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
