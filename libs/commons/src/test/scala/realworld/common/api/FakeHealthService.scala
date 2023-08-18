package es.eriktorr
package realworld.common.api

import realworld.application.HealthConfig
import realworld.application.HealthConfig.{LivenessPath, ReadinessPath}
import realworld.common.api.FakeHealthService.HealthServiceState
import realworld.common.api.ServiceName

import cats.effect.{IO, Ref}
import io.github.iltotore.iron.autoRefine

final class FakeHealthService(stateRef: Ref[IO, HealthServiceState]) extends HealthService:
  override def isReady: IO[Boolean] = IO.pure(true)

  override def livenessPath: LivenessPath = HealthConfig.defaultLivenessPath

  override def markReady: IO[Unit] = stateRef.update(_.copy(true))

  override def markUnready: IO[Unit] = stateRef.update(_.copy(false))

  override def readinessPath: ReadinessPath = HealthConfig.defaultReadinessPath

  override def serviceName: ServiceName = ServiceName("ServiceName")

object FakeHealthService:
  final case class HealthServiceState(ready: Boolean)

  object HealthServiceState:
    val unready: HealthServiceState = HealthServiceState(false)
