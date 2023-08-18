package es.eriktorr
package realworld.application

import realworld.application.HealthConfig.{LivenessPath, ReadinessPath}
import realworld.common.data.refined.Constraints.UrlPathSegment

import cats.Show
import cats.data.ValidatedNel
import com.monovore.decline.Argument
import io.github.iltotore.iron.*
import io.github.iltotore.iron.cats.*

final case class HealthConfig(livenessPath: LivenessPath, readinessPath: ReadinessPath)

object HealthConfig:
  opaque type LivenessPath <: String :| UrlPathSegment = String :| UrlPathSegment
  object LivenessPath:
    def apply(value: String :| UrlPathSegment): LivenessPath = value

  opaque type ReadinessPath <: String :| UrlPathSegment = String :| UrlPathSegment
  object ReadinessPath:
    def apply(value: String :| UrlPathSegment): ReadinessPath = value

  val defaultLivenessPath: LivenessPath = LivenessPath.apply("/healthz".refine)
  val defaultReadinessPath: ReadinessPath = ReadinessPath.apply("/ready".refine)

  given Show[HealthConfig] = Show.show(config =>
    s"liveness-path: ${config.livenessPath}, readiness-path: ${config.readinessPath}",
  )

  given livenessPathArgument: Argument[LivenessPath] = new Argument[LivenessPath]:
    override def read(string: String): ValidatedNel[String, LivenessPath] =
      string.refineValidatedNel[UrlPathSegment].map(LivenessPath.apply)

    override def defaultMetavar: String = "path"

  given readinessPathArgument: Argument[ReadinessPath] = new Argument[ReadinessPath]:
    override def read(string: String): ValidatedNel[String, ReadinessPath] =
      string.refineValidatedNel[UrlPathSegment].map(ReadinessPath.apply)

    override def defaultMetavar: String = "path"
