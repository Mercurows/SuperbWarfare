package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.body.PhysicsJoints
import cn.solarmoon.spark_core.physics.body.collideWithOwnerGroups
import cn.solarmoon.spark_core.physics.body.name
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.collision.shapes.CylinderCollisionShape
import com.jme3.bullet.joints.New6Dof
import com.jme3.bullet.joints.motors.MotorParam
import com.jme3.bullet.joints.PhysicsJoint
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Matrix3f
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The physical mechanism of one tracked vehicle: dynamic wheels, dynamic shoes,
 * closed pin loops, passive suspension and a passive tensioner.
 *
 * Load path (the only one):
 *
 *     terrain -> shoes -> road wheels -> spring/damper -> hull
 *
 * The axle joints are guides only: they lock longitudinal/lateral motion and
 * bound vertical travel, but they carry no weight in mid-travel. Nothing in this
 * class writes a pose after [create]; the only per-substep work is reading
 * relative motion and applying bounded forces.
 */
class TrackedRig(
    private val space: PhysicsSpace,
    private val chassis: PhysicsRigidBody,
    val spec: TrackedVehicleSpec,
    private val addJoint: (PhysicsJoint) -> Unit = { space.addJoint(it) },
    private val addBody: (PhysicsRigidBody) -> Unit = { space.addCollisionObject(it) },
) : AutoCloseable {

    class WheelBody(
        val wheel: CompiledWheel,
        val body: PhysicsRigidBody,
        val joint: com.jme3.bullet.joints.PhysicsJoint,
        val anchorLocal: Vector3f,
        val dampingNsPerM: Double,
    ) {
        var travel = 0.0
        var travelVelocity = 0.0
        var springForceN = 0.0
        var grounded = false
        /** First-order filtered relative velocity used by the damper. */
        var filteredVelocity = 0.0
    }

    class LinkBody(
        val index: Int,
        val body: PhysicsRigidBody,
        val halfPitch: Double,
    )

    val wheels = ArrayList<WheelBody>()
    val links = ArrayList<LinkBody>()
    private val sideOfWheels = HashMap<TrackedSide, MutableList<WheelBody>>()
    private val sideOfLinks = HashMap<TrackedSide, MutableList<LinkBody>>()
    private val joints = ArrayList<PhysicsJoint>()
    private var started = false
    private var warnedStability = false

    /** Sum of every body mass this rig actually created, for accounting tests. */
    val createdMassKg: Double get() = wheels.sumOf { it.wheel.massKg } + links.sumOf { spec.linkMassKg }

    /**
     * Creates every body and joint. Bodies are positioned from the chassis
     * transform *before* any joint exists, so the solver never has to repair a
     * metre-scale constraint violation on the first step.
     */
    fun create(includeLinks: Boolean = true) {
        if (started) return
        started = true
        val position = chassis.getPhysicsLocation(null)
        val rotation = chassis.getPhysicsRotation(null)
        val matrix = rotation.toRotationMatrix()
        for (side in TrackedSide.entries) {
            val compiled = spec.sides[side] ?: continue
            createWheels(compiled, position, matrix, rotation)
            if (includeLinks) createLinks(compiled, position, matrix, rotation)
        }
        chassis.activate(true)
        links.forEach { it.body.activate(true) }
        wheels.forEach { it.body.activate(true) }
    }

    private fun createWheels(
        compiled: CompiledTrackSide,
        chassisPosition: Vector3f,
        chassisMatrix: Matrix3f,
        chassisRotation: Quaternion,
    ) {
        val list = ArrayList<WheelBody>(compiled.wheels.size)
        for (wheel in compiled.wheels) {
            val radius = wheel.radius.toFloat()
            val halfWidth = wheel.halfWidth.toFloat().coerceAtLeast(.02f)
            val shape = CylinderCollisionShape(Vector3f(halfWidth, radius, radius), 0).apply { margin = .001f }
            val body = PhysicsRigidBody(shape, wheel.massKg.toFloat().coerceAtLeast(1f))
            val anchor = wheel.center.bullet()
            val world = chassisPosition.add(chassisMatrix.mult(anchor, null))
            body.apply {
                name = "track_wheel_${compiled.side.name.lowercase()}_${wheel.index}"
                collisionGroup = COLLISION_WHEEL
                setCollideWithGroups(COLLISION_TERRAIN or COLLISION_LINK)
                // Same-owner filter: a wheel still collides with its own belt,
                // because its mask *includes* the link group.
                collideWithOwnerGroups = COLLISION_LINK
                friction = spec.drivetrain.friction.toFloat()
                restitution = 0f
                setDamping(0.04f, 0.02f)
                setSleepingThresholds(.02f, .02f)
                ccdMotionThreshold = radius * .5f
                ccdSweptSphereRadius = radius * .4f
                setProtectGravity(true)
                setGravity(Vector3f())
                setPhysicsLocation(world)
                setPhysicsRotation(chassisRotation)
                addBody(this)
                activate(true)
            }
            // Only road wheels are sprung. Sprockets and idlers sit on rigid
            // axles: locking LIN_Y to a zero window leaves them a pure revolute
            // joint, which is what Machine-Max does for its guide connectors.
            val sprung = wheel.suspension.isSprung
            val joint = if (sprung) {
                val stability = TrackedStability.clamp(
                    wheel.suspension.stiffnessNPerM,
                    (wheel.suspension.compressionDampingNsPerM + wheel.suspension.reboundDampingNsPerM) * 0.5,
                    TrackedStability.effectiveMassKg(chassis.mass.toDouble(), body.mass.toDouble()),
                    1.0 / 60.0,
                )
                if (stability.limited && !warnedStability) {
                    warnedStability = true
                    com.atsuishio.superbwarfare.Mod.LOGGER.warn(
                        "[tracked] {} spring clamped: k {}/{} c {}/{}", spec.vehicleId,
                        stability.stiffnessNPerM, stability.maxStiffnessNPerM,
                        stability.dampingNsPerM, stability.maxDampingNsPerM,
                    )
                }
                PhysicsJoints.wheelAxleSpring(
                    chassis, body, anchor, Vector3f(),
                    (-wheel.suspension.travelDown).toFloat(), wheel.suspension.travelUp.toFloat(),
                    stability.stiffnessNPerM.toFloat(), stability.dampingNsPerM.toFloat(),
                    ( -wheel.suspension.staticCompressionM).toFloat(),
                )
            } else {
                PhysicsJoints.guidedWheelAxle(
                    chassis, body, anchor, Vector3f(), Matrix3f(), Matrix3f(), 0f, 0f,
                )
            }
            addJoint(joint)
            joints += joint
            val runtime = WheelBody(wheel, body, joint, anchor, TrackedStability.clamp(
                wheel.suspension.stiffnessNPerM,
                (wheel.suspension.compressionDampingNsPerM + wheel.suspension.reboundDampingNsPerM) * 0.5,
                TrackedStability.effectiveMassKg(chassis.mass.toDouble(), body.mass.toDouble()),
                1.0 / 60.0,
            ).dampingNsPerM)
            wheels += runtime
            list += runtime
        }
        sideOfWheels[compiled.side] = list
    }

    /**
     * Shoes sit on the solved pin polygon: each body is centred on the chord
     * between two pins, with its local +Z along that chord. Consecutive bodies
     * are joined by a pin hinge and the last is joined back to the first, so the
     * belt is closed without anchoring any single shoe to the hull.
     */
    private fun createLinks(
        compiled: CompiledTrackSide,
        chassisPosition: Vector3f,
        chassisMatrix: Matrix3f,
        chassisRotation: Quaternion,
    ) {
        val pins = compiled.loop.pins
        val linkCount = compiled.loop.linkCount
        if (pins.size < linkCount + 1) throw TrackedGeometryException(
            "${compiled.side} belt exposes ${pins.size} pins for $linkCount links",
        )
        val template = compiled.linkTemplate
        val halfPitch = spec.pitch * .5

        val shape = BoxCollisionShape(
            (template.width * .5).toFloat().coerceAtLeast(.01f),
            (template.thickness * .5).toFloat().coerceAtLeast(.005f),
            (spec.pitch * .5).toFloat().coerceAtLeast(.01f),
        ).apply { margin = .001f }

        val lateral = (sideOfWheels[compiled.side]?.firstOrNull()?.wheel?.center?.x ?: 0.0).toFloat()
        val bodies = ArrayList<LinkBody>(linkCount)

        for (index in 0 until linkCount) {
            val a = pins[index]
            val b = pins[index + 1]
            val midZ = (a.z + b.z) * .5
            val midY = (a.y + b.y) * .5
            val length = sqrt((b.z - a.z) * (b.z - a.z) + (b.y - a.y) * (b.y - a.y))
            if (length < 1.0e-6) throw TrackedGeometryException("${compiled.side} link $index has zero length")
            val angle = Math.toDegrees(atan2(b.y - a.y, b.z - a.z)).toFloat()
            val local = Quaternion().fromAngleNormalAxis(Math.toRadians(-angle.toDouble()).toFloat(), Vector3f.UNIT_X)
            val world = chassisPosition.add(chassisMatrix.mult(Vector3f(lateral, midY.toFloat(), midZ.toFloat()), null))

            val body = PhysicsRigidBody(shape, spec.linkMassKg.toFloat().coerceAtLeast(1f)).apply {
                name = "track_link_${compiled.side.name.lowercase()}_$index"
                collisionGroup = COLLISION_LINK
                setCollideWithGroups(COLLISION_TERRAIN or COLLISION_WHEEL)
                // NONE here: the engine suppresses same-owner contacts only when
                // *both* masks lack the other group, so this keeps shoe-shoe off
                // while wheel-shoe stays on (the wheel's mask includes LINK).
                collideWithOwnerGroups = COLLISION_NONE
                friction = .9f
                restitution = 0f
                setDamping(.02f, .05f)
                setSleepingThresholds(.02f, .02f)
                ccdMotionThreshold = .05f
                ccdSweptSphereRadius = .02f
                setProtectGravity(true)
                setGravity(Vector3f())
                // A shoe mass is ~200x smaller than the load its contact must
                // carry, so Bullet's default soft contact lets it sink ~20 mm into
                // the ground. Machine-Max stiffens its contact rows explicitly
                // (CFM/ERP) for the same reason.
                setContactStiffness(LINK_CONTACT_STIFFNESS)
                setContactDamping(LINK_CONTACT_DAMPING)
                setContactProcessingThreshold(LINK_CONTACT_THRESHOLD)
                setPhysicsLocation(world)
                setPhysicsRotation(chassisRotation.mult(local))
                addBody(this)
                // A sleeping shoe never generates a new manifold, so the wheel
                // would pass straight through it. Wake the whole belt explicitly.
                activate(true)
            }
            bodies += LinkBody(index, body, halfPitch)
        }
        if (bodies.size < 4) throw TrackedGeometryException("${compiled.side} belt has only ${bodies.size} links")
        links += bodies

        for (index in bodies.indices) {
            val current = bodies[index]
            val next = bodies[(index + 1) % bodies.size]
            val hinge = PhysicsJoints.trackLinkHinge(
                current.body,
                next.body,
                Vector3f(0f, 0f, current.halfPitch.toFloat()),
                Vector3f(0f, 0f, -next.halfPitch.toFloat()),
                (Math.toRadians(spec.angularLimitDegrees).toFloat()).coerceAtLeast(0.05f),
            )
            hinge.setCollisionBetweenLinkedBodies(false)
            // A closed belt is one long constraint cycle; the default global
            // iteration budget is spent long before the loop closes. Machine-Max
            // overrides iterations per joint instead of raising the world budget.
            hinge.overrideIterations(TRACK_HINGE_ITERATIONS)
            addJoint(hinge)
            joints += hinge
        }
        sideOfLinks[compiled.side] = bodies
    }

    /** One substep: explicit gravity, spring/damper, tensioner. Called from PRE. */
    fun preStep(dt: Float) {
        val rotation = chassis.getPhysicsRotation(null)
        val matrix = rotation.toRotationMatrix()
        val chassisPosition = chassis.getPhysicsLocation(null)
        val chassisVelocity = chassis.getLinearVelocity(null)
        val chassisAngular = chassis.getAngularVelocity(null)
        val up = matrix.mult(Vector3f.UNIT_Y, null).normalize()

        for (side in TrackedSide.entries) {
            val sideWheels = sideOfWheels[side] ?: continue
            for (wheel in sideWheels) {
                // Gravity applies to every wheel, sprung or not. Only the spring
                // row is role-dependent.
                wheel.body.applyCentralForce(Vector3f(0f, -wheel.body.mass * GRAVITY, 0f))
                val spring = wheel.wheel.suspension
                if (!spring.isSprung) continue
                val anchorWorld = chassisPosition.add(matrix.mult(wheel.anchorLocal, null))
                val offset = wheel.body.getPhysicsLocation(null).subtract(anchorWorld)
                val travel = offset.dot(up).toDouble()
                val anchorVelocity = chassisVelocity.add(chassisAngular.cross(anchorWorld.subtract(chassisPosition), null))
                val relative = wheel.body.getLinearVelocity(null).subtract(anchorVelocity)
                val travelVelocity = relative.dot(up).toDouble()
                // Contact chatter must not be read as suspension velocity; a short
                // first-order filter keeps the damper from pumping energy.
                val filterAlpha = (dt / TRAVEL_VELOCITY_FILTER_SECONDS).coerceIn(0.0f, 1.0f)
                wheel.filteredVelocity += (travelVelocity - wheel.filteredVelocity) * filterAlpha
                wheel.travel = travel
                wheel.travelVelocity = travelVelocity
                val force = suspensionForce(spring, travel, wheel.filteredVelocity, wheel.dampingNsPerM)
                wheel.springForceN = force
                if (false) {
                    // The engine wrapper deliberately keeps the 6DOF spring disabled
                    // (guidedWheelAxle) because libbulletjme's spring2 row is not
                    // trustworthy here; the equal/opposite oleo pair is the supported
                    // equivalent of Machine-Max's joint spring.
                    PhysicsJoints.applyOleoForce(chassis, wheel.body, up, force.toFloat(), anchorWorld)
                }
                wheel.grounded = force > 0.0
                applyRollingResistance(wheel, matrix)
            }
            applyTensioner(side, matrix, chassisPosition, chassisVelocity, chassisAngular)
        }

        for (link in links) {
            link.body.applyCentralForce(Vector3f(0f, -link.body.mass * GRAVITY, 0f))
        }
    }

    /**
     * Spring law with an explicit free length: `travel` is measured from the
     * authored wheel position, so the spring is pre-compressed by exactly the
     * static sag `g / omega^2` at that position. That makes the model pose the
     * static equilibrium by construction, and any added static load sags the
     * hull for real instead of being compensated away.
     */
    private fun suspensionForce(
        spring: CompiledSuspension,
        travel: Double,
        velocity: Double,
        dampingNsPerM: Double,
    ): Double {
        val compression = travel + spring.staticCompressionM
        if (compression <= 0.0) return 0.0
        var force = spring.stiffnessNPerM * compression
        val bumpStart = spring.bumpStopStart * spring.travelUp
        if (compression > bumpStart) {
            val window = (spring.travelUp - bumpStart).coerceAtLeast(1.0e-3)
            force += spring.maxForceN * ((compression - bumpStart) / window).pow(spring.bumpStopExponent)
        }
        // Single stability-clamped coefficient: the asymmetric pair cannot both be
        // honoured inside the explicit-force stability envelope.
        force += dampingNsPerM * velocity
        return force.coerceIn(0.0, spring.maxForceN)
    }

    /**
     * Rolling resistance: an undriven wheel on a rigid floor has no mechanism
     * that ever removes its spin, so the hull keeps rolling forward forever.
     * The torque is bounded so it can decelerate the wheel to a stop within the
     * substep but can never spin it backwards.
     */
    private fun applyRollingResistance(wheel: WheelBody, matrix: Matrix3f) {
        val resistance = spec.drivetrain.rollingResistance
        if (resistance <= 0.0) return
        val axis = matrix.mult(Vector3f.UNIT_X, null).normalize()
        val omega = wheel.body.getAngularVelocity(null)
        val spin = omega.dot(axis).toDouble()
        if (abs(spin) < 1.0e-4) return
        val inverseInertia = wheel.body.getInverseInertiaLocal(null).x.toDouble()
        if (inverseInertia <= 1.0e-9) return
        val loadN = wheel.springForceN.coerceAtLeast(0.0)
        val dt = 1.0 / 60.0
        val maxDelta = abs(spin)
        val delta = (resistance * loadN * wheel.wheel.radius * inverseInertia * dt).coerceAtMost(maxDelta)
        val torque = delta / (inverseInertia * dt)
        wheel.body.applyTorque(axis.mult((-kotlin.math.sign(spin) * torque).toFloat(), null))
    }

    /** Asymmetric damper: compression and rebound coefficients differ. */
    private fun springDamping(spring: CompiledSuspension, velocity: Double = 0.0): Double =
        if (velocity >= 0.0) spring.compressionDampingNsPerM else spring.reboundDampingNsPerM

    /** Passive tensioner: bounded preload along the side's longitudinal axis. */
    private fun applyTensioner(
        side: TrackedSide,
        matrix: Matrix3f,
        chassisPosition: Vector3f,
        chassisVelocity: Vector3f,
        chassisAngular: Vector3f,
    ) {
        val wheel = sideOfWheels[side]?.firstOrNull { it.wheel.role == TrackWheelRole.IDLER_GUIDE } ?: return
        val tensioner = wheel.wheel.tensioner ?: return
        val axis = matrix.mult(Vector3f.UNIT_Z, null).normalize()
        val anchorWorld = chassisPosition.add(matrix.mult(wheel.anchorLocal, null))
        val along = wheel.body.getPhysicsLocation(null).subtract(anchorWorld).dot(axis).toDouble()
            .coerceIn(-tensioner.travel, tensioner.travel)
        val anchorVelocity = chassisVelocity.add(chassisAngular.cross(anchorWorld.subtract(chassisPosition), null))
        val velocity = wheel.body.getLinearVelocity(null).subtract(anchorVelocity).dot(axis).toDouble()
        val travel = tensioner.travel.coerceAtLeast(.01)
        val force = (tensioner.preloadN * (1.0 - along / travel) - velocity * tensioner.dampingNsPerM)
        if (force > 0.0) {
            PhysicsJoints.applyOleoForce(chassis, wheel.body, axis, force.toFloat(), anchorWorld)
        }
    }

    override fun close() {
        joints.forEach { runCatching { space.removeJoint(it) } }
        joints.clear()
        wheels.forEach { runCatching { space.removeCollisionObject(it.body) } }
        links.forEach { runCatching { space.removeCollisionObject(it.body) } }
        wheels.clear()
        links.clear()
        sideOfWheels.clear()
        sideOfLinks.clear()
        started = false
    }

    companion object {
        private const val GRAVITY = 9.81f
        private const val TRAVEL_VELOCITY_FILTER_SECONDS = 0.02f
        private const val TRACK_HINGE_ITERATIONS = 40
        private const val LINK_CONTACT_STIFFNESS = 1.0e8f
        private const val LINK_CONTACT_DAMPING = 1.0e5f
        private const val LINK_CONTACT_THRESHOLD = 0.02f

        /** Terrain group, mirroring CollisionGroups.TERRAIN. */
        const val COLLISION_TERRAIN = 1
        const val COLLISION_NONE = 0
        const val COLLISION_CHASSIS = 1 shl 4
        const val COLLISION_WHEEL = 1 shl 5
        const val COLLISION_LINK = 1 shl 6

        /**
         * Mirrors `PhysicsWorld.needsCollision`, so tests exercise the same
         * same-owner filtering the game uses instead of a simplified mask test.
         */
        @JvmStatic
        fun collisionAllowed(
            groupA: Int, maskA: Int, ownerMaskA: Int,
            groupB: Int, maskB: Int, ownerMaskB: Int,
            sameOwner: Boolean,
        ): Boolean {
            val result = (groupA and maskB) != 0 || (groupB and maskA) != 0
            if (!result) return false
            if (!sameOwner) return true
            return !((ownerMaskA and groupB) == 0 && (ownerMaskB and groupA) == 0)
        }
    }
}

private fun Vec3.bullet() = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
