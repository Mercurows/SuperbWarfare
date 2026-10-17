package com.atsuishio.superbwarfare.physics

import com.atsuishio.superbwarfare.Mod
import cn.solarmoon.spark_core.physics.PhysicsSuspensionSolver
import cn.solarmoon.spark_core.physics.SideSuspensionInput
import cn.solarmoon.spark_core.physics.VehicleControlMode
import cn.solarmoon.spark_core.physics.PhysicsHost
import cn.solarmoon.spark_core.physics.body.*
import cn.solarmoon.spark_core.physics.level.PhysicsLevel
import cn.solarmoon.spark_core.physics.query.TerrainRayQuery
import cn.solarmoon.spark_core.physics.query.sweepBody
import cn.solarmoon.spark_core.physics.suspension.ProbeSuspensionUnit
import cn.solarmoon.spark_core.physics.terrain.LocalHeightField
import com.atsuishio.superbwarfare.data.vehicle.subdata.WheelRole
import com.atsuishio.superbwarfare.data.vehicle.subdata.WheelSide
import com.jme3.bullet.RotationOrder
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.SolverMode
import com.jme3.bullet.collision.ManifoldPoints
import com.jme3.bullet.collision.PersistentManifolds
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

data class TrackContactDiagnostic(val index: Int, val side: WheelSide, val touching: Boolean,
    val travel: Float, val force: Float, val point: Vec3, val normal: Vec3,
    /** Actual world-space corners of the side's continuous bottom face. */
    val footprint: List<Vec3> = emptyList(), val manifoldContacts: Int = 0)
data class VehiclePhysicsDiagnostic(val frame: Long, val waitingForTerrain: Boolean, val position: Vec3,
    val velocity: Vec3, val contacts: List<TrackContactDiagnostic>)
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
    private var trackPhaseLeft = 0f
    private var trackPhaseRight = 0f
    private var renderInitialized = false
    private var currentContactSupport = 0f
    private var previousContactSupport = 0f
    private val wheels = ArrayList<WheelRuntime>()
    /**
     * One tracked side's aggregated state. The physics is carried by the
     * per-station support probes; this record only serves reporting, the
     * whole-side visual height and the acceptance diagnostics.
     */
    private class TrackContact(val side: SideSuspensionSpec) {
        var travel = 0f
        var normal = Vector3f.UNIT_Y.clone()
        var point = Vector3f()
        var other: PhysicsRigidBody? = null
        var force = 0f
        var touching = false
        var manifoldContacts = 0
        var lossReports = 0
        var preSolveState = ""
    }
    private val trackContacts = ArrayList<TrackContact>()
    /** One generic probe suspension unit per tracked side (core-owned math). */
    private val trackUnits = ArrayList<ProbeSuspensionUnit>()
    /** Smoothed climbable-step fallback, rebuilt when the centre block moves. */
    private var trackField: LocalHeightField? = null
    private var trackFieldRaw = FloatArray(0)
    /** Drivable support height per column: real block top or climb wedge. */
    private var trackFieldSupport = FloatArray(0)
    private var trackFieldMinX = 0
    private var trackFieldMinZ = 0
    private var trackFieldSize = 0
    private var trackFieldCenterX = Int.MIN_VALUE
    private var trackFieldCenterZ = Int.MIN_VALUE
    /** Fallback refresh cadence in physics substeps, terrain edits included. */
    private val TRACK_FIELD_REFRESH_STEPS = 8L
    /** Smoothed yaw-limited belt speeds per side (m/s). */
    private var trackBeltLeft = 0f
    private var trackBeltRight = 0f
    /** Low-passed yaw-rate feedback (rad/s). */
    private var trackYawRateFiltered = 0.0
    /** Previous controlled yaw rate for the acceleration limiter. */
    private var trackYawRateLimited = 0.0
    /** Deferred climb-assist contact recorded by the last contact callback. */
    private var hullContactGuard = false
    /** Highest raw block rise a hull contact may be turned into a climb ramp. */
    private val CLIMB_STEP_BLOCKS = 1.05f
    /** Horizontal run of that ramp, matching the authored terrain wedge. */
    private val CLIMB_RUN_BLOCKS = 3f
    /** Smallest raw rise that still counts as a step instead of flat ground. */
    private val CLIMB_MIN_RISE = .25f
    private val trackWheelSpins = HashMap<Pair<WheelSide, Int>, Float>()
    private val trackSuspensionProfile = TrackSuspensionProfile(settings.suspensionTravelUp,
        settings.suspensionTravelDown, settings.suspensionVisualScale, settings.suspensionVisualMaxOffset)
    private var heldMotion: List<Triple<PhysicsRigidBody, Vector3f, Vector3f>> = emptyList()
    private var hasReadyTerrain = false
    private var terrainIssue: String? = null
    private val diagnosticSteps = Integer.getInteger("superbwarfare.physics.traceSteps", 0).coerceIn(0, 3600)
    private val climbTrace = java.lang.Boolean.getBoolean("superbwarfare.physics.traceClimb")
    private var climbTraceLines = 0
    private var diagnosticSamples = 0
    private var turretBody: PhysicsRigidBody? = null
    private var turretJoint: New6Dof? = null
    private var barrelBody: PhysicsRigidBody? = null
    private var barrelJoint: New6Dof? = null
    /** Total mass reserved for the independent wheel bodies. */
    private val wheelMassTotal: Float by lazy {
        if (trackedVehicle || spec.wheelStations.isEmpty() || anchored) 0f
        else (mass * settings.wheelMassFraction.coerceIn(0f, .35f)).coerceIn(0f, mass * .35f)
    }
    /** Total mass reserved for the constrained turret and barrel bodies. */
    private val articulationMassTotal: Float by lazy {
        if (anchored || !settings.articulationEnabled || (spec.turret == null && spec.barrel == null)) 0f
        else (mass * settings.articulationMassFraction.coerceIn(0f, .2f)).coerceIn(0f, mass * .2f)
    }
    /** Chassis mass plus all articulated and wheel masses must equal the authored vehicle mass. */
    private val chassisMass: Float get() = (mass - wheelMassTotal - articulationMassTotal).coerceAtLeast(.1f)
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

    fun create() {
        physics.assertPhysicsThread()
        if (disposed) return
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
        // Solid hull contacts remain a safety boundary; the side support
        // probes own normal TRACK ride-height support, not the legacy OBB.
        //
        // A sprung tracked hull must not collide with the generated climb
        // wedges (SLOPE). The wedge is a convex prism: a flat hull box that
        // meets it penetrates the incline and the solver answers with a large
        // backward reaction, which stalled a tracked vehicle on the very first
        // stair even though its belt probes were already riding the ramp. The
        // belt probes still sweep DRIVABLE (blocks plus wedges) and own the
        // ride height; the hull keeps colliding with real blocks, so a
        // two-block wall still blocks it.
        // The generated climb wedges support the hull as well as the belt
        // probes. A tracked hull is a flat box, so letting only the probes ride
        // a ramp made the front row carry the whole nose alone (3.5x its weight
        // share) and lifted the middle anchors out of reach. With the wedge in
        // the hull mask the hull rides the same incline, the nose cannot be
        // levered up on one probe, and every station keeps contact. A two-block
        // wall has no wedge, so it still blocks the hull on real blocks.
        val chassisMask = CollisionGroups.DRIVABLE or CollisionGroups.CHASSIS or CollisionGroups.PHYSICS_BODY or CollisionGroups.WHEEL
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
            // The axle constraints own the chassis/wheel relationship. Do not
            // let same-owner contact resolution fight those constraints.
            collideWithOwnerGroups = CollisionGroups.NONE
        }

        // The chassis must be in Spark before its joints are registered.  The
        // wheel bodies are created after the chassis body, but before reset so
        // the initial pose is applied to the complete constrained assembly.
        physics.addBody(body)
        if (trackedVehicle && !anchored && !clientSide) createTrackProbes()
        if (!trackedVehicle && spec.wheelStations.isNotEmpty()) createWheelBodies()
        if (!clientSide) createArticulation()
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
                currentContactSupport += event.snapshot.normalImpulse * PhysicsLevel.TPS.toFloat()
            }
            if (!clientSide && event.snapshot.normalImpulse > mass * 0.05)
                impacts.add(VehicleImpact(event.other.owner, event.snapshot))
            if (climbTrace && climbTraceLines < 700) {
                val speed = body.getLinearVelocity(null).length()
                if (speed < .5f && abs(normal.y) < .9f) {
                    climbTraceLines++
                    Mod.LOGGER.info(
                        "[vehicle-hull] {} frame={} speed={} other={} normal={} impulse={} point={}",
                        spec.diagnosticName, physics.tickCount, speed,
                        (event.other as? PhysicsRigidBody)?.collisionGroup, normal,
                        event.snapshot.normalImpulse, event.snapshot.point)
                }
            }
        }
        if (trackedVehicle && !clientSide) {
            body.onCollideProcessed { event -> onTrackHullContact(event) }
        }
        body.onPrePhysicsTick { preStep() }
        body.onPostPhysicsTick {
            previousContactSupport = currentContactSupport
            currentContactSupport = 0f
            if (trackedVehicle && !clientSide) limitTrackYawRate()
            if (trackedVehicle && !clientSide) updateTrackProjection()
            grounded = grounded || bodyGrounded
            if (!clientSide) {
                state = readState(physics.tickCount, lastSequence)
            }
            if (trackedVehicle) updateTrackPhase()
            if (!trackedVehicle && wheels.isNotEmpty()) updateWheelProjection()
            val visualScale = settings.suspensionVisualScale.coerceIn(0f, 1f)
            val visualLimit = settings.suspensionVisualMaxOffset.coerceIn(.005f, 1f)
            val targetLeftTravel = if (trackedVehicle) {
                trackSuspensionProfile.height(leftSuspension.travel)
            } else renderLeftTravel.coerceIn(-visualLimit, visualLimit)
            val targetRightTravel = if (trackedVehicle) {
                trackSuspensionProfile.height(rightSuspension.travel)
            } else renderRightTravel.coerceIn(-visualLimit, visualLimit)
            // Rendered suspension follows the physical target over several
            // substeps so a contact edge cannot teleport a wheel bone.
            renderLeftTravel += (targetLeftTravel - renderLeftTravel) * .24f
            renderRightTravel += (targetRightTravel - renderRightTravel) * .24f
            val authoritativeRender = if (clientSide) clientRenderCurrent else null
            renderState = authoritativeRender ?: VehiclePhysicsRenderState(
                physicsFrame = physics.tickCount,
                leftTravel = renderLeftTravel,
                rightTravel = renderRightTravel,
                leftForce = leftSuspension.force,
                rightForce = rightSuspension.force,
                leftTrackPhase = trackPhaseLeft,
                rightTrackPhase = trackPhaseRight,
                wheels = if (trackedVehicle) spec.wheelStations.map { station ->
                    val side = if (station.side == WheelSide.LEFT) leftSuspension else rightSuspension
                    WheelRenderState(station.index, station.side,
                        if (station.side == WheelSide.LEFT) renderLeftTravel else renderRightTravel,
                        spin = trackWheelSpins[station.side to station.index] ?: 0f, grounded = side.grounded)
                } else wheels.map { wheel ->
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
            )
            trace("step")
            diagnostic = VehiclePhysicsDiagnostic(physics.tickCount, terrainWaiting, state.position, state.velocity,
                trackContacts.map { c -> TrackContactDiagnostic(0, c.side.side, c.touching,
                    c.travel, c.force, c.point.minecraft(), c.normal.minecraft(), trackFootprint(c), c.manifoldContacts) })
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
                    min(previous.minSupportContacts, trackContacts.count { it.touching && it.force > 0f }),
                    previous.terrainLost || terrainWaiting,
                    max(previous.maxAngularSpeed, spin),
                    max(previous.maxAngularDelta, spinDelta),
                    previous.angularJitterSteps + if (spinDelta > .08) 1 else 0)
            }
        }
        ready = true
    }

    fun reset(value: RigidState) {
        if (!::body.isInitialized || !value.position.finite() || !value.velocity.finite()) return
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
        trackWheelSpins.clear()
        trackBeltLeft = 0f
        trackBeltRight = 0f
        trackYawRateFiltered = 0.0
        trackYawRateLimited = 0.0
        trackUnits.forEach { unit -> unit.probes.forEach { p ->
            p.grounded = false; p.force = 0f; p.travel = -settings.suspensionTravelDown; p.resetTraction() } }
        trackContacts.forEach {
            it.touching = false
            it.force = 0f
            it.travel = -settings.suspensionTravelDown
            it.manifoldContacts = 0
        }
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
        val cycle = spec.trackAnimationLength.coerceAtLeast(1f)
        trackPhaseLeft = (trackPhaseLeft + leftSpeed * step).let { ((it % cycle) + cycle) % cycle }
        trackPhaseRight = (trackPhaseRight + rightSpeed * step).let { ((it % cycle) + cycle) % cycle }
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
        terrainIssue = physics.terrainManager.readinessIssue(readyBox)
        val terrainReady = terrainIssue == null
        if (!terrainReady) {
            if (!terrainWaiting && !hasReadyTerrain) {
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
            trackContacts.forEach { it.touching = false; it.force = 0f; it.travel = -settings.suspensionTravelDown }
            trackUnits.forEach { unit -> unit.probes.forEach { p ->
                p.grounded = false; p.force = 0f; p.travel = -settings.suspensionTravelDown; p.resetTraction() } }
            grounded = false
            // Only initial placement is held. Runtime mesh invalidation must
            // not lock a falling/impacted vehicle or erase its momentum.
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
            if (terrainReady && groundVehicle && settings.suspensionEnabled && spec.suspensionSides.isNotEmpty()) {
                if (trackedVehicle) applyTrackSuspension(body, physics.world) else applyWheelSuspension(body, physics.world)
            }
            // Suspension contact is sampled before traction so the first input
            // frame after placement can use the actual support state.
            applyDrive(body, physics.world, input)
            driveArticulation(input)
        }
        trace("forces")
        if (diagnosticSteps > 0) trackContacts.forEach { c ->
            if (c.lossReports < 4) {
                c.preSolveState = "v=${body.getLinearVelocity(null)} active=${body.isActive}"
            }
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
            // TRACK traction is the explicit Coulomb-bounded probe forces;
            // WHEEL traction is the wheel bodies' native friction reacting to
            // the spin motor. Neither path adds a second tire force.
            if (trackedVehicle) applyTrackDrive(input) else applyWheelDrive(input)
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

    /**
     * One probe suspension unit per side, one support probe per authored
     * SUPPORT station. The probes own no rigid bodies: the belt's ride height
     * is pure spring/damper force at the wheel bottoms, exactly like the
     * reference independent track suspensions, so a long side follows ramps
     * instead of bridging them.
     */
    private fun createTrackProbes() {
        // The probes own no rigid body. The whole authored mass therefore stays
        // on the chassis (minus the articulation bodies), and every support
        // point carries a share of it. Reserving a "track plate" fraction here
        // used to delete that mass from the simulation entirely, because no
        // body was ever created for it.
        val supportedMassPerSide = (mass * .5f).coerceAtLeast(.01f)
        spec.suspensionSides.forEach { side ->
            val stations = spec.wheelStations.filter { it.side == side.side && it.role == WheelRole.SUPPORT }
            val contact = TrackContact(side)
            trackContacts += contact
            val shares = tributaryShares(stations.map { it.center.z })
            val tag = if (side.side == WheelSide.LEFT) 0 else 1
            val probes = stations.mapIndexed { index, station ->
                ProbeSuspensionUnit.Probe(
                    station.center.bullet(), station.contactRadius.toFloat(),
                    (supportedMassPerSide.toDouble() * shares[index]).toFloat().coerceAtLeast(.01f), tag)
            }
            trackUnits += ProbeSuspensionUnit(body, probes,
                settings.suspensionTravelUp, settings.suspensionTravelDown, settings.suspensionFrequency,
                settings.gravity.toFloat(), settings.trackCompressionDampingRatio,
                settings.trackReboundDampingRatio, settings.suspensionCompressionVelocityLimit,
                settings.trackMaxSuspensionForce.coerceAtLeast(0f))
        }
    }

    /**
     * Static load share of every support point along one tracked side.
     *
     * A support point carries the belt span it stands on: its Voronoi cell
     * along the longitudinal axis, with the two end cells mirrored by half of
     * their neighbour gap so the shares always sum to one. Uniformly spaced
     * road wheels therefore get uniform shares, while an uneven wheel base
     * still spreads the authored weight and pitch moment sensibly - without a
     * per-vehicle constant. The resting pitch angle is finally set by the
     * spring equilibrium, so this only has to be a sound starting distribution.
     */
    private fun tributaryShares(longitudinal: List<Double>): DoubleArray {
        val count = longitudinal.size
        if (count == 0) return DoubleArray(0)
        if (count == 1) return doubleArrayOf(1.0)
        val order = longitudinal.indices.sortedBy { longitudinal[it] }
        val firstGap = longitudinal[order[1]] - longitudinal[order[0]]
        val lastGap = longitudinal[order[count - 1]] - longitudinal[order[count - 2]]
        val spans = DoubleArray(count)
        for (slot in order.indices) {
            val i = order[slot]
            val before = if (slot == 0) firstGap * .5
            else (longitudinal[i] - longitudinal[order[slot - 1]]) * .5
            val after = if (slot == count - 1) lastGap * .5
            else (longitudinal[order[slot + 1]] - longitudinal[i]) * .5
            spans[i] = (before + after).coerceAtLeast(1.0e-4)
        }
        val total = spans.sum()
        if (!total.isFinite() || total <= 1.0e-6) return DoubleArray(count) { 1.0 / count }
        return DoubleArray(count) { spans[it] / total }
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
        val shared = articulationMassTotal.coerceAtLeast(.1f)
        val turretMass = if (barrelSpec != null) (shared * .7f).coerceAtLeast(.1f) else shared
        val barrelMass = (shared * .3f).coerceAtLeast(.1f)

        val turretShape = articulationShape(turretSpec) ?: return
        val turret = articulationBody("sbw_turret", turretShape, turretMass)
        turretBody = turret
        turretJoint = articulationJoint(body, turret, turretSpec)
        physics.addBody(turret)
        physics.addJoint(turretJoint!!)

        if (barrelSpec != null) {
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
            setCollideWithGroups(CollisionGroups.DRIVABLE or CollisionGroups.PHYSICS_BODY or
                CollisionGroups.CHASSIS or CollisionGroups.WHEEL)
            // Same-owner contacts (the chassis and the wheels) must never fight
            // the revolute constraint; terrain and other vehicles still collide.
            collideWithOwnerGroups = CollisionGroups.NONE
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

    /**
     * Tracked-side suspension: the core probe units solve every support point,
     * and this method only aggregates the per-side reporting the visuals and
     * the acceptance diagnostics consume. A missing raw contact may fall back
     * to the smoothed local height field for climbable one-block steps; taller
     * raw cliffs stay blocking because the gate compares raw column tops.
     */
    internal fun applyTrackSuspension(target: PhysicsRigidBody, space: PhysicsSpace) {
        if (trackUnits.isEmpty()) return
        val rotation = target.getPhysicsRotation(null)
        val up = rotation.toRotationMatrix().mult(Vector3f.UNIT_Y, null).normalize()
        val chassisPosition = target.getPhysicsLocation(null)
        val velocity = target.getLinearVelocity(null)
        val angular = target.getAngularVelocity(null)
        val dt = 1f / PhysicsLevel.TPS
        if (up.y < .15f) {
            trackUnits.forEach { unit -> unit.probes.forEach { p ->
                p.grounded = false; p.force = 0f; p.travel = -settings.suspensionTravelDown } }
            trackContacts.forEach { c -> c.touching = false; c.force = 0f; c.travel = -settings.suspensionTravelDown }
            leftSuspension = SideSuspensionState(-settings.suspensionTravelDown)
            rightSuspension = SideSuspensionState(-settings.suspensionTravelDown)
            grounded = false
            return
        }
        refreshTrackHeightField(chassisPosition)
        val fallback = trackGroundFallback
        // Blocks plus the generated climb wedges: the belt rides the incline
        // exactly like the hull does, so both share the same support surface.
        val mask = CollisionGroups.DRIVABLE or CollisionGroups.PHYSICS_BODY
        trackUnits.forEach { unit ->
            unit.step(space, mask, rotation, up, chassisPosition, velocity, angular, dt, fallback)
        }
        trackContacts.forEachIndexed { index, c ->
            val unit = trackUnits.getOrNull(index) ?: return@forEachIndexed
            var forceSum = 0f
            var travelSum = 0f
            var count = 0
            var anyHit = false
            val weightedPoint = Vector3f()
            val weightedNormal = Vector3f()
            unit.probes.forEach { p ->
                if (p.grounded) anyHit = true
                if (p.force > 0f) {
                    forceSum += p.force
                    travelSum += p.travel
                    count++
                    weightedPoint.addLocal(p.point.mult(p.force))
                    weightedNormal.addLocal(p.normal.mult(p.force))
                }
            }
            c.touching = anyHit && forceSum > 0f
            c.force = forceSum
            c.travel = if (count > 0) travelSum / count else -settings.suspensionTravelDown
            c.point = if (forceSum > 0f) weightedPoint.mult(1f / forceSum) else Vector3f()
            c.normal = if (weightedNormal.lengthSquared() > 1e-6f) weightedNormal.normalize()
            else Vector3f.UNIT_Y.clone()
            c.manifoldContacts = count
            c.other = null
        }
        grounded = trackContacts.any { it.touching && it.force > 0f }
        if (climbTrace && climbTraceLines < 400) {
            val speed = body.getLinearVelocity(null).length()
            if (speed < .5f) {
                climbTraceLines++
                Mod.LOGGER.info(
                    "[vehicle-belt] {} frame={} speed={} pos={} probes={}",
                    spec.diagnosticName, physics.tickCount, speed, chassisPosition,
                    trackUnits.joinToString(" | ") { unit ->
                        unit.probes.joinToString(",") { p ->
                            "${p.tag}:${if (p.grounded) "G" else "-"}" +
                                " t=${"%.3f".format(p.travel)} f=${"%.0f".format(p.force)}" +
                                " y=${"%.3f".format(p.point.y)}"
                        }
                    })
            }
        }
        fun suspension(side: WheelSide): SideSuspensionState {
            val c = trackContacts.firstOrNull { it.side.side == side } ?: return SideSuspensionState()
            return SideSuspensionState(c.travel, c.force, c.touching, 0, c.normal.clone())
        }
        leftSuspension = suspension(WheelSide.LEFT)
        rightSuspension = suspension(WheelSide.RIGHT)
    }

    /**
     * Rebuild the smoothed column grid when the centre block moved and at a
     * bounded cadence, so a terrain edit (a dug-out side) refreshes the
     * fallback within the acceptance window instead of holding stale ground.
     * The grid samples physics-thread terrain queries; friction/material
     * metadata is not needed by the support fallback, so every column reads
     * hard with a neutral friction.
     */
    private fun refreshTrackHeightField(chassisPosition: Vector3f) {
        val centerX = floor(chassisPosition.x).toInt()
        val centerZ = floor(chassisPosition.z).toInt()
        val moved = trackField == null || centerX != trackFieldCenterX || centerZ != trackFieldCenterZ
        if (!moved && physics.tickCount % TRACK_FIELD_REFRESH_STEPS != 0L) return
        trackFieldCenterX = centerX
        trackFieldCenterZ = centerZ
        // Radius 4 also covers the hull nose of the longest tracked vehicles,
        // whose front contacts must see the same climb-assist surface.
        val radius = 4
        val size = radius * 2 + 1
        val minX = centerX - radius
        val minZ = centerZ - radius
        // Scan from a fixed clearance above the chassis centre down past the
        // belt plane. Deriving the start from `half.y` put the ray origin inside
        // the block the vehicle rests on, so the closest hit could be a block's
        // *underside* (a value a full block below the real surface). That made
        // the climb gate compare a wrong "raw" height against the belt.
        val baseY = chassisPosition.y
        val heights = FloatArray(size * size)
        val support = FloatArray(size * size)
        val friction = FloatArray(size * size) { .8f }
        val hard = BooleanArray(size * size) { true }
        for (z in 0 until size) {
            for (x in 0 until size) {
                val wx = minX + x
                val wz = minZ + z
                val query = TerrainRayQuery(wx + .5f, baseY + 3f, wz + .5f, wx + .5f, baseY - 6f, wz + .5f)
                val block = physics.raycastClosestTerrain(query, CollisionGroups.TERRAIN)
                heights[z * size + x] = block?.pointY ?: (baseY - 6f)
                // Support height: the drivable surface, i.e. the real block top
                // or the generated climb wedge above it. A wedge is a real
                // support surface, so the belt must be able to rest on it; the
                // block-only column above stays the climb *gate*, so a taller
                // face still refuses the fallback.
                val drivable = physics.raycastClosestTerrain(query, CollisionGroups.DRIVABLE)
                support[z * size + x] = drivable?.pointY ?: (block?.pointY ?: (baseY - 6f))
            }
        }
        trackFieldRaw = heights
        trackFieldSupport = support
        trackFieldMinX = minX
        trackFieldMinZ = minZ
        trackFieldSize = size
        trackField = LocalHeightField.fromColumns(minX, minZ, size, heights, friction, hard)
    }

    /** Drivable support height of one column (real block top or climb wedge). */
    private fun supportColumn(worldX: Float, worldZ: Float): Float {
        if (trackFieldSize <= 0) return Float.NaN
        val gx = (floor(worldX).toInt() - trackFieldMinX).coerceIn(0, trackFieldSize - 1)
        val gz = (floor(worldZ).toInt() - trackFieldMinZ).coerceIn(0, trackFieldSize - 1)
        return trackFieldSupport[gz * trackFieldSize + gx]
    }

    private fun rawColumn(worldX: Float, worldZ: Float): Float {
        if (trackFieldSize <= 0) return Float.NaN
        val gx = (floor(worldX).toInt() - trackFieldMinX).coerceIn(0, trackFieldSize - 1)
        val gz = (floor(worldZ).toInt() - trackFieldMinZ).coerceIn(0, trackFieldSize - 1)
        return trackFieldRaw[gz * trackFieldSize + gx]
    }

    /**
     * Fallback used only when a probe's raw sweep misses.
     *
     * The support height is the **raw column top**, never the smoothed value:
     * the smoothed field exists to give the contact a ramp-like normal so the
     * belt can drive up a step, but using its height as the support plane
     * spread the ramp several blocks in front of the step. Probes there then
     * read ground above the real floor, their travel went negative and their
     * spring unloaded, so a tracked vehicle stalled on a bridge of two end
     * probes well before reaching the step.
     *
     * The height gate compares the raw column with its own low side, so a
     * taller continuous face still refuses the fallback.
     */
    private val trackGroundFallback = ProbeSuspensionUnit.GroundFallback { anchor, up, radius, tag ->
        val field = trackField ?: return@GroundFallback null
        val raw = rawColumn(anchor.x, anchor.z)
        if (!raw.isFinite()) return@GroundFallback null
        // Lowest readable neighbour, but only where the probe can actually
        // reach: an unclimbable face must not be flattened into a ramp.
        var low = Float.POSITIVE_INFINITY
        for (dx in -1..1) {
            for (dz in -1..1) {
                val n = rawColumn(anchor.x + dx, anchor.z + dz)
                if (n.isFinite() && n < low) low = n
            }
        }
        if (!low.isFinite() || raw - low > 1f + .05f) return@GroundFallback null
        val sample = field.sample(anchor.x.toDouble(), anchor.z.toDouble())
        // Support is the real block top. The generated wedge only supplies the
        // ramp normal for traction; using its height as the support plane lifted
        // probes several blocks before a step and left a long hull bridged on
        // its end rows.
        ProbeSuspensionUnit.GroundSample(anchor.x, raw, anchor.z,
            Vector3f(sample.normalX, sample.normalY, sample.normalZ))
    }

    /**
     * TRACK traction. The belt owns no rigid contact body, so every grounded
     * support probe contributes an explicit Coulomb-bounded force: a
     * load-proportional slip stiffness drives the contact toward the belt
     * speed the native belt rows used to demand, and lateral slip is resisted
     * inside the same mu*N budget. Zero load can never produce force, and
     * coasting only loses the authored rolling resistance.
     */
    private fun applyTrackDrive(input: DriveFrame) {
        if (trackUnits.isEmpty()) return
        val dt = 1f / PhysicsLevel.TPS
        val friction = settings.friction.coerceIn(0f, 3f)
        val resistance = settings.trackRollingResistance.coerceAtLeast(0f)
        val matrix = body.getPhysicsRotation(null).toRotationMatrix()
        val forwardWorld = matrix.mult(Vector3f.UNIT_Z, null)
        val upWorld = matrix.mult(Vector3f.UNIT_Y, null)
        val chassisPosition = body.getPhysicsLocation(null)
        val velocity = body.getLinearVelocity(null)
        val angular = body.getAngularVelocity(null)
        // Authored pivot/turn limiter: the differential steering tracks a
        // target yaw rate (kinematic feedforward plus a bounded error term)
        // instead of a raw demand, and the commanded belt speeds are
        // first-order smoothed, so a pivot can neither spin up to friction
        // saturation nor relay-chatter.
        val leftAnchor = spec.suspensionSides.firstOrNull { it.side == WheelSide.LEFT }?.anchorX ?: 1.0
        val rightAnchor = spec.suspensionSides.firstOrNull { it.side == WheelSide.RIGHT }?.anchorX ?: -1.0
        val trackWidth = abs(leftAnchor - rightAnchor).coerceAtLeast(.5)
        val halfWidth = trackWidth * .5
        val maxYawRate = settings.trackMaxYawRate.toDouble().coerceIn(0.0, 6.0)
        val targetYaw = -input.steering.coerceIn(-1.0, 1.0) * maxYawRate
        // Low-pass the measured yaw rate before the error term: the raw
        // substep rate carries contact noise that would otherwise relay the
        // belt targets every step.
        val yawRateRaw = angular.dot(upWorld).toDouble()
        trackYawRateFiltered += (yawRateRaw - trackYawRateFiltered) * (dt / .08f).coerceIn(0f, 1f)
        val yawRate = trackYawRateFiltered
        val correction = (-targetYaw * halfWidth -
            (targetYaw - yawRate) * halfWidth * settings.trackYawResponse.coerceAtLeast(0f).toDouble())
            .coerceIn(-maxYawRate * halfWidth * 1.25, maxYawRate * halfWidth * 1.25)
        val centerDemand = input.centerDrive.coerceIn(-1.25, 1.25)
        val hasInput = abs(centerDemand) > .0001 || abs(input.steering) > .0001
        val driveActive = input.engineActive && !input.brake && hasInput
        val blend = (dt / settings.trackYawSmoothing.coerceAtLeast(.02f)).coerceIn(0f, 1f)
        if (driveActive) {
            val desiredLeft = (centerDemand * input.targetSpeed + correction).toFloat()
            val desiredRight = (centerDemand * input.targetSpeed - correction).toFloat()
            trackBeltLeft += (desiredLeft - trackBeltLeft) * blend
            trackBeltRight += (desiredRight - trackBeltRight) * blend
        }
        trackUnits.forEach { unit ->
            unit.probes.forEach { p ->
                if (!p.grounded || p.force <= 0f || friction <= 0f) {
                    p.resetTraction()
                    return@forEach
                }
                val normal = p.normal
                val forward = forwardWorld.subtract(normal.mult(forwardWorld.dot(normal), null))
                if (forward.lengthSquared() < 1.0e-6f) return@forEach
                val unitForward = forward.normalize()
                val lateral = normal.cross(unitForward, null).normalize()
                val anchor = chassisPosition.add(matrix.mult(p.offset, null))
                val relative = anchor.subtract(chassisPosition)
                val pointVelocity = velocity.add(angular.cross(relative, null))
                val vf = pointVelocity.dot(unitForward)
                val vl = pointVelocity.dot(lateral)
                // The belt-surface target mixes the driver demand (through the
                // yaw-rate limiter) with the same coasting decay the contact
                // rows used: a released throttle only loses the authored
                // rolling resistance, never a hidden brake.
                val belt = when {
                    driveActive -> (if (p.tag == 0) trackBeltLeft else trackBeltRight)
                    input.brake -> 0f
                    else -> vf - sign(vf) * min(abs(vf), resistance * 9.81f * dt)
                }
                val budget = friction * p.force
                // Reach the Coulomb budget at about 15 cm/s of belt slip.
                val slipStiffness = budget / .15f
                val longitudinal = (belt - vf) * slipStiffness
                val sideways = -vl * slipStiffness
                val magnitude = sqrt(longitudinal * longitudinal + sideways * sideways)
                val scale = if (magnitude > budget && magnitude > 1.0e-6f) budget / magnitude else 1f
                // First-order relaxation on the applied tangential force so a
                // momentarily saturated contact cannot relay-chatter.
                val relax = (dt / .04f).coerceIn(0f, 1f)
                p.appliedLongitudinal += (longitudinal * scale - p.appliedLongitudinal) * relax
                p.appliedLateral += (sideways * scale - p.appliedLateral) * relax
                val force = unitForward.mult(p.appliedLongitudinal, Vector3f())
                    .addLocal(lateral.mult(p.appliedLateral, Vector3f()))
                if (!force.x.isFinite() || !force.y.isFinite() || !force.z.isFinite()) return@forEach
                body.applyForce(force, relative)
            }
        }
        if (climbTrace && climbTraceLines < 600) {
            val speed = body.getLinearVelocity(null).length()
            if (speed < .5f) {
                climbTraceLines++
                Mod.LOGGER.info(
                    "[vehicle-traction] {} frame={} speed={} belt=({}, {}) center={} target={} steer={} drive={} grounded={} force={} vel={}",
                    spec.diagnosticName, physics.tickCount, speed, trackBeltLeft, trackBeltRight,
                    command.centerDrive, command.targetSpeed, command.steering,
                    driveActive, trackUnits.sumOf { u -> u.probes.count { it.grounded } },
                    body.totalAppliedForce(null), body.getLinearVelocity(null))
            }
        }
    }

    /**
     * Limit the *controlled* yaw acceleration after Bullet has integrated the
     * current substep. Differential traction can lose one support row for a
     * single contact update; without this final controller limiter that load
     * change can turn a normal steering command into a one-frame yaw kick.
     *
     * This is not a terrain correction: it is only active while a driver is
     * steering an awake tracked vehicle, clamps only the chassis yaw component,
     * and leaves no-input motion and external impulses untouched. The capped
     * rate is fed back on the next substep, so the controller remains smooth
     * instead of repeatedly snapping to a target.
     */
    private fun limitTrackYawRate() {
        if (anchored || !groundVehicle) return
        val input = command
        val controlled = input.engineActive && !input.brake && abs(input.steering) > .0001
        val rotation = body.getPhysicsRotation(null)
        val up = rotation.toRotationMatrix().mult(Vector3f.UNIT_Y, null).normalize()
        val angular = body.getAngularVelocity(null)
        val raw = angular.dot(up).toDouble()
        if (!raw.isFinite()) {
            trackYawRateLimited = 0.0
            return
        }
        if (!controlled) {
            trackYawRateLimited = raw
            return
        }
        val maxRate = settings.trackMaxYawRate.toDouble().coerceIn(.0, 6.0)
        val desired = raw.coerceIn(-maxRate, maxRate)
        val maxDelta = (settings.trackMaxYawAcceleration.coerceAtLeast(.1f) /
            PhysicsLevel.TPS.toFloat()).toDouble()
        val limited = trackYawRateLimited +
            (desired - trackYawRateLimited).coerceIn(-maxDelta, maxDelta)
        val correction = limited - raw
        if (abs(correction) > 1.0e-6) {
            body.setAngularVelocity(angular.add(up.mult(correction.toFloat(), Vector3f())))
        }
        trackYawRateLimited = limited
    }

    /**
     * Climbable one-block step support for the solid hull.
     *
     * A tracked hull is a box, so a step face solved as a pure horizontal
     * reaction stops the vehicle no matter how well the probes follow the
     * ground. For a step whose raw block rise above its own low side is at most
     * one block, this rewrites the *contact normal only* to the normal of the
     * equivalent 1:3 climb ramp. The same contact then pushes the hull up and
     * back instead of purely back.
     *
     * Nothing else is written: the position, the velocity, the contact point
     * and the manifold distance stay untouched, so the vehicle is never lifted
     * or teleported. A taller step keeps its solved normal and stays blocking,
     * and a floor, a generated ramp or a ceiling never qualifies.
     */
    private fun onTrackHullContact(event: PhysicsBodyEvent.Contact) {
        if (clientSide || terrainWaiting || !settings.suspensionEnabled) return
        if (hullContactGuard) return
        val other = event.other as? PhysicsRigidBody ?: return
        val normal = event.snapshot.normal.bullet()
        val slanted = abs(normal.y) <= .8f
        if (other.collisionGroup and CollisionGroups.TERRAIN == 0) {
            if (slanted) traceClimb("reject-group", event.snapshot.point.bullet(), Vector3f(), Float.NaN, Float.NaN, Float.NaN)
            return
        }
        if (abs(normal.y) > .45f) {
            if (slanted) traceClimb("reject-normal", event.snapshot.point.bullet(), Vector3f(), Float.NaN, Float.NaN, Float.NaN)
            return
        }
        val horizontal = Vector3f(normal.x, 0f, normal.z)
        if (horizontal.lengthSquared() < 1.0e-6f) return
        // `snapshot.normal` is the reaction on the hull, so it points *out of*
        // the obstacle and toward the vehicle. The step lies along `-normal`.
        val outOfStep = horizontal.normalize()
        val towardStep = outOfStep.negate()
        val point = event.snapshot.point.bullet()
        // Read the column the hull actually presses (just inside the face) and
        // the column the belt is still standing on (just outside it). Both are
        // sampled on the block grid so a stair tread never borrows its
        // neighbour's height.
        val stepColumn = rawColumn(
            floor(point.x + towardStep.x * .35f) + .5f,
            floor(point.z + towardStep.z * .35f) + .5f)
        val lowColumn = rawColumn(
            floor(point.x + outOfStep.x * .35f) + .5f,
            floor(point.z + outOfStep.z * .35f) + .5f)
        if (!stepColumn.isFinite() || !lowColumn.isFinite()) {
            traceClimb("reject-columns", point, towardStep, stepColumn, lowColumn, Float.NaN)
            return
        }
        val rise = stepColumn - lowColumn
        if (rise < CLIMB_MIN_RISE) {
            traceClimb("reject-flat", point, towardStep, stepColumn, lowColumn, Float.NaN)
            return
        }
        if (rise > CLIMB_STEP_BLOCKS) {
            traceClimb("reject-height", point, towardStep, stepColumn, lowColumn, Float.NaN)
            return
        }
        // Reaction on the hull: mostly up, tilted toward the hull by the same
        // 1:3 angle the authored ramp uses. This is a contact-data correction
        // only: the solver then pushes the hull up the stair face instead of
        // stopping it dead, and each successive tread repeats the same step.
        val desired = WORLD_UP.mult(CLIMB_RUN_BLOCKS, Vector3f())
            .addLocal(outOfStep)
            .normalize()
        hullContactGuard = true
        try {
            // Bullet stores the normal from B to A, while `snapshot.normal` is
            // the reaction on `self`; self = B therefore needs the negated
            // vector to keep the snapshot pointing at `desired`.
            val stored = if (event.selfPoint.objectIndex == 0) desired else desired.negate()
            event.selfPoint.setNormalWorldOnB(stored)
            traceClimb("accept", point, towardStep, stepColumn, lowColumn, rise)
        } catch (failure: Throwable) {
            Mod.LOGGER.error("[vehicle-climb] failed to correct a step contact normal", failure)
        } finally {
            hullContactGuard = false
        }
    }

    private fun traceClimb(decision: String, point: Vector3f, away: Vector3f, raw: Float,
                           low: Float, belt: Float) {
        if (!climbTrace || climbTraceLines >= 200) return
        // Only step encounters are interesting; flat-ground contacts flood the
        // log and would exhaust the budget before the staircase fixture runs.
        val stepEncounter = raw.isFinite() && low.isFinite() && raw - low > .4f
        if (decision != "accept" && !stepEncounter &&
            decision != "reject-group" && decision != "reject-normal") return
        climbTraceLines++
        Mod.LOGGER.info(
            "[vehicle-climb] {} {} frame={} point={} away={} raw={} low={} belt={} hull={}",
            spec.diagnosticName, decision, physics.tickCount, point, away, raw, low, belt,
            body.getPhysicsLocation(null))
    }

    /** Refresh the visual wheel spins from the live chassis velocity. */    private fun updateTrackProjection() {
        val matrix = body.getPhysicsRotation(null).toRotationMatrix()
        spec.wheelStations.forEach { station ->
            val point = body.getPhysicsLocation(null).add(matrix.mult(station.center.bullet(), null))
            val speed = contactVelocity(body, point).dot(matrix.mult(Vector3f.UNIT_Z, null))
            val key = station.side to station.index
            trackWheelSpins[key] = ((trackWheelSpins[key] ?: 0f) + speed / (station.radius.toFloat() * PhysicsLevel.TPS)) % (2f * PI.toFloat())
        }
    }

    private fun trackFootprint(c: TrackContact): List<Vec3> {
        val matrix = body.getPhysicsRotation(null).toRotationMatrix()
        val center = body.getPhysicsLocation(null)
        return listOf(-1f to -1f, -1f to 1f, 1f to 1f, 1f to -1f).map { (x, z) ->
            center.add(matrix.mult(Vector3f(
                (c.side.anchorX + x * c.side.halfWidth).toFloat(),
                c.side.bottomY.toFloat(),
                (c.side.centerZ + z * c.side.halfLength).toFloat()), null)).minecraft()
        }
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
            trackContacts.joinToString { "${it.side.side}:0 hit=${it.touching} travel=${it.travel} N=${it.force} p=${it.point} n=${it.normal} manifold=${it.manifoldContacts} other=${it.other?.name}" })
    }

    private fun stepAssist(target: PhysicsRigidBody, space: PhysicsSpace, height: Float) {
        val q = target.getPhysicsRotation(null)
        if (q.toRotationMatrix().mult(Vector3f.UNIT_Y, null).y < .5f) return
        val pos = target.getPhysicsLocation(null).subtract(
            q.toRotationMatrix().mult(settings.centerOfMassOffset.bullet(), null))
        val mask = CollisionGroups.DRIVABLE or CollisionGroups.PHYSICS_BODY
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
            trackContacts.clear()
            trackUnits.clear()
            wheels.forEach { wheel -> physics.removeJoint(wheel.joint) }
            wheels.forEach { wheel -> physics.removeBody(wheel.body) }
            wheels.clear()
            barrelJoint?.let { physics.removeJoint(it) }
            turretJoint?.let { physics.removeJoint(it) }
            barrelBody?.let { physics.removeBody(it) }
            turretBody?.let { physics.removeBody(it) }
            barrelJoint = null
            turretJoint = null
            barrelBody = null
            turretBody = null
            if (::body.isInitialized) physics.removeBody(body)
        }
    }
}
