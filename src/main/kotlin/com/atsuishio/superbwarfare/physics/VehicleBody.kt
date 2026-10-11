package com.atsuishio.superbwarfare.physics

import cn.solarmoon.spark_core.physics.PhysicsSuspensionSolver
import cn.solarmoon.spark_core.physics.SideSuspensionInput
import cn.solarmoon.spark_core.physics.VehicleControlMode
import cn.solarmoon.spark_core.physics.PhysicsHost
import cn.solarmoon.spark_core.physics.body.*
import cn.solarmoon.spark_core.physics.level.PhysicsLevel
import cn.solarmoon.spark_core.physics.query.sweepBody
import com.atsuishio.superbwarfare.data.vehicle.subdata.WheelSide
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.collision.shapes.CollisionShape
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.collision.shapes.CompoundCollisionShape
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
    val targetSpeed: Double = 0.0,
    /** Local linear acceleration commands for flight vehicles. */
    val acceleration: Vec3 = Vec3.ZERO,
    /** Desired local angular velocity for flight attitude control. */
    val angularVelocity: Vec3 = Vec3.ZERO,
    val aerodynamicLift: Boolean = false,
    val engineActive: Boolean = false,
    val awake: Boolean = false,
)

internal data class RigidState(val frame: Long, val sequence: Long, val position: Vec3,
    val rotation: Quaternionf, val velocity: Vec3, val angularVelocity: Vec3, val grounded: Boolean)
internal data class VehicleImpact(val otherOwner: PhysicsHost?, val contact: ContactSnapshot)

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
    @Volatile var command = DriveFrame()
    @Volatile var disposed = false
    @Volatile var ready = false
    @Volatile var needsInitialSync = true
    @Volatile var authoritative: RigidState? = null
    @Volatile var authoritativeRender: VehiclePhysicsRenderState? = null
    val impacts = ConcurrentLinkedQueue<VehicleImpact>()
    private val impulses = ConcurrentLinkedQueue<Pair<Vec3, Vec3?>>()
    private val angularImpulses = ConcurrentLinkedQueue<Vec3>()
    lateinit var body: PhysicsRigidBody
        private set
    lateinit var box: BoxCollisionShape
        private set
    private lateinit var suspensionProbe: BoxCollisionShape
    /** Thin probe half height, cached because Bullet does not expose the shape field. */
    private var suspensionProbeHalfY = .03f
    @Volatile var renderState = VehiclePhysicsRenderState()
        private set
    internal val isGrounded: Boolean get() = grounded
    private var grounded = false
    private var bodyGrounded = false
    private var leftSuspension = SideSuspensionState()
    private var rightSuspension = SideSuspensionState()
    private var terrainWaiting = false
    private var waitingVelocity = Vec3.ZERO
    private var lastSequence = 0L
    private var renderLeftTravel = 0f
    private var renderRightTravel = 0f
    private var trackPhaseLeft = 0f
    private var trackPhaseRight = 0f
    private var renderInitialized = false
    private var currentContactSupport = 0f
    private var previousContactSupport = 0f

    private fun setSimulationGravity(enabled: Boolean) {
        // Gravity is applied as an explicit Bullet force in preStep().  This
        // keeps placement deterministic even while Spark is waiting for a
        // terrain section to become active, and avoids native space-gravity
        // state being replaced when a body is inserted into the world.
        body.setGravity(Vector3f())
    }

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

    fun create() {
        physics.assertPhysicsThread()
        if (disposed) return
        box = BoxCollisionShape(half.bullet()).apply {
            // Keep the physical hull flush with the authored OBB/model.  The
            // default Bullet margin is large enough to create visible gaps on
            // small vehicles and can make the chassis appear to float.
            margin = .001f
        }
        // The suspension probe is a thin slab swept from the wheel centre down
        // past the contact patch. Its half height is intentionally tiny so its
        // own faces can never lift the chassis.
        val probeWidth = spec.suspensionSides.maxOfOrNull { it.halfWidth }?.toFloat()?.coerceIn(.06f, 2f)
            ?: max(half.x.toFloat() * .16f, .08f)
        val probeLength = spec.suspensionSides.maxOfOrNull { it.halfLength }?.toFloat()?.coerceIn(.12f, 8f)
            ?: max(half.z.toFloat() * .72f, .12f)
        suspensionProbeHalfY = min(.05f, half.y.toFloat() * .08f).coerceAtLeast(.015f)
        suspensionProbe = BoxCollisionShape(Vector3f(
            probeWidth,
            suspensionProbeHalfY,
            probeLength
        )).apply { margin = .001f }
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
        // Keep the chassis-terrain contact as a continuous Bullet safety
        // boundary. The side probe adds the small spring/rebound response, but
        // it must never be the only thing preventing a newly placed vehicle
        // from tunnelling through terrain.
        val chassisTerrainCollision = true
        val chassisMask = if (chassisTerrainCollision) {
            CollisionGroups.TERRAIN or CollisionGroups.CHASSIS or CollisionGroups.PHYSICS_BODY
        } else {
            CollisionGroups.CHASSIS or CollisionGroups.PHYSICS_BODY
        }
        body = PhysicsRigidBody(shape, if (anchored) 0f else mass).apply {
            name = "sbw_chassis"
            owner = ownerHost
            collisionGroup = CollisionGroups.CHASSIS
            setCollideWithGroups(chassisMask)
            // The chassis remains a continuous terrain safety boundary for
            // every vehicle. Ground vehicles add side probes for spring and
            // rebound visuals, while aircraft still receive Bullet gravity.
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
        }
        reset(state)
        body.onResolvedContact { event ->
            // A banked vehicle's support normal must be compared with its
            // current chassis up axis, rather than world Y.
            val chassisUp = body.getPhysicsRotation(null)
                .toRotationMatrix().mult(Vector3f.UNIT_Y, null).normalize()
            val normal = event.snapshot.normal
            val support = normal.x * chassisUp.x + normal.y * chassisUp.y + normal.z * chassisUp.z
            if (support > 0.45 || (groundVehicle && abs(normal.y) > .45f)) bodyGrounded = true
            if (support > 0.45f && event.snapshot.normalImpulse.isFinite()) {
                currentContactSupport = max(currentContactSupport, event.snapshot.normalImpulse * PhysicsLevel.TPS.toFloat())
            }
            if (!clientSide && event.snapshot.normalImpulse > mass * 0.05)
                impacts.add(VehicleImpact(event.other.owner, event.snapshot))
        }
        body.onPrePhysicsTick { preStep() }
        body.onPostPhysicsTick {
            previousContactSupport = currentContactSupport
            currentContactSupport = 0f
            grounded = grounded || bodyGrounded
            if (!clientSide) {
                state = readState(physics.tickCount, lastSequence)
            }
            if (groundVehicle) updateTrackPhase()
            val visualScale = settings.suspensionVisualScale.coerceIn(0f, 1f)
            val visualLimit = settings.suspensionVisualMaxOffset.coerceIn(.005f, .25f)
            val targetLeftTravel = (leftSuspension.travel * visualScale).coerceIn(-visualLimit, visualLimit)
            val targetRightTravel = (rightSuspension.travel * visualScale).coerceIn(-visualLimit, visualLimit)
            // Rendered suspension follows the physical target over several
            // substeps so a contact edge cannot teleport a wheel bone.
            renderLeftTravel += (targetLeftTravel - renderLeftTravel) * .24f
            renderRightTravel += (targetRightTravel - renderRightTravel) * .24f
            renderState = VehiclePhysicsRenderState(
                physicsFrame = physics.tickCount,
                leftTravel = renderLeftTravel,
                rightTravel = renderRightTravel,
                leftForce = leftSuspension.force,
                rightForce = rightSuspension.force,
                leftTrackPhase = trackPhaseLeft,
                rightTrackPhase = trackPhaseRight,
                grounded = grounded,
            )
        }
        physics.addBody(body)
        ready = true
    }

    fun reset(value: RigidState) {
        if (!::body.isInitialized || !value.position.finite() || !value.velocity.finite()) return
        body.clearForces()
        body.setPhysicsLocation(center(value.position, value.rotation).bullet())
        body.setPhysicsRotation(value.rotation.bullet())
        body.setLinearVelocity((if (anchored) Vec3.ZERO else value.velocity).bullet())
        body.setAngularVelocity((if (anchored) Vec3.ZERO else value.angularVelocity).bullet())
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
    }

    fun readState(frame: Long, sequence: Long): RigidState {
        val raw = body.getPhysicsRotation(null).joml()
        val q = if (raw.isFinite && raw.lengthSquared() > .0001f) raw.normalize() else Quaternionf()
        val position = body.getPhysicsLocation(null).minecraft()
        val velocity = body.getLinearVelocity(null).minecraft().let { if (it.finite()) it.limited(500.0) else Vec3.ZERO }
        val angular = body.getAngularVelocity(null).minecraft().let { if (it.finite()) it.limited(30.0) else Vec3.ZERO }
        return RigidState(frame, sequence, origin(position, q), q, velocity, angular, grounded)
    }

    /** Integrate the visual belt phase from Bullet's local chassis velocity. */
    private fun updateTrackPhase() {
        if (!::body.isInitialized || !trackedVehicle) return
        val q = body.getPhysicsRotation(null)
        val localVelocity = q.inverse().toRotationMatrix().mult(body.getLinearVelocity(null), null)
        val localAngular = q.inverse().toRotationMatrix().mult(body.getAngularVelocity(null), null)
        val sideOffset = spec.suspensionSides.maxOfOrNull { abs(it.anchorX).toFloat() } ?: half.x.toFloat()
        // In the vehicle frame +Z is forward and +Y angular velocity is a
        // right turn. The left contact group then travels farther than the
        // right group, matching the differential-drive force convention.
        val leftSpeed = localVelocity.z + localAngular.y * sideOffset
        val rightSpeed = localVelocity.z - localAngular.y * sideOffset
        val scale = settings.trackPhaseScale.coerceIn(.01f, 20f)
        val step = 16f * scale / PhysicsLevel.TPS.toFloat()
        trackPhaseLeft = (trackPhaseLeft + leftSpeed * step).let { ((it % 100f) + 100f) % 100f }
        trackPhaseRight = (trackPhaseRight + rightSpeed * step).let { ((it % 100f) + 100f) % 100f }
    }

    fun impulse(value: Vec3, point: Vec3? = null) {
        if (!anchored && value.finite() && (point == null || point.finite())) impulses.add(value to point)
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
                    renderState = value
                    authoritativeRender = null
                }
            }
            return
        }
        val velocity = body.getLinearVelocity(null).minecraft()
        if (!anchored) {
            // Apply gravity on every fixed Bullet substep.  It remains active
            // during terrain streaming, so a newly placed vehicle immediately
            // enters the same physical fall as an already cached vehicle.
            body.applyCentralForce(Vector3f(0f, -mass * settings.gravity.toFloat(), 0f))
        }
        val bound = body.boundingBox(null)
        val extent = bound.getExtent(null)
        val pos = body.getPhysicsLocation(null)
        val readyBox = net.minecraft.world.phys.AABB(pos.x-extent.x.toDouble(), pos.y-extent.y.toDouble(), pos.z-extent.z.toDouble(),
            pos.x+extent.x.toDouble(), pos.y+extent.y.toDouble(), pos.z+extent.z.toDouble())
            .expandTowards(velocity.scale(0.1))
            .inflate(.1, (settings.suspensionTravelUp + settings.suspensionTravelDown + .5).toDouble(), .1)
        if (!physics.terrainManager.isReady(readyBox)) {
            if (!terrainWaiting) waitingVelocity = velocity
            terrainWaiting = true
            // Terrain is streamed asynchronously. Keep Bullet dynamic while
            // the cache is being built so placement does not leave a vehicle
            // frozen in a non-physical preview state. The terrain body is
            // inserted at a physics-step boundary; no velocity reset is
            // needed when it becomes ready.
            setSimulationGravity(true)
            body.activate(true)
            return
        }
        if (terrainWaiting) {
            body.activate(true)
            terrainWaiting = false
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
        bodyGrounded = false
        val input = command
        if (!anchored) {
            if (groundVehicle && settings.suspensionEnabled && spec.suspensionSides.isNotEmpty()) {
                applySuspension(body, physics.world)
            }
            // Suspension contact is sampled before traction so the first input
            // frame after placement can use the actual support state.
            applyDrive(body, physics.world, input)
        }
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
        if (input.awake || input.acceleration.lengthSqr() > .01 || (groundVehicle && !grounded)) {
            target.activate(true)
        }
        if (groundVehicle) {
            applyGroundDrive(target, q, input)
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

    /** Apply traction at the two physical contact groups so steering can generate pitch/yaw naturally. */
    private fun applyGroundDrive(target: PhysicsRigidBody, rotation: com.jme3.math.Quaternion, input: DriveFrame) {
        if (!input.engineActive || spec.suspensionSides.isEmpty()) return
        // A freshly inserted body can be resting on a terrain mesh before the
        // contact callback reaches this frame. Permit traction while vertical
        // speed is effectively zero; a genuinely airborne chassis keeps its
        // Bullet momentum and does not receive wheel force.
        val verticalSpeed = abs(target.getLinearVelocity(null).y)
        if (!grounded && !bodyGrounded &&
            !leftSuspension.grounded && !rightSuspension.grounded && verticalSpeed > .2f) return
        val matrix = rotation.toRotationMatrix()
        val inverse = rotation.inverse()
        val localVelocity = inverse.toRotationMatrix().mult(target.getLinearVelocity(null), null)
        val localAngular = inverse.toRotationMatrix().mult(target.getAngularVelocity(null), null)
        val forward = matrix.mult(Vector3f.UNIT_Z, null).normalize()
        val up = matrix.mult(Vector3f.UNIT_Y, null).normalize()
        val sideAxis = matrix.mult(Vector3f.UNIT_X, null).normalize()
        val span = max(spec.suspensionSides.maxOfOrNull { abs(it.anchorX).toFloat() } ?: half.x.toFloat(), .1f)
        val normalLoad = (mass * settings.gravity.toFloat()).coerceAtLeast(1f)
        val maxTraction = normalLoad * settings.friction.coerceIn(.05f, 3f)
        val gain = (mass * 5f * settings.driveScale.coerceIn(.05, 4.0)).toFloat()
        val demands = floatArrayOf(input.leftDrive.coerceIn(-1.0, 1.0).toFloat(), input.rightDrive.coerceIn(-1.0, 1.0).toFloat())
        val sides = arrayOf(WheelSide.LEFT, WheelSide.RIGHT)
        demands.forEachIndexed { index, demand ->
            if (abs(demand) < .0001f) return@forEachIndexed
            val side = spec.side(sides[index]) ?: return@forEachIndexed
            val sideX = side.anchorX.toFloat()
            val point = Vector3f(sideX, (side.bottomY + suspensionProbeHalfY).toFloat(), side.centerZ.toFloat())
            val contactVelocity = Vector3f(
                localVelocity.x + localAngular.y * point.z - localAngular.z * point.y,
                localVelocity.y + localAngular.z * point.x - localAngular.x * point.z,
                localVelocity.z + localAngular.x * point.y - localAngular.y * point.x,
            )
            val targetSideSpeed = demand * input.targetSpeed.toFloat()
            val force = ((targetSideSpeed - contactVelocity.z) * gain).coerceIn(-maxTraction, maxTraction)
            target.applyForce(forward.mult(force), matrix.mult(point, null))
            val lateralForce = (-contactVelocity.x * mass * 3f).coerceIn(-maxTraction, maxTraction)
            target.applyForce(sideAxis.mult(lateralForce), matrix.mult(point, null))
        }
        // A grounded vehicle has no artificial attitude controller. The contact
        // forces above create yaw from the left/right force difference; upright
        // damping comes from the configured Bullet angular damping.
    }

    internal fun applySuspension(target: PhysicsRigidBody, space: PhysicsSpace) {
        val rotation = target.getPhysicsRotation(null)
        val matrix = rotation.toRotationMatrix()
        val up = matrix.mult(Vector3f.UNIT_Y, null).normalize()
        val sideAxis = matrix.mult(Vector3f.UNIT_X, null).normalize()
        val forward = matrix.mult(Vector3f.UNIT_Z, null).normalize()
        if (up.y < .15f) {
            leftSuspension = SideSuspensionState(-settings.suspensionTravelDown)
            rightSuspension = SideSuspensionState(-settings.suspensionTravelDown)
            grounded = false
            return
        }

        val center = target.getPhysicsLocation(null)
        val obbCenter = center.subtract(matrix.mult(settings.centerOfMassOffset.bullet(), null))
        val sideOffset = max(settings.suspensionSideOffset,
            spec.suspensionSides.maxOfOrNull { abs(it.anchorX).toFloat() } ?: 0f)
            .coerceIn(.05f, max(half.x.toFloat() * .85f, .05f))
        val mask = CollisionGroups.TERRAIN or CollisionGroups.PHYSICS_BODY
        val states = arrayOf(leftSuspension, rightSuspension)
        val forcePoints = arrayOfNulls<Vector3f>(2)
        val forceValues = FloatArray(2)

        for (index in 0..1) {
            val sideSpec = spec.side(if (index == 0) WheelSide.LEFT else WheelSide.RIGHT)
                ?: continue
            val previous = states[index]
            val anchor = Vector3f(obbCenter)
                .add(sideAxis.mult(sideSpec.anchorX.toFloat()))
                .add(up.mult((sideSpec.bottomY + suspensionProbeHalfY).toFloat()))
                .add(forward.mult(sideSpec.centerZ.toFloat()))
            val from = anchor.add(up.mult(settings.suspensionTravelUp))
            val to = anchor.subtract(up.mult(settings.suspensionTravelDown))
            val hit = sweepBody(space, suspensionProbe, from, to, rotation, target, mask)
            var travel = -settings.suspensionTravelDown
            var normal = previous.normal
            var contact = false
            if (hit != null) {
                val candidate = hit.getHitNormalLocal(null).normalize()
                if (candidate.dot(up) >= .25f) {
                    travel = settings.suspensionTravelUp - hit.hitFraction.coerceIn(0f, 1f) *
                        (settings.suspensionTravelUp + settings.suspensionTravelDown)
                    normal = candidate.clone()
                    contact = true
                }
            } else if (previous.grounded && previous.missedSteps < 2) {
                travel = previous.travel
                contact = true
            }

            val relative = anchor.subtract(center)
            val linear = target.getLinearVelocity(null)
            val angular = target.getAngularVelocity(null)
            val anchorVelocity = Vector3f(
                linear.x + angular.y * relative.z - angular.z * relative.y,
                linear.y + angular.z * relative.x - angular.x * relative.z,
                linear.z + angular.x * relative.y - angular.y * relative.x,
            )
            val compressionVelocity = -anchorVelocity.dot(up).coerceIn(
                -settings.suspensionCompressionVelocityLimit,
                settings.suspensionCompressionVelocityLimit,
            )
            if (!contact) {
                states[index] = SideSuspensionState(travel, 0f, false, previous.missedSteps + 1, normal)
                continue
            }
            val solved = PhysicsSuspensionSolver.solve(
                SideSuspensionInput(true, travel, compressionVelocity, normal.dot(up)),
                mass, settings.gravity.toFloat(), settings.suspensionTravelUp,
                settings.suspensionTravelDown, settings.suspensionFrequency,
                settings.suspensionCompressionDamping, settings.suspensionReboundDamping,
                settings.suspensionMaxForce,
            )
            states[index] = SideSuspensionState(travel, solved.forceN, solved.grounded, 0, normal)
            forceValues[index] = solved.forceN
            forcePoints[index] = relative
        }

        val supportLimit = (mass * settings.gravity.toFloat() *
            settings.suspensionMaxSupportAcceleration.coerceAtLeast(1f) - previousContactSupport)
            .coerceAtLeast(0f)
        val supportTotal = forceValues.sum()
        val supportScale = when {
            supportTotal <= 0f -> 0f
            supportLimit <= 0f -> 0f
            supportTotal > supportLimit -> supportLimit / supportTotal
            else -> 1f
        }
        for (index in 0..1) {
            val point = forcePoints[index] ?: continue
            val force = forceValues[index] * supportScale
            states[index].force = force
            if (force > 0f) target.applyForce(up.mult(force), point)
        }

        val stiffness = mass * .5f * (2f * Math.PI.toFloat() * settings.suspensionFrequency.coerceAtLeast(.1f)).let { it * it }
        if (states[0].grounded && states[1].grounded) {
            val difference = states[0].travel.coerceAtLeast(0f) - states[1].travel.coerceAtLeast(0f)
            if (abs(difference) > .01f) {
                val maxCorrection = min(settings.suspensionMaxForce * .25f, mass * settings.gravity.toFloat() * .25f)
                val (leftCorrection, rightCorrection) = PhysicsSuspensionSolver.antiRoll(
                    states[0].travel.coerceAtLeast(0f), states[1].travel.coerceAtLeast(0f), stiffness,
                    settings.suspensionAntiRoll, maxCorrection,
                )
                target.applyForce(up.mult(leftCorrection), sideAxis.mult(-sideOffset))
                target.applyForce(up.mult(rightCorrection), sideAxis.mult(sideOffset))
            }
        }
        leftSuspension = states[0]
        rightSuspension = states[1]
        grounded = states.any { it.grounded }
    }

    private fun stepAssist(target: PhysicsRigidBody, space: PhysicsSpace, height: Float) {
        val q = target.getPhysicsRotation(null)
        if (q.toRotationMatrix().mult(Vector3f.UNIT_Y, null).y < .5f) return
        val pos = target.getPhysicsLocation(null).subtract(
            q.toRotationMatrix().mult(settings.centerOfMassOffset.bullet(), null))
        val mask = CollisionGroups.TERRAIN or CollisionGroups.PHYSICS_BODY
        val down = sweepBody(space, box, pos, pos.add(0f, -.2f, 0f), q, target, mask) ?: return
        if (down.getHitNormalLocal(null).y < .4f) return
        val v = target.getLinearVelocity(null)
        val forward = Vector3f(v.x, 0f, v.z).mult(.1f)
        if (forward.lengthSquared() < .0001f) return
        val obstacle = sweepBody(space, box, pos, pos.add(forward), q, target, mask) ?: return
        if (obstacle.getHitNormalLocal(null).y > .7f) return
        val up = pos.add(0f, height + .03f, 0f)
        if (sweepBody(space, box, pos.add(0f, .03f, 0f), up, q, target, mask) != null) return
        if (sweepBody(space, box, up, up.add(forward), q, target, mask) != null) return
        val landing = sweepBody(space, box, up.add(forward), pos.add(forward).add(0f, -.15f, 0f), q, target, mask) ?: return
        if (landing.getHitNormalLocal(null).y < .4f) return
        val rise = (height + .03f) - landing.hitFraction * (height + .18f)
        if (rise <= .025f || rise > height + .04f) return
        // Whole-hull clearance authorizes a bounded assist, never a teleport.
        val acceleration = (rise * 100f - v.y * 20f + settings.gravity.toFloat()).coerceIn(0f, 100f)
        target.applyCentralForce(Vector3f(0f, mass * acceleration, 0f))
    }

    fun close() {
        disposed = true
        ready = false
        physics.submitImmediateTask {
            if (::body.isInitialized) physics.removeBody(body)
        }
    }
}
