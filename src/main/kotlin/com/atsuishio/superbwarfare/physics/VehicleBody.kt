package com.atsuishio.superbwarfare.physics

import com.atsuishio.superbwarfare.Mod
import cn.solarmoon.spark_core.physics.PhysicsSuspensionSolver
import cn.solarmoon.spark_core.physics.SideSuspensionInput
import cn.solarmoon.spark_core.physics.VehicleControlMode
import cn.solarmoon.spark_core.physics.PhysicsHost
import cn.solarmoon.spark_core.physics.body.*
import cn.solarmoon.spark_core.physics.level.PhysicsLevel
import cn.solarmoon.spark_core.physics.level.PhysicsLevelNotRunningException
import cn.solarmoon.spark_core.physics.query.sweepBody
import com.atsuishio.superbwarfare.data.vehicle.subdata.WheelRole
import com.atsuishio.superbwarfare.data.vehicle.subdata.WheelSide
import com.jme3.bullet.RotationOrder
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.SolverMode
import com.jme3.bullet.collision.PhysicsCollisionObject
import com.jme3.bullet.collision.shapes.CollisionShape
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.collision.shapes.CompoundCollisionShape
import com.jme3.bullet.collision.shapes.CylinderCollisionShape
import com.jme3.bullet.collision.shapes.SphereCollisionShape
import com.jme3.bullet.joints.New6Dof
import com.jme3.bullet.joints.motors.MotorParam
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Matrix3f
import com.jme3.math.Vector3f
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.*

internal data class DriveFrame(
    val sequence: Long = 0,
    val mode: VehicleControlMode = VehicleControlMode.PASSIVE,
    /** Normalised demand for the left and right ground contact groups. */
    val leftDrive: Double = 0.0,
    val rightDrive: Double = 0.0,
    /**
     * Longitudinal demand of the whole vehicle, independent of the steering
     * split. A differential turn must never be able to cancel the throttle by
     * saturating one side, so this is carried explicitly instead of being
     * re-derived from [leftDrive]/[rightDrive].
     */
    val centerDrive: Double = 0.0,
    val targetSpeed: Double = 0.0,
    /** Signed steering demand. Positive is a right turn. */
    val steering: Double = 0.0,
    /** Turret yaw servo target in radians, relative to the chassis. */
    val turretYaw: Float = 0f,
    /** Barrel pitch servo target in radians, relative to the turret. */
    val barrelPitch: Float = 0f,
    /** Local linear acceleration commands for flight vehicles. */
    val acceleration: Vec3 = Vec3.ZERO,
    /** Desired local angular velocity for flight attitude control. */
    val angularVelocity: Vec3 = Vec3.ZERO,
    val aerodynamicLift: Boolean = false,
    val engineActive: Boolean = false,
    val awake: Boolean = false,
    val brake: Boolean = false,
)

internal data class RigidState(val frame: Long, val sequence: Long, val position: Vec3,
    val rotation: Quaternionf, val velocity: Vec3, val angularVelocity: Vec3, val grounded: Boolean)
internal data class VehicleImpact(val otherOwner: PhysicsHost?, val contact: ContactSnapshot)
data class VehiclePhysicsDiagnostic(val frame: Long, val waitingForTerrain: Boolean, val position: Vec3,
    val velocity: Vec3)

data class VehicleStabilityObservation(val samples: Int, val maxDisplacement: Double,
    val maxHorizontalSpeed: Double, val minSupportContacts: Int, val terrainLost: Boolean,
    /** Largest horizontal spin (rad/s) over the observation window. */
    val maxAngularSpeed: Double = 0.0,
    /** Largest substep change of the horizontal spin (rad/s), a jitter probe. */
    val maxAngularDelta: Double = 0.0,
    /** Substep count whose spin change exceeded the jitter threshold. */
    val angularJitterSteps: Int = 0)

/** All native fields below are confined to PhysicsLevel's worker. */
internal class VehicleBody(
    val physics: PhysicsLevel,
    val ownerHost: PhysicsHost,
    val spec: VehiclePhysicsSpec,
    initial: RigidState,
) {
    val clientSide: Boolean get() = spec.clientSide
    val half: Vec3 get() = spec.half
    val localCenter: Vec3 get() = spec.localCenter
    val pivot: Double get() = spec.pivot
    val settings: VehiclePhysicsSettings get() = spec.settings
    val mass: Float get() = spec.massKg
    val anchored: Boolean get() = spec.anchored
    val groundVehicle: Boolean get() = spec.groundVehicle
    val trackedVehicle: Boolean get() = spec.trackedVehicle
    val flightVehicle: Boolean get() = spec.flightVehicle
    @Volatile var state = initial
    @Volatile var command = DriveFrame(turretYaw = spec.initialTurretYaw, barrelPitch = spec.initialBarrelPitch)
    @Volatile var disposed = false
    @Volatile var ready = false
    /** Set when the tracked mechanism cannot be assembled. The hull must not become ready. */
    @Volatile var assemblyFailed = false
        private set
    @Volatile var authoritative: RigidState? = null
    @Volatile var authoritativeRender: VehiclePhysicsRenderState? = null
    val impacts = ConcurrentLinkedQueue<VehicleImpact>()
    private val impulses = ConcurrentLinkedQueue<Pair<Vec3, Vec3?>>()
    private val impulseTraces = java.util.concurrent.atomic.AtomicInteger()
    private val angularImpulses = ConcurrentLinkedQueue<Vec3>()

    /** World up, never the tilted chassis axis; see the wheel oleo pair. */
    private val WORLD_UP = Vector3f(0f, 1f, 0f)
    lateinit var body: PhysicsRigidBody
        private set
    lateinit var box: BoxCollisionShape
        private set
    @Volatile var renderState = VehiclePhysicsRenderState()
        private set
    @Volatile var diagnostic: VehiclePhysicsDiagnostic? = null
        private set
    @Volatile var stabilityObservation: VehicleStabilityObservation? = null
        private set
    private var observationOrigin: Vec3? = null
    private var observationSteps = 0
    private var lastObservationSpin = 0.0
    private var observationSpinPrimed = false
    /**
     * S2 tracked running gear, present only for an engine type of TRACK.
     *
     * The chassis body above is the caller-owned hull; this runtime owns the
     * wheels, mounts, runs and end constraints. It is created after the hull is
     * in the world and closed before the hull is removed.
     */
    @Volatile var tracked: com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleRuntime? = null
        private set
    /** Idempotent teardown result; also used by the physics layer to wait. */
    private val closeFuture = java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CompletableFuture<Void>?>()
    /**
     * S4 drive demand, published from the game thread each tick.
     *
     * Kept as plain data so the physics thread never reads entity state.
     */
    @Volatile var trackedCommand: com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleRuntime.DriveCommand =
        com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleRuntime.DriveCommand.COAST

    fun startStabilityObservation(steps: Int) {
        physics.submitImmediateTask {
            observationOrigin = null
            observationSteps = steps.coerceIn(1, 3600)
            // Prime on the first physics sample. The request arrives from the
            // game thread and can be consumed one substep after the moving
            // vehicle has changed its yaw rate; reading here would still make
            // that scheduling gap look like a turn spike.
            lastObservationSpin = 0.0
            observationSpinPrimed = false
            stabilityObservation = VehicleStabilityObservation(0, 0.0, 0.0, Int.MAX_VALUE, false)
        }
    }
    internal val isGrounded: Boolean get() = grounded
    private var grounded = false
    private var bodyGrounded = false
    private var leftSuspension = SideSuspensionState()
    private var rightSuspension = SideSuspensionState()
    private var terrainWaiting = false
    private var lastSequence = 0L
    private var renderLeftTravel = 0f
    private var renderRightTravel = 0f
    private var renderInitialized = false
    private var currentContactSupport = 0f
    private var previousContactSupport = 0f
    private val wheels = ArrayList<WheelRuntime>()
    private var heldMotion: List<Triple<PhysicsRigidBody, Vector3f, Vector3f>> = emptyList()
    private var hasReadyTerrain = false
    private var terrainIssue: String? = null
    private val diagnosticSteps = Integer.getInteger("superbwarfare.physics.traceSteps", 0).coerceIn(0, 3600)
    private var diagnosticSamples = 0
    private var turretBody: PhysicsRigidBody? = null
    private var turretJoint: New6Dof? = null
    private var barrelBody: PhysicsRigidBody? = null
    private var barrelJoint: New6Dof? = null
    /** Total mass reserved for the independent wheel bodies. */
    private val wheelMassTotal: Float get() = spec.massPlan.wheelMassKg
    /** Total mass reserved for the constrained turret and barrel bodies. */
    private val articulationMassTotal: Float get() = spec.massPlan.articulationMassKg
    /**
     * Hull mass. It is a plan value, never `mass - running gear` recomputed here:
     * the plan is the only place that knows whether a tracked running gear or a
     * wheeled share already owns part of the authored mass.
     */
    private val chassisMass: Float get() = spec.massPlan.chassisMassKg
    /** Last immutable render projection accepted from the server on a client. */
    private var clientAuthoritativeRender: VehiclePhysicsRenderState? = null
    private var clientRenderTarget: VehiclePhysicsRenderState? = null
    private var clientRenderCurrent: VehiclePhysicsRenderState? = null

    private data class WheelSuspensionState(
        var travel: Float = 0f,
        var force: Float = 0f,
        var grounded: Boolean = false,
        var missedSteps: Int = 0,
        var normal: Vector3f = Vector3f.UNIT_Y.clone(),
    )

    private class WheelRuntime(
        val station: WheelStationSpec,
        val body: PhysicsRigidBody,
        val joint: New6Dof,
        val probe: SphereCollisionShape,
        var suspension: WheelSuspensionState = WheelSuspensionState(),
        var steeringAngle: Float = 0f,
        var spin: Float = 0f,
    )

    private data class SideSuspensionState(
        var travel: Float = 0f,
        var force: Float = 0f,
        var grounded: Boolean = false,
        var missedSteps: Int = 0,
        var normal: Vector3f = Vector3f.UNIT_Y.clone(),
    )

    /** Match VehicleEntity's pivoted Default transform exactly. */
    fun center(origin: Vec3, rotation: Quaternionf) =
        origin.add(0.0, pivot, 0.0).add(rotation.rotate(localCenter))
    fun origin(center: Vec3, rotation: Quaternionf) =
        center.subtract(rotation.rotate(localCenter)).add(0.0, -pivot, 0.0)

    fun markAssemblyFailed() {
        assemblyFailed = true
        ready = false
    }

    fun closeInProgress(): Boolean = closeFuture.get()?.isDone == false

    fun closeNeedsRetry(): Boolean = closeFuture.get()?.isCompletedExceptionally == true

    fun create() {
        physics.assertPhysicsThread()
        if (disposed) return
        if (trackedVehicle && !clientSide) {
            val reason = TrackedPhysicsAdmission.rejectReason(spec.trackedSpec, spec.massPlan)
            if (reason != null) {
                Mod.LOGGER.error("Tracked vehicle {} refused before body creation: {}", spec.diagnosticName, reason)
                markAssemblyFailed()
                return
            }
        }
        box = BoxCollisionShape(half.bullet()).apply {
            // Keep the physical hull flush with the authored OBB/model.  The
            // default Bullet margin is large enough to create visible gaps on
            // small vehicles and can make the chassis appear to float.
            margin = .001f
        }
        val shape: CollisionShape = if (spec.collisionParts.isEmpty()) {
            if (settings.centerOfMassOffset == Vec3.ZERO) box else CompoundCollisionShape().apply {
                addChildShape(box, settings.centerOfMassOffset.scale(-1.0).bullet())
            }
        } else {
            CompoundCollisionShape(spec.collisionParts.size).apply {
                spec.collisionParts.forEach { part ->
                    val child = BoxCollisionShape(part.half.bullet()).apply { margin = .001f }
                    val rotation = Matrix3f()
                    part.rotation.bullet().toRotationMatrix(rotation)
                    addChildShape(child, part.center.bullet(), rotation)
                }
            }
        }
        // Tracked vehicles collide only with their local discrete Bullet
        // terrain mesh. The raw block AABB group is reserved for other physical
        // objects and must never reach a tracked hull. Non-tracked vehicles
        // keep the existing block terrain path.
        val chassisMask = if (trackedVehicle) {
            com.atsuishio.superbwarfare.physics.tracked.TrackedTerrainCollision.TERRAIN_MESH or
                CollisionGroups.CHASSIS or CollisionGroups.PHYSICS_BODY or CollisionGroups.WHEEL or
                CollisionGroups.TRACK_LINK
        } else {
            CollisionGroups.DRIVABLE or CollisionGroups.CHASSIS or CollisionGroups.PHYSICS_BODY or
                CollisionGroups.WHEEL or CollisionGroups.TRACK_LINK
        }
        body = PhysicsRigidBody(shape, if (anchored) 0f else chassisMass).apply {
            name = "sbw_chassis"
            owner = ownerHost
            collisionGroup = CollisionGroups.CHASSIS
            setCollideWithGroups(chassisMask)
            // Only supported conventional ground vehicles enter this path.
            isContactResponse = !clientSide
            friction = settings.friction.coerceIn(0f, 10f)
            restitution = if (groundVehicle) 0f else settings.restitution.coerceIn(0f, 1f)
            setDamping(settings.linearDamping.coerceIn(0f, 1f), settings.angularDamping.coerceIn(0f, 1f))
            setSleepingThresholds(0.05f, 0.05f)
            ccdMotionThreshold = minOf(half.x, half.y, half.z).toFloat().coerceAtLeast(0.05f) * .5f
            ccdSweptSphereRadius = ccdMotionThreshold * .8f
            isKinematic = clientSide
            setProtectGravity(true)
            // Gravity is applied explicitly in preStep for every fixed Bullet
            // substep. Keep native space gravity disabled so it cannot be
            // replaced or duplicated during body insertion.
            setGravity(Vector3f())
            // The axle constraints own the chassis/wheel relationship. The
            // only same-owner terrain contact allowed is the private local
            // mesh used by tracked vehicles.
            collideWithOwnerGroups = if (trackedVehicle) {
                com.atsuishio.superbwarfare.physics.tracked.TrackedTerrainCollision.TERRAIN_MESH
            } else {
                CollisionGroups.NONE
            }
        }

        // The chassis must be in Spark before its joints are registered.  The
        // wheel bodies are created after the chassis body, but before reset so
        // the initial pose is applied to the complete constrained assembly.
        physics.addBody(body)
        if (!trackedVehicle && spec.wheelStations.isNotEmpty()) createWheelBodies()
        if (!clientSide) createArticulation()
        reset(state)
        if (trackedVehicle && !clientSide) createTrackedRuntime()
        body.onResolvedContact { event ->
            // A banked vehicle's support normal must be compared with its
            // current chassis up axis, rather than world Y.
            val chassisUp = body.getPhysicsRotation(null)
                .toRotationMatrix().mult(Vector3f.UNIT_Y, null).normalize()
            val normal = event.snapshot.normal
            val support = normal.x * chassisUp.x + normal.y * chassisUp.y + normal.z * chassisUp.z
            if (support > 0.45 || (groundVehicle && abs(normal.y) > .45f)) bodyGrounded = true
            if (support > 0.45f && event.snapshot.normalImpulse.isFinite()) {
                currentContactSupport += event.snapshot.normalImpulse * PhysicsLevel.TPS.toFloat()
            }
            if (!clientSide && event.snapshot.normalImpulse > mass * 0.05)
                impacts.add(VehicleImpact(event.other.owner, event.snapshot))
        }
        body.onPrePhysicsTick { preStep() }
        body.onPostPhysicsTick {
            previousContactSupport = currentContactSupport
            currentContactSupport = 0f
            grounded = if (trackedVehicle) {
                bodyGrounded || tracked?.beltSupported() == true
            } else {
                grounded || bodyGrounded
            }
            if (!clientSide) {
                state = readState(physics.tickCount, lastSequence)
            }
            if (!trackedVehicle && wheels.isNotEmpty()) updateWheelProjection()
            val visualScale = settings.suspensionVisualScale.coerceIn(0f, 1f)
            val visualLimit = settings.suspensionVisualMaxOffset.coerceIn(.005f, 1f)
            val targetLeftTravel = renderLeftTravel.coerceIn(-visualLimit, visualLimit)
            val targetRightTravel = renderRightTravel.coerceIn(-visualLimit, visualLimit)
            // Rendered suspension follows the physical target over several
            // substeps so a contact edge cannot teleport a wheel bone.
            renderLeftTravel += (targetLeftTravel - renderLeftTravel) * .24f
            renderRightTravel += (targetRightTravel - renderRightTravel) * .24f
            val authoritativeRender = if (clientSide) clientRenderCurrent else null
            val trackedRender = if (trackedVehicle && !clientSide) tracked?.captureRenderState(physics.tickCount) else null
            renderState = authoritativeRender ?: VehiclePhysicsRenderState(
                physicsFrame = physics.tickCount,
                leftTravel = renderLeftTravel,
                rightTravel = renderRightTravel,
                leftForce = leftSuspension.force,
                rightForce = rightSuspension.force,
                wheels = trackedRender?.wheelStates() ?: wheels.map { wheel ->
                    WheelRenderState(
                        index = wheel.station.index,
                        side = wheel.station.side,
                        suspensionTravel = (wheel.suspension.travel * visualScale)
                            .coerceIn(-visualLimit, visualLimit),
                        steeringAngle = wheel.steeringAngle,
                        spin = wheel.spin,
                        grounded = wheel.suspension.grounded,
                        steerable = wheel.station.steerable,
                    )
                },
                grounded = grounded,
                tracked = trackedRender,
            )
            trace("step")
            diagnostic = VehiclePhysicsDiagnostic(physics.tickCount, terrainWaiting, state.position, state.velocity)
            stabilityObservation?.takeIf { it.samples < observationSteps }?.let { previous ->
                val origin = observationOrigin ?: state.position.also { observationOrigin = it }
                val spinVector = body.getAngularVelocity(null)
                val spinAxis = body.getPhysicsRotation(null).toRotationMatrix().mult(Vector3f.UNIT_Y, null)
                val signedSpin = spinVector.dot(spinAxis).toDouble()
                val spin = abs(signedSpin)
                val previousSpin = lastObservationSpin
                val spinDelta = if (observationSpinPrimed) abs(signedSpin - previousSpin) else 0.0
                lastObservationSpin = signedSpin
                observationSpinPrimed = true
                stabilityObservation = VehicleStabilityObservation(previous.samples + 1,
                    max(previous.maxDisplacement, state.position.subtract(origin).horizontalDistance()),
                    max(previous.maxHorizontalSpeed, state.velocity.horizontalDistance()),
                    min(previous.minSupportContacts, if (grounded) 1 else 0),
                    previous.terrainLost || terrainWaiting,
                    max(previous.maxAngularSpeed, spin),
                    max(previous.maxAngularDelta, spinDelta),
                    previous.angularJitterSteps + if (spinDelta > .08) 1 else 0)
            }
        }
        // A tracked server hull stays not-ready until its running gear commits.
        // A missing spec is an admission failure, not a chassis-only vehicle.
        if (!trackedVehicle || clientSide) ready = true
    }

    fun reset(value: RigidState) {
        if (!::body.isInitialized || !value.position.finite() || !value.velocity.finite()) return
        // Native reads and writes stay on the physics thread. The game thread
        // only queues this call; it must not wait, or it can deadlock the worker.
        if (!physics.isPhysicsThread()) {
            physics.submitTask("vehicle reset ${spec.diagnosticName}") { reset(value) }
            return
        }
        if (!clientSide) {
            heldMotion.forEach { (part, _, _) -> part.isKinematic = false }
            heldMotion = emptyList()
            terrainWaiting = false
            hasReadyTerrain = false
        }
        body.clearForces()
        body.setPhysicsLocation(center(value.position, value.rotation).bullet())
        body.setPhysicsRotation(value.rotation.bullet())
        body.setLinearVelocity((if (anchored) Vec3.ZERO else value.velocity).bullet())
        body.setAngularVelocity((if (anchored) Vec3.ZERO else value.angularVelocity).bullet())
        if (wheels.isNotEmpty()) {
            val chassisRotation = value.rotation.bullet()
            val chassisVelocity = (if (anchored) Vec3.ZERO else value.velocity).bullet()
            val chassisAngular = (if (anchored) Vec3.ZERO else value.angularVelocity).bullet()
            val chassisCenter = body.getPhysicsLocation(null)
            val chassisMatrix = chassisRotation.toRotationMatrix()
            wheels.forEach { wheel ->
                val local = wheel.station.center.bullet()
                val relative = chassisMatrix.mult(local, null)
                wheel.body.setPhysicsLocation(chassisCenter.add(relative))
                wheel.body.setPhysicsRotation(chassisRotation)
                wheel.body.setLinearVelocity(chassisVelocity.add(chassisAngular.cross(relative, null)))
                wheel.body.setAngularVelocity(chassisAngular)
                wheel.suspension = WheelSuspensionState(-settings.suspensionTravelDown)
                wheel.steeringAngle = 0f
                wheel.spin = 0f
            }
        }
        resetArticulation(value)
        previousContactSupport = 0f
        currentContactSupport = 0f
        leftSuspension = SideSuspensionState(-settings.suspensionTravelDown)
        rightSuspension = SideSuspensionState(-settings.suspensionTravelDown)
        grounded = value.grounded
        bodyGrounded = value.grounded
        if (!renderInitialized) {
            val scale = settings.suspensionVisualScale.coerceIn(0f, 1f)
            renderLeftTravel = (-settings.suspensionTravelDown * scale)
                .coerceIn(-settings.suspensionVisualMaxOffset, settings.suspensionVisualMaxOffset)
            renderRightTravel = (-settings.suspensionTravelDown * scale)
                .coerceIn(-settings.suspensionVisualMaxOffset, settings.suspensionVisualMaxOffset)
            renderInitialized = true
        }
        body.activate(true)
        state = value
        // The hull pose is already the reset pose. Re-seat the running gear in
        // this same physics-thread call so a later task cannot observe the hull
        // at the new pose and the belts at the old one.
        tracked?.resetNow()
        trace("reset")
    }

    /** Snap the articulated bodies onto the freshly placed chassis pose. */
    private fun resetArticulation(value: RigidState) {
        val chassisRotation = value.rotation.bullet()
        val chassisMatrix = chassisRotation.toRotationMatrix()
        val chassisCenter = body.getPhysicsLocation(null)
        val linear = if (anchored) Vector3f() else value.velocity.bullet()
        val angular = if (anchored) Vector3f() else value.angularVelocity.bullet()
        turretBody?.let { turret ->
            val pivot = spec.turret?.pivot ?: Vec3.ZERO
            val relative = chassisMatrix.mult(pivot.bullet(), null)
            turret.clearForces()
            turret.setPhysicsLocation(chassisCenter.add(relative))
            val yaw = com.jme3.math.Quaternion().fromAngleNormalAxis(command.turretYaw, Vector3f.UNIT_Y)
            turret.setPhysicsRotation(chassisRotation.mult(yaw))
            turret.setLinearVelocity(linear.add(angular.cross(relative, null)))
            turret.setAngularVelocity(angular)
            turret.activate(true)
        }
        barrelBody?.let { barrel ->
            val turret = turretBody ?: body
            val parentRotation = turret.getPhysicsRotation(null)
            val pivot = spec.barrel?.pivot ?: Vec3.ZERO
            val relative = parentRotation.toRotationMatrix().mult(pivot.bullet(), null)
            barrel.clearForces()
            barrel.setPhysicsLocation(turret.getPhysicsLocation(null).add(relative))
            val pitch = com.jme3.math.Quaternion().fromAngleNormalAxis(command.barrelPitch, Vector3f.UNIT_X)
            barrel.setPhysicsRotation(parentRotation.mult(pitch))
            barrel.setLinearVelocity(turret.getLinearVelocity(null).add(turret.getAngularVelocity(null).cross(relative, null)))
            barrel.setAngularVelocity(turret.getAngularVelocity(null))
            barrel.activate(true)
        }
    }

    fun readState(frame: Long, sequence: Long): RigidState {
        val raw = body.getPhysicsRotation(null).joml()
        val q = if (raw.isFinite && raw.lengthSquared() > .0001f) raw.normalize() else Quaternionf()
        val position = body.getPhysicsLocation(null).minecraft()
        val velocity = body.getLinearVelocity(null).minecraft().let { if (it.finite()) it.limited(500.0) else Vec3.ZERO }
        val angular = body.getAngularVelocity(null).minecraft().let { if (it.finite()) it.limited(30.0) else Vec3.ZERO }
        return RigidState(frame, sequence, origin(position, q), q, velocity, angular, grounded)
    }

    fun impulse(value: Vec3, point: Vec3? = null) {
        if (!anchored && value.finite() && (point == null || point.finite())) {
            if (diagnosticSteps > 0 && value.lengthSqr() > 1.0 && impulseTraces.getAndIncrement() < 64)
                com.atsuishio.superbwarfare.Mod.LOGGER.info(
                    "[vehicle-impulse] {} value={} point={} source={}", spec.diagnosticName, value, point,
                    Throwable().stackTrace.take(12).joinToString(" <- "))
            impulses.add(value to point)
        }
    }

    /** Queue a delta in angular velocity; the physics thread converts it to a torque impulse. */
    fun angularImpulse(value: Vec3) {
        if (!anchored && value.finite()) angularImpulses.add(value.limited(30.0))
    }

    private fun preStep() {
        if (disposed) return
        if (clientSide) {
            if (physics.tickCount % 3L == 1L) {
                authoritative?.let { state = it; authoritative = null }
                reset(state)
                authoritativeRender?.let { value ->
                    val scale = settings.suspensionVisualScale.coerceIn(.001f, 1f)
                    leftSuspension = SideSuspensionState(value.leftTravel / scale, value.leftForce,
                        value.grounded, 0, Vector3f.UNIT_Y.clone())
                    rightSuspension = SideSuspensionState(value.rightTravel / scale, value.rightForce,
                        value.grounded, 0, Vector3f.UNIT_Y.clone())
                    grounded = value.grounded
                    clientRenderTarget = value
                    if (clientRenderCurrent == null) clientRenderCurrent = value
                    authoritativeRender = null
                }
            }
            clientRenderTarget?.let { target ->
                clientRenderCurrent = (clientRenderCurrent ?: target).interpolate(target, .35f, spec.trackAnimationLength)
            }
            return
        }
        // Trace before queued inputs and before the custom contact solver so a
        // velocity discontinuity can be attributed without changing dynamics.
        trace("pre-step")
        val velocity = body.getLinearVelocity(null).minecraft()
        val bound = body.boundingBox(null)
        val extent = bound.getExtent(null)
        val pos = body.getPhysicsLocation(null)
        var readyBox = net.minecraft.world.phys.AABB(pos.x-extent.x.toDouble(), pos.y-extent.y.toDouble(), pos.z-extent.z.toDouble(),
            pos.x+extent.x.toDouble(), pos.y+extent.y.toDouble(), pos.z+extent.z.toDouble())
        readyBox = readyBox
            .expandTowards(velocity.scale(0.1))
            .inflate(.1, (settings.suspensionTravelUp + settings.suspensionTravelDown + .5).toDouble(), .1)
        // A tracked vehicle's terrain authority is its private Bullet mesh,
        // never the global block-AABB cache. Non-tracked vehicles keep the
        // existing block terrain readiness gate.
        val trackedTerrainReady = !trackedVehicle || tracked?.terrainReady() == true
        terrainIssue = if (trackedVehicle) {
            if (trackedTerrainReady) null else "tracked-mesh"
        } else {
            physics.terrainManager.readinessIssue(readyBox)
        }
        val terrainReady = trackedTerrainReady && (trackedVehicle || terrainIssue == null)
        if (!terrainReady) {
            if (trackedVehicle) {
                // The tracked runtime owns this wait. It factor-locks the
                // chassis, wheels and belt runs (`TrackedTerrainWaitController`)
                // instead of parking them, because a tracked assembly already has
                // mounts and run-end joints and Bullet rejects a joint between two
                // kinematic bodies. Converting the hull here would leave the whole
                // mechanism invalid rather than merely paused, and it would also
                // fight the runtime's own release, which restores factors the hull
                // never had.
                if (!terrainWaiting) trace("tracked-terrain-wait")
                terrainWaiting = true
                grounded = false
                return
            }
            val capture = !terrainWaiting && !hasReadyTerrain
            if (capture) {
                heldMotion = assembly().filter { !it.isStatic }.map { Triple(it, it.getLinearVelocity(null), it.getAngularVelocity(null)) }
                heldMotion.forEach { (part, _, _) ->
                    part.clearForces()
                    part.isKinematic = true
                    part.setLinearVelocity(Vector3f())
                    part.setAngularVelocity(Vector3f())
                }
                trace("initial-terrain-wait")
            }
            if (!terrainWaiting && hasReadyTerrain) trace("runtime-terrain-loss")
            terrainWaiting = true
            grounded = false
            if (!hasReadyTerrain) return
        }
        if (terrainReady && terrainWaiting) {
            heldMotion.forEach { (part, linear, angular) ->
                part.isKinematic = false
                part.setLinearVelocity(linear)
                part.setAngularVelocity(angular)
                part.activate(true)
            }
            heldMotion = emptyList()
            terrainWaiting = false
            trace("terrain-ready")
        }
        if (terrainReady) hasReadyTerrain = true
        if (!anchored) {
            body.applyCentralForce(Vector3f(0f, -chassisMass * settings.gravity.toFloat(), 0f))
            applyArticulationGravity()
            wheels.forEach { it.body.applyCentralForce(Vector3f(0f, -it.body.mass * settings.gravity.toFloat(), 0f)) }
        }
        while (true) {
            val (j, point) = impulses.poll() ?: break
            if (point == null) body.applyCentralImpulse(j.bullet())
            else body.applyImpulse(j.bullet(), point.bullet().subtract(body.getPhysicsLocation(null)))
            body.activate(true)
        }
        while (true) {
            val delta = angularImpulses.poll() ?: break
            val inverse = body.getInverseInertiaLocal(null)
            val torqueImpulse = Vector3f(
                if (inverse.x > 1e-6f) delta.x.toFloat() / inverse.x else 0f,
                if (inverse.y > 1e-6f) delta.y.toFloat() / inverse.y else 0f,
                if (inverse.z > 1e-6f) delta.z.toFloat() / inverse.z else 0f,
            )
            body.applyTorqueImpulse(torqueImpulse)
            body.activate(true)
        }
        stabilizeGroundAngularVelocity()
        trace("pre-contact")
        bodyGrounded = false
        val input = command
        if (!anchored) {
            if (terrainReady && groundVehicle && settings.suspensionEnabled && !trackedVehicle && spec.suspensionSides.isNotEmpty()) {
                applyWheelSuspension(body, physics.world)
            }
            // Tracked running gear owns its own bilateral rows: one per compiled
            // station, applied as internal pairs between hull and wheel body.
            if (trackedVehicle) {
                // The runtime publishes the plain wheel pose from its own
                // pre-step hook, which also runs for a bare runtime with no
                // VehicleBody. Publishing here as well would duplicate the
                // native reads for no benefit.
                //
                // The runtime publishes the previous contact window from that
                // same hook. Calling beginSupportWindow here would reset it
                // before the solver samples it.
                tracked?.applySuspension()
                // S4: drive, brake and rolling resistance. The command was
                // published on the game thread; the targets are computed here.
                tracked?.setDriveCommand(trackedCommand)
                tracked?.applyDrive()
            }
            // Suspension contact is sampled before traction so the first input
            // frame after placement can use the actual support state.
            applyDrive(body, physics.world, input)
            driveArticulation(input)
        }
        trace("forces")
        lastSequence = input.sequence
        if (!groundVehicle || !settings.suspensionEnabled) grounded = false
    }

    /** Keep invalid angular states from poisoning the physics worker. */
    private fun stabilizeGroundAngularVelocity() {
        if (!groundVehicle || anchored) return
        val angular = body.getAngularVelocity(null)
        if (!angular.x.isFinite() || !angular.y.isFinite() || !angular.z.isFinite()) {
            body.setAngularVelocity(Vector3f())
            return
        }
        val limit = settings.angularVelocityLimit.coerceAtLeast(.25f)
        if (angular.length() > limit && angular.length().isFinite()) {
            body.setAngularVelocity(angular.normalize().mult(limit))
        }
    }

    fun applyDrive(target: PhysicsRigidBody, space: PhysicsSpace, input: DriveFrame) {
        val q = target.getPhysicsRotation(null)
        // A body that is still falling must not sleep just because the driver
        // has not entered the vehicle yet. Gravity and suspension are the
        // simulation input in that state.
        // Wake a sleeping hull once. Calling activate every substep resets
        // Bullet's deactivation timer, so a supported vehicle never settles.
        val shouldWake = input.awake || input.acceleration.lengthSqr() > .01 || (groundVehicle && !grounded)
        if (shouldWake && !target.isActive) target.activate(true)
        if (groundVehicle) {
            // WHEEL traction is the wheel bodies' native friction reacting to
            // the spin motor. Tracked vehicles have no drive path until the
            // MCSP-style TrackRun runtime is wired.
            if (!trackedVehicle) applyWheelDrive(input)
        } else {
            val localAcceleration = input.acceleration
            target.applyCentralForce(q.toRotationMatrix().mult(localAcceleration.bullet(), null).multLocal(mass))
            if (input.aerodynamicLift && input.engineActive && spec.fixedWing) {
                val localVelocity = q.inverse().toRotationMatrix().mult(target.getLinearVelocity(null), null)
                val speed = max(localVelocity.z, 0f)
                val liftRatio = ((speed - 4f) / 12f).coerceIn(0f, 1.2f)
                val throttleRatio = (localAcceleration.z / 8.0).coerceIn(0.0, 1.5)
                val liftAcceleration = settings.gravity.toFloat() * liftRatio * throttleRatio
                if (liftAcceleration > 0f) {
                    target.applyCentralForce(q.toRotationMatrix().mult(Vector3f(0f, (liftAcceleration * mass).toFloat(), 0f), null))
                }
            }
            val desired = q.toRotationMatrix().mult(input.angularVelocity.bullet(), null)
            val error = desired.subtract(target.getAngularVelocity(null))
            val localError = q.inverse().toRotationMatrix().mult(error, null)
            val inverse = target.getInverseInertiaLocal(null)
            val gain = 18f
            val torque = Vector3f(
                if (inverse.x > 1e-6f) localError.x * gain / inverse.x else 0f,
                if (inverse.y > 1e-6f) localError.y * gain / inverse.y else 0f,
                if (inverse.z > 1e-6f) localError.z * gain / inverse.z else 0f)
            val maxTorque = settings.maxSteeringTorque.coerceAtLeast(1f)
            if (torque.lengthSquared() > maxTorque * maxTorque) torque.normalize().multLocal(maxTorque)
            target.applyTorque(q.toRotationMatrix().mult(torque, null))
        }
    }

    private fun contactVelocity(part: PhysicsRigidBody?, point: Vector3f): Vector3f =
        if (part == null || part.isStatic) Vector3f() else part.getLinearVelocity(null)
            .add(part.getAngularVelocity(null).cross(point.subtract(part.getPhysicsLocation(null)), null))

    private fun predictedContactVelocity(part: PhysicsRigidBody?, point: Vector3f, dt: Float): Vector3f {
        val velocity = contactVelocity(part, point)
        if (part == null || !part.isDynamic) return velocity
        val linearDelta = part.totalAppliedForce(null).mult(dt / part.mass)
        val angularDelta = part.getInverseInertiaWorld(null).mult(part.totalAppliedTorque(null), null).mult(dt)
        return velocity.add(linearDelta).add(angularDelta.cross(point.subtract(part.getPhysicsLocation(null)), null))
    }

    private fun inverseEffectiveMass(part: PhysicsRigidBody?, point: Vector3f, axis: Vector3f): Float {
        if (part == null || !part.isDynamic) return 0f
        val r = point.subtract(part.getPhysicsLocation(null))
        val angular = part.getInverseInertiaWorld(null).mult(r.cross(axis, null), null).cross(r, null)
        return 1f / part.mass + axis.dot(angular)
    }

    /**
     * Build the S2 tracked running gear for this hull.
     *
     * The compiled spec is required: a tracked vehicle without one must not be
     * given a wheeled suspension as a fallback. Assembly is queued on the physics
     * thread by the runtime itself, so this returns immediately.
     */
    private fun createTrackedRuntime() {
        val compiled = spec.trackedSpec ?: run {
            Mod.LOGGER.error(
                "Tracked vehicle {} has no compiled running gear; it stays out of the physics path",
                spec.diagnosticName,
            )
            return
        }
        val runtime = com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleRuntime(
            physics,
            ownerHost,
            compiled,
            body,
            Vector3f(0f, -settings.gravity.toFloat(), 0f),
        )
        tracked = runtime
        val hullParts = if (spec.collisionParts.isNotEmpty()) {
            spec.collisionParts
        } else {
            listOf(CollisionPart(half, settings.centerOfMassOffset.scale(-1.0)))
        }
        runtime.setDebugCollisionParts(hullParts)
        runtime.create().whenComplete { _, error ->
            if (disposed) return@whenComplete
            if (error != null) {
                Mod.LOGGER.error("Tracked running gear failed to assemble for {}", spec.diagnosticName, error)
                // Keep the failed runtime so closeAsync can finish its rollback.
                markAssemblyFailed()
            } else {
                ready = true
            }
        }
    }

    /** Create one dynamic wheel body and one constrained axle for every WHEEL station. */
    private fun createWheelBodies() {
        physics.assertPhysicsThread()
        val wheelMass = (wheelMassTotal / spec.wheelStations.size.coerceAtLeast(1))
            .coerceIn(.05f, wheelMassTotal.coerceAtLeast(.05f))
        spec.wheelStations.forEach { station ->
            val radius = station.radius.toFloat().coerceIn(.05f, 2f)
            val halfWidth = station.halfWidth.toFloat().coerceIn(.05f, 2f)
            val shape = CylinderCollisionShape(Vector3f(halfWidth, radius, radius), 0).apply { margin = .001f }
            val wheel = PhysicsRigidBody(shape, wheelMass).apply {
                name = "sbw_wheel_${station.index}"
                owner = ownerHost
                collisionGroup = CollisionGroups.WHEEL
                setCollideWithGroups(CollisionGroups.DRIVABLE or CollisionGroups.PHYSICS_BODY or CollisionGroups.CHASSIS)
                collideWithOwnerGroups = CollisionGroups.NONE
                isContactResponse = !clientSide
                friction = settings.friction.coerceIn(0f, 10f)
                restitution = 0f
                setDamping((settings.linearDamping * 1.5f).coerceIn(0f, 1f), settings.angularDamping.coerceIn(0f, 1f))
                setSleepingThresholds(.02f, .02f)
                ccdMotionThreshold = radius * .5f
                ccdSweptSphereRadius = radius * .45f
                isKinematic = clientSide
                setProtectGravity(true)
                setGravity(Vector3f())
            }
            val joint = New6Dof(
                body,
                wheel,
                station.center.bullet(),
                Vector3f(),
                Matrix3f(),
                Matrix3f(),
                RotationOrder.XYZ,
            ).also { dof ->
                PhysicsJoints.lock(dof, PhysicsJoints.LIN_X)
                PhysicsJoints.lock(dof, PhysicsJoints.LIN_Z)
                PhysicsJoints.limit(dof, PhysicsJoints.LIN_Y,
                    -settings.suspensionTravelDown.coerceAtLeast(.001f),
                    settings.suspensionTravelUp.coerceAtLeast(.001f))
                PhysicsJoints.free(dof, PhysicsJoints.ANG_X)
                if (station.steerable) {
                    PhysicsJoints.limit(dof, PhysicsJoints.ANG_Y,
                        -settings.wheelSteeringAngle.coerceAtLeast(.01f),
                        settings.wheelSteeringAngle.coerceAtLeast(.01f))
                } else {
                    PhysicsJoints.lock(dof, PhysicsJoints.ANG_Y)
                }
                PhysicsJoints.lock(dof, PhysicsJoints.ANG_Z)
                dof.enableSpring(PhysicsJoints.LIN_Y, false)
            }
            val probe = SphereCollisionShape(radius).apply { margin = .001f }
            val runtime = WheelRuntime(station, wheel, joint, probe)
            wheels += runtime
            physics.addBody(wheel)
            physics.addJoint(joint)
        }
    }

    /**
     * Create the constrained turret and barrel bodies. Both are children of the
     * chassis hierarchy: the turret yaws about the authored turret pivot, the
     * barrel pitches about the authored barrel pivot inside the turret frame.
     * The bodies exist only on the server, because the renderer keeps reading
     * the authoritative entity angles instead of the raw Bullet rotation.
     */
    private fun createArticulation() {
        physics.assertPhysicsThread()
        if (anchored || !settings.articulationEnabled) return
        val turretSpec = spec.turret ?: return
        val barrelSpec = spec.barrel
        // Mass comes from the plan, not from a second local split, so the hull
        // and the articulated bodies cannot disagree about who owns what.
        val turretMass = spec.massPlan.turretMassKg
        val barrelMass = spec.massPlan.barrelMassKg
        if (turretMass <= 0f) return

        val turretShape = articulationShape(turretSpec) ?: return
        val turret = articulationBody("sbw_turret", turretShape, turretMass)
        turretBody = turret
        turretJoint = articulationJoint(body, turret, turretSpec)
        physics.addBody(turret)
        physics.addJoint(turretJoint!!)

        if (barrelSpec != null && barrelMass > 0f) {
            val barrelShape = articulationShape(barrelSpec) ?: return
            val barrel = articulationBody("sbw_barrel", barrelShape, barrelMass)
            barrelBody = barrel
            barrelJoint = articulationJoint(turret, barrel, barrelSpec)
            physics.addBody(barrel)
            physics.addJoint(barrelJoint!!)
        }
    }

    private fun articulationJoint(parent: PhysicsRigidBody, child: PhysicsRigidBody, articulation: ArticulationSpec): New6Dof =
        New6Dof(
            parent,
            child,
            articulation.pivot.bullet(),
            Vector3f(),
            Matrix3f(),
            Matrix3f(),
            RotationOrder.XYZ,
        ).also { dof ->
            PhysicsJoints.lock(dof, PhysicsJoints.LIN_X)
            PhysicsJoints.lock(dof, PhysicsJoints.LIN_Y)
            PhysicsJoints.lock(dof, PhysicsJoints.LIN_Z)
            val free = when (articulation.axis) {
                0 -> PhysicsJoints.ANG_X
                2 -> PhysicsJoints.ANG_Z
                else -> PhysicsJoints.ANG_Y
            }
            PhysicsJoints.limit(dof, free, articulation.lowerLimit.toFloat(), articulation.upperLimit.toFloat())
            if (free != PhysicsJoints.ANG_X) PhysicsJoints.lock(dof, PhysicsJoints.ANG_X)
            if (free != PhysicsJoints.ANG_Y) PhysicsJoints.lock(dof, PhysicsJoints.ANG_Y)
            if (free != PhysicsJoints.ANG_Z) PhysicsJoints.lock(dof, PhysicsJoints.ANG_Z)
            dof.enableSpring(PhysicsJoints.LIN_X, false)
            dof.enableSpring(PhysicsJoints.LIN_Y, false)
            dof.enableSpring(PhysicsJoints.LIN_Z, false)
        }

    private fun articulationShape(articulation: ArticulationSpec): CollisionShape? {
        if (articulation.parts.isEmpty()) return null
        return CompoundCollisionShape(articulation.parts.size).apply {
            articulation.parts.forEach { part ->
                val child = BoxCollisionShape(part.half.bullet()).apply { margin = .001f }
                val rotation = Matrix3f()
                part.rotation.bullet().toRotationMatrix(rotation)
                addChildShape(child, part.center.bullet(), rotation)
            }
        }
    }

    private fun articulationBody(name: String, shape: CollisionShape, bodyMass: Float): PhysicsRigidBody =
        PhysicsRigidBody(shape, bodyMass.coerceAtLeast(.1f)).apply {
            this.name = name
            owner = ownerHost
            collisionGroup = CollisionGroups.CHASSIS
            setCollideWithGroups(if (trackedVehicle) {
                com.atsuishio.superbwarfare.physics.tracked.TrackedTerrainCollision.TERRAIN_MESH or
                    CollisionGroups.PHYSICS_BODY or CollisionGroups.CHASSIS or CollisionGroups.WHEEL
            } else {
                CollisionGroups.DRIVABLE or CollisionGroups.PHYSICS_BODY or
                    CollisionGroups.CHASSIS or CollisionGroups.WHEEL
            })
            // Same-owner contacts (the chassis and the wheels) must never fight
            // the revolute constraint; terrain and other vehicles still collide.
            collideWithOwnerGroups = if (trackedVehicle) {
                com.atsuishio.superbwarfare.physics.tracked.TrackedTerrainCollision.TERRAIN_MESH
            } else {
                CollisionGroups.NONE
            }
            isContactResponse = true
            friction = settings.friction.coerceIn(0f, 10f)
            restitution = 0f
            setDamping(settings.linearDamping.coerceIn(0f, 1f), settings.angularDamping.coerceIn(0f, 1f))
            setSleepingThresholds(.05f, .05f)
            ccdMotionThreshold = minOf(half.x, half.y, half.z).toFloat().coerceAtLeast(.05f) * .25f
            ccdSweptSphereRadius = ccdMotionThreshold * .5f
            setProtectGravity(true)
            setGravity(Vector3f())
        }

    /** Servo the turret and barrel toward the authoritative aiming angles. */
    private fun driveArticulation(input: DriveFrame) {
        if (anchored) return
        turretJoint?.let { joint ->
            val motor = joint.getRotationMotor(1)
            motor.setServoEnabled(true)
            motor.setMotorEnabled(true)
            motor.set(MotorParam.ServoTarget, input.turretYaw)
            motor.set(MotorParam.MaxMotorForce, settings.articulationServoForce.coerceAtLeast(0f))
        }
        barrelJoint?.let { joint ->
            val motor = joint.getRotationMotor(0)
            motor.setServoEnabled(true)
            motor.setMotorEnabled(true)
            motor.set(MotorParam.ServoTarget, input.barrelPitch)
            motor.set(MotorParam.MaxMotorForce, settings.articulationServoForce.coerceAtLeast(0f))
        }
    }

    /** The articulated bodies carry their reserved share of the vehicle mass. */
    private fun applyArticulationGravity() {
        turretBody?.let { body ->
            body.applyCentralForce(Vector3f(0f, -body.mass * settings.gravity.toFloat(), 0f))
            body.activate(true)
        }
        barrelBody?.let { body ->
            body.applyCentralForce(Vector3f(0f, -body.mass * settings.gravity.toFloat(), 0f))
            body.activate(true)
        }
    }

    /** Per-wheel terrain contact and explicit oleo force. */
    private fun applyWheelSuspension(target: PhysicsRigidBody, space: PhysicsSpace) {
        if (wheels.isEmpty()) return
        val chassisRotation = target.getPhysicsRotation(null)
        val matrix = chassisRotation.toRotationMatrix()
        val up = matrix.mult(Vector3f.UNIT_Y, null).normalize()
        if (up.y < .15f) {
            wheels.forEach { it.suspension = WheelSuspensionState(-settings.suspensionTravelDown) }
            grounded = false
            return
        }
        val chassisPosition = target.getPhysicsLocation(null)
        val velocity = target.getLinearVelocity(null)
        val angular = target.getAngularVelocity(null)
        val supportCount = wheels.count { it.station.role == WheelRole.SUPPORT }
            .coerceAtLeast(1)
        val effectiveMass = mass * 2f / supportCount
        val mask = CollisionGroups.DRIVABLE or CollisionGroups.PHYSICS_BODY

        wheels.forEach { wheel ->
            val station = wheel.station
            val anchor = chassisPosition.add(matrix.mult(station.center.bullet(), null))
            val from = anchor.add(up.mult(settings.suspensionTravelUp))
            val to = anchor.subtract(up.mult(settings.suspensionTravelDown))
            val hit = sweepBody(space, wheel.probe, from, to, chassisRotation, target, mask)
            val previous = wheel.suspension
            var contact = false
            var travel = -settings.suspensionTravelDown
            var normal = previous.normal
            if (hit != null) {
                val candidate = hit.getHitNormalLocal(null).normalize()
                if (candidate.dot(up) >= .2f) {
                    travel = settings.suspensionTravelUp - hit.hitFraction.coerceIn(0f, 1f) *
                        (settings.suspensionTravelUp + settings.suspensionTravelDown)
                    normal = candidate.clone()
                    contact = true
                }
            } else if (previous.grounded && previous.missedSteps < 2) {
                travel = previous.travel
                contact = true
            }
            if (!contact) {
                wheel.suspension = WheelSuspensionState(travel, 0f, false, previous.missedSteps + 1, normal)
                return@forEach
            }
            val relative = anchor.subtract(chassisPosition)
            val anchorVelocity = velocity.add(angular.cross(relative, null))
            val compressionVelocity = -anchorVelocity.dot(up).coerceIn(
                -settings.suspensionCompressionVelocityLimit,
                settings.suspensionCompressionVelocityLimit,
            )
            val solved = PhysicsSuspensionSolver.solve(
                SideSuspensionInput(true, travel, compressionVelocity, normal.dot(up)),
                effectiveMass,
                settings.gravity.toFloat(),
                settings.suspensionTravelUp,
                settings.suspensionTravelDown,
                settings.suspensionFrequency,
                settings.suspensionCompressionDamping,
                settings.suspensionReboundDamping,
                (settings.suspensionMaxForce / supportCount).coerceAtLeast(1f),
            )
            val force = solved.forceN.coerceAtMost((settings.suspensionMaxForce / supportCount).coerceAtLeast(0f))
            wheel.suspension = WheelSuspensionState(travel, force, solved.grounded, 0, normal)
            // The oleo pair is internal, but a tilted pair couples into the
            // rolling contact and steadily pushes the hull whenever the chassis
            // is pitched (the guide stays chassis-local, the force is the world
            // up the solver names). Keep it vertical so a resting vehicle has
            // no way to propel itself.
            if (force > 0f) PhysicsJoints.applyOleoForce(target, wheel.body, WORLD_UP, force, anchor)
        }
        grounded = wheels.any { it.suspension.grounded }
        val left = wheels.filter { it.station.side == WheelSide.LEFT && it.suspension.grounded }
        val right = wheels.filter { it.station.side == WheelSide.RIGHT && it.suspension.grounded }
        if (left.isNotEmpty() && right.isNotEmpty()) {
            val leftTravel = left.map { it.suspension.travel.coerceAtLeast(0f) }.average().toFloat()
            val rightTravel = right.map { it.suspension.travel.coerceAtLeast(0f) }.average().toFloat()
            val stiffness = mass * .5f * (2f * Math.PI.toFloat() * settings.suspensionFrequency.coerceAtLeast(.1f)).let { it * it }
            val (leftCorrection, rightCorrection) = PhysicsSuspensionSolver.antiRoll(
                leftTravel, rightTravel, stiffness, settings.suspensionAntiRoll,
                (settings.suspensionMaxForce * .25f).coerceAtLeast(1f),
            )
            target.applyForce(up.mult(leftCorrection), matrix.mult(Vector3f(
                (spec.side(WheelSide.LEFT)?.anchorX ?: 0.5).toFloat(), 0f, 0f), null))
            target.applyForce(up.mult(rightCorrection), matrix.mult(Vector3f(
                (spec.side(WheelSide.RIGHT)?.anchorX ?: -0.5).toFloat(), 0f, 0f), null))
        }
    }

    /**
     * Steering servo and wheel spin motor. Traction, braking and lateral grip
     * are the wheel bodies' native contact friction, reacting to the spin the
     * motor commands. No explicit tire force may duplicate that native friction,
     * and the spin enters exactly once (native manifold), never twice.
     *
     * Forward demand drives the wheel forward: positive angular velocity about
     * the chassis X axis, because that is what makes the contact patch move
     * backwards against the ground and the friction push the hull forward.
     * The same positive spin is what `updateWheelProjection` exposes to both
     * renderers and matches the authored belt-loop direction.
     */
    private fun applyWheelDrive(input: DriveFrame) {
        wheels.forEach { wheel ->
            val steering = if (wheel.station.steerable) {
                (-input.steering * settings.wheelSteeringAngle.toDouble() * settings.steeringScale)
                    .coerceIn(-settings.wheelSteeringAngle.toDouble(), settings.wheelSteeringAngle.toDouble()).toFloat()
            } else 0f
            wheel.steeringAngle = steering
            val motor = wheel.joint.getRotationMotor(1)
            motor.setServoEnabled(wheel.station.steerable)
            motor.setMotorEnabled(wheel.station.steerable)
            motor.set(MotorParam.ServoTarget, steering)
            motor.set(MotorParam.MaxMotorForce, settings.wheelSteeringForce.coerceAtLeast(0f))

            val demand = when (wheel.station.side) {
                WheelSide.LEFT -> input.leftDrive
                WheelSide.RIGHT -> input.rightDrive
            }.coerceIn(-1.0, 1.0).toFloat()
            val radius = wheel.station.radius.toFloat().coerceAtLeast(.05f)
            val desiredOmega = demand * input.targetSpeed.toFloat() / radius
            // Coasting (no demand, no brake) leaves the motor at zero force so
            // the wheels roll freely; braking is the driver's explicit command.
            val coasting = !input.brake && kotlin.math.abs(demand) < .001f
            PhysicsJoints.setRollMotor(
                wheel.joint,
                if (input.engineActive) desiredOmega else 0f,
                when {
                    !input.engineActive -> 0f
                    coasting -> 0f
                    else -> settings.wheelDriveTorque.coerceAtLeast(0f)
                },
            )
            if (input.engineActive) wheel.body.activate(true)
            // Bearing/rolling resistance. Bullet exposes no rolling friction
            // for these bodies, so without it a free wheel coasts forever and a
            // settling vehicle can never rest. A load-scaled torque opposing
            // the spin models tire losses without touching traction: the
            // drive/brake motor dwarfs it, coasting drops about a tenth of a
            // m/s^2, and the impulse is capped so it can never reverse or lock
            // the spin.
            if (wheel.suspension.grounded && settings.wheelRollingResistance > 0f) {
                val axle = wheel.body.getPhysicsRotation(null).toRotationMatrix()
                    .mult(Vector3f(1f, 0f, 0f), null)
                val spin = wheel.body.getAngularVelocity(null).dot(axle)
                if (spin.isFinite() && kotlin.math.abs(spin) > 1.0e-3f) {
                    val torque = settings.wheelRollingResistance * wheel.suspension.force * radius
                    val inertia = .5f * wheel.body.mass * radius * radius
                    val impulse = (torque / PhysicsLevel.TPS.toFloat())
                        .coerceAtMost(kotlin.math.abs(spin) * inertia)
                    axle.multLocal(-kotlin.math.sign(spin) * impulse)
                    wheel.body.applyTorqueImpulse(axle)
                }
            }
        }
    }

    private fun updateWheelProjection() {
        if (wheels.isEmpty()) return
        wheels.forEach { wheel ->
            val localAngular = wheel.body.getAngularVelocityLocal(null)
            if (localAngular.x.isFinite()) {
                wheel.spin = (wheel.spin + localAngular.x / PhysicsLevel.TPS.toFloat())
                    .let { ((it + Math.PI.toFloat()) % (Math.PI.toFloat() * 2f) + Math.PI.toFloat() * 2f) % (Math.PI.toFloat() * 2f) - Math.PI.toFloat() }
            }
        }
        val left = wheels.filter { it.station.side == WheelSide.LEFT }
        val right = wheels.filter { it.station.side == WheelSide.RIGHT }
        fun average(values: List<WheelRuntime>, selector: (WheelRuntime) -> Float): Float =
            if (values.isEmpty()) 0f else values.map(selector).average().toFloat()
        renderLeftTravel = average(left) { it.suspension.travel * settings.suspensionVisualScale }
            .coerceIn(-settings.suspensionVisualMaxOffset, settings.suspensionVisualMaxOffset)
        renderRightTravel = average(right) { it.suspension.travel * settings.suspensionVisualScale }
            .coerceIn(-settings.suspensionVisualMaxOffset, settings.suspensionVisualMaxOffset)
    }

    private fun assembly(): List<PhysicsRigidBody> = listOfNotNull(body, turretBody, barrelBody) +
        wheels.map { it.body }

    /** Opt-in, bounded per-substep diagnostics; never read Entity/Level on this worker. */
    private fun trace(reason: String) {
        if (clientSide || diagnosticSamples >= diagnosticSteps || !::body.isInitialized) return
        diagnosticSamples++
        val localVelocity = body.getPhysicsRotation(null).inverse().toRotationMatrix().mult(body.getLinearVelocity(null), null)
        com.atsuishio.superbwarfare.Mod.LOGGER.info(
            "[vehicle-physics] {} frame={} reason={} waiting={} terrainIssue={} active={} pos={} localV={} rotation={} drive={} forces={} torque={} contacts={}",
            spec.diagnosticName, physics.tickCount, reason, terrainWaiting, terrainIssue, body.isActive,
            body.getPhysicsLocation(null), localVelocity, body.getPhysicsRotation(null), command,
            body.totalAppliedForce(null), body.totalAppliedTorque(null),
            wheels.joinToString { "wheel=${it.station.index} travel=${it.suspension.travel} force=${it.suspension.force}" })
    }

    fun close() {
        closeAsync()
    }

    /**
     * Physics-thread teardown used by a level shutdown participant.
     *
     * This does not submit work. The level has already stopped accepting tasks,
     * so [closeAsync] would be rejected and must not be treated as success.
     */
    fun closeNow() {
        physics.assertPhysicsThread()
        synchronized(this) {
            disposed = true
            ready = false
            try {
                tracked?.closeNow()
                removeOwnedHullObjects()
                tracked = null
                lastCloseError = null
                val existing = closeFuture.get()
                if (existing == null) closeFuture.set(java.util.concurrent.CompletableFuture.completedFuture(null))
                else if (!existing.isDone) existing.complete(null)
            } catch (error: Throwable) {
                lastCloseError = error.message ?: error::class.java.simpleName
                closeFuture.get()?.let { if (!it.isDone) it.completeExceptionally(error) }
                throw error
            }
        }
    }

    /**
     * Close this hull and everything constrained to it, in dependency order.
     *
     * The running gear constrains to the hull, so its joints, runs, mounts and
     * wheels must be gone before the chassis body is removed. Returning a future
     * lets [VehiclePhysics] keep the entity managed until the native state is
     * actually gone instead of dropping the only reference that could retry.
     */
    fun closeAsync(): java.util.concurrent.CompletableFuture<Void> {
        synchronized(this) {
            closeFuture.get()?.let { existing ->
                if (!existing.isCompletedExceptionally) return existing
            }
            disposed = true
            ready = false
            val result = java.util.concurrent.CompletableFuture<Void>()
            closeFuture.set(result)

            // Keep [tracked] until teardown succeeds so a failed close can retry
            // the same runtime instead of dropping the only rollback owner.
            val runtime = tracked
            val runtimeStage: java.util.concurrent.CompletableFuture<Void> = if (runtime != null) {
                runtime.closeAsync().handle { _, error ->
                    if (error == null || isLevelShutdown(error)) null else throw error
                }.thenCompose {
                    // A close rejected by shutdown removed nothing. The level's
                    // teardown owns those objects, so wait for it instead of
                    // reporting success over live native state.
                    if (runtime.hasResidual && !physics.isAcceptingTasks()) {
                        physics.closeAsync().thenApply<Void> { null }
                    } else {
                        java.util.concurrent.CompletableFuture.completedFuture(null)
                    }
                }
            } else {
                java.util.concurrent.CompletableFuture.completedFuture(null)
            }

            runtimeStage.thenCompose {
                if (!physics.isAcceptingTasks()) {
                    // Same rule for the hull: level teardown is the barrier.
                    physics.closeAsync().thenApply<Void> { null }
                } else {
                    physics.submitTask("vehicle body teardown ${spec.diagnosticName}") {
                        removeOwnedHullObjects()
                    }.handle { _, error ->
                        if (error == null || isLevelShutdown(error)) null else throw error
                    }
                }
            }.whenComplete { _, error ->
                if (error != null) {
                    lastCloseError = error.message ?: error::class.java.simpleName
                    result.completeExceptionally(error)
                } else {
                    // A stopping level destroys every registered body itself.
                    tracked = null
                    lastCloseError = null
                    result.complete(null)
                }
            }
            return result
        }
    }

    private fun isLevelShutdown(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is PhysicsLevelNotRunningException) return true
            current = current.cause
        }
        return false
    }

    /** Physics thread only. Leaves anything that could not be removed for a retry. */
    private fun removeOwnedHullObjects() {
        physics.assertPhysicsThread()
        val failures = ArrayList<Throwable>()
        val remainingWheels = ArrayList<WheelRuntime>()
        for (wheel in wheels.asReversed()) {
            try {
                physics.removeJoint(wheel.joint)
                physics.removeBody(wheel.body)
            } catch (error: Throwable) {
                failures += error
                remainingWheels += wheel
            }
        }
        wheels.clear()
        wheels.addAll(remainingWheels.asReversed())
        barrelJoint?.let { joint ->
            try {
                physics.removeJoint(joint)
                barrelJoint = null
            } catch (error: Throwable) {
                failures += error
            }
        }
        turretJoint?.let { joint ->
            try {
                physics.removeJoint(joint)
                turretJoint = null
            } catch (error: Throwable) {
                failures += error
            }
        }
        barrelBody?.let { body ->
            try {
                physics.removeBody(body)
                barrelBody = null
            } catch (error: Throwable) {
                failures += error
            }
        }
        turretBody?.let { body ->
            try {
                physics.removeBody(body)
                turretBody = null
            } catch (error: Throwable) {
                failures += error
            }
        }
        if (::body.isInitialized) {
            try {
                physics.removeBody(body)
            } catch (error: Throwable) {
                failures += error
            }
        }
        if (failures.isNotEmpty()) {
            val primary = failures.first()
            failures.drop(1).forEach { primary.addSuppressed(it) }
            throw primary
        }
    }

    /** Last running-gear close failure, kept for diagnostics. */
    @Volatile var lastCloseError: String? = null
        private set
}
