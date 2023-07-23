package es.eriktorr
package realworld.shared.adapter.rest

import realworld.shared.adapter.rest.FakeHealthService.HealthServiceState
import realworld.shared.adapter.rest.ServiceName
import realworld.shared.application.HealthConfig
import realworld.shared.application.HealthConfig.{LivenessPath, ReadinessPath}

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
