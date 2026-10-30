package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.NativePhysicsLoader
import cn.solarmoon.spark_core.physics.PhysicsHost
import cn.solarmoon.spark_core.physics.body.CollisionGroups
import cn.solarmoon.spark_core.physics.body.collideWithOwnerGroups
import cn.solarmoon.spark_core.physics.body.owner
import cn.solarmoon.spark_core.physics.track.TrackRunAssembly
import cn.solarmoon.spark_core.physics.track.TrackRunSpec
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.collision.PersistentManifolds
import com.jme3.bullet.collision.PhysicsCollisionObject
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.collision.shapes.CylinderCollisionShape
import com.jme3.bullet.joints.New6Dof
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Vector3f
import net.minecraft.world.phys.Vec3

/**
 * Native B2 mechanism builder.
 *
 * This is deliberately **not** a second assembly implementation. It takes the
 * run bodies from [TrackRunAssembly], the specs from [TrackedRunSpecs], and the
 * end constraints from [TrackedJointFactory], i.e. the same three production
 * pieces a vehicle runtime composes. What it does not have is a Minecraft
 * `PhysicsLevel`, so it cannot exercise the `TrackRunRuntime` lifecycle; that
 * belongs to the runtime test and is not claimed here.
 *
 * Ownership: one host per mechanism, because the runs and their wheels are one
 * vehicle. Runs are filtered from sibling runs by the Core same-owner allow
 * list, not by inventing a second owner.
 */
object TrackedMechanismFixture {

    init {
        NativePhysicsLoader.load()
    }

    /** One assembled mechanism: wheels, runs, the external joints, and its owner. */
    class Mechanism(
        val space: PhysicsSpace,
        /** The compiled side this mechanism was built from, for measurement. */
        val view: CompiledTrackSide,
        val wheels: List<PhysicsRigidBody>,
        val runs: List<PhysicsRigidBody>,
        /** External end constraints, two per run. */
        val joints: List<New6Dof>,
        /** The plan each joint was built from, in [joints] order. */
        val jointPlans: List<CompiledJointPlan>,
        val host: PhysicsHost,
        /** Contact tallies accumulated by the fixture's own listener. */
        val contacts: ContactTally,
    ) : AutoCloseable {
        override fun close() {
            // Joints first: an external joint must never outlive either body.
            joints.forEach { space.removeJoint(it) }
            space.destroy()
        }

        fun wheelBody(wheelIndex: Int): PhysicsRigidBody =
            wheels[view.wheels.indexOfFirst { it.index == wheelIndex }]

        fun runBody(runIndex: Int): PhysicsRigidBody =
            runs[view.runs.indexOfFirst { it.index == runIndex }]

        fun step(substeps: Int) {
            repeat(substeps) {
                space.update(1f / 60f, 0, false, true, false)
                contacts.sample(space, wheels, runs)
            }
        }

        /** Sample contacts once, after a batch of steps. */
        fun sampleContacts() = contacts.sample(space, wheels, runs)

        /** Anchor gap of one run's first end, metres. */
        fun firstEndAnchorGap(run: CompiledRun): Double {
            val plan = run.first.joint
            val a = frameOrigin(plan.bodyA, plan.frameInA, run)
            val b = frameOrigin(plan.bodyB, plan.frameInB, run)
            return a.subtract(b).length().toDouble()
        }

        /** Largest anchor gap over every run's first end, metres. */
        fun firstEndAnchorGap(): Double {
            var worst = 0.0
            for (run in view.runs) {
                val plan = run.first.joint
                val a = frameOrigin(plan.bodyA, plan.frameInA, run)
                val b = frameOrigin(plan.bodyB, plan.frameInB, run)
                worst = maxOf(worst, a.subtract(b).length().toDouble())
            }
            return worst
        }

        /** Largest anchor gap over every run's second end, metres. */
        fun secondEndAnchorGap(): Double {
            var worst = 0.0
            for (run in view.runs) {
                val plan = run.second.joint
                val a = frameOrigin(plan.bodyA, plan.frameInA, run)
                val b = frameOrigin(plan.bodyB, plan.frameInB, run)
                worst = maxOf(worst, a.subtract(b).length().toDouble())
            }
            return worst
        }

        /** Largest locked-axis disagreement over every run's first end, radians. */
        fun firstEndAxisError(): Double {
            var worst = 0.0
            for (run in view.runs) {
                val plan = run.first.joint
                val a = frameAxis(plan.bodyA, plan.frameInA, run, Vec3(1.0, 0.0, 0.0))
                val b = frameAxis(plan.bodyB, plan.frameInB, run, Vec3(1.0, 0.0, 0.0))
                val dot = a.dot(b).coerceIn(-1f, 1f).toDouble()
                worst = maxOf(worst, kotlin.math.acos(dot))
            }
            return worst
        }

        /** Second-end anchor offset of one run, in that run body's own frame. */
        fun secondEndOffset(run: CompiledRun): Vector3f {
            val attachment = run.second
            val plan = attachment.joint
            val runBody = runBody(run.index)
            val wheelBody = wheelBody(attachment.wheelIndex)
            val runAnchor = worldPoint(runBody, plan.frameInA.origin)
            val wheelAnchor = worldPoint(wheelBody, plan.frameInB.origin)
            val delta = wheelAnchor.subtract(runAnchor)
            val inverse = runBody.getPhysicsRotation(null).inverse().toRotationMatrix()
            return inverse.mult(delta, Vector3f())
        }

        /** Tangential component of the second end's offset, metres. */
        fun secondEndTangentialSlip(run: CompiledRun): Double {
            val local = secondEndOffset(run)
            val normal = localNormal(run.second)
            val alongNormal = normal.mult(local.dot(normal))
            return local.subtract(alongNormal).length().toDouble()
        }

        /** Component along the second end's declared normal, metres. */
        fun secondEndNormalResidual(run: CompiledRun): Double {
            val local = secondEndOffset(run)
            return kotlin.math.abs(local.dot(localNormal(run.second)).toDouble())
        }

        /** Deepest overlap between any run box and any wheel, metres; zero means clear. */
        fun worstRunWheelPenetration(): Double {
            val audit = TrackedRunGeometryAudit.audit(view.runs, view.wheels, view.belt)
            return maxOf(0.0, -audit.minRunWheelGapM)
        }

        fun localNormal(attachment: CompiledRunAttachment): Vector3f {
            val normal = attachment.lockedNormalLocal!!.normalize()
            return Vector3f(normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat()).normalize()
        }

        private fun frameOrigin(
            body: CompiledBodyRef,
            frame: CompiledConstraintFrame,
            run: CompiledRun,
        ): Vector3f {
            val rotated = rotate(frame.origin, frame.rotation)
            return when (body.kind) {
                CompiledBodyKind.RUN -> worldPoint(runBody(run.index), rotated)
                CompiledBodyKind.WHEEL -> worldPoint(wheelBody(body.ordinal), rotated)
                CompiledBodyKind.CHASSIS -> error("a run end cannot reference the chassis")
            }
        }

        private fun frameAxis(
            body: CompiledBodyRef,
            frame: CompiledConstraintFrame,
            run: CompiledRun,
            localDir: Vec3,
        ): Vector3f {
            val inFrame = rotate(localDir, frame.rotation)
            val native = when (body.kind) {
                CompiledBodyKind.RUN -> runBody(run.index)
                CompiledBodyKind.WHEEL -> wheelBody(body.ordinal)
                CompiledBodyKind.CHASSIS -> error("a run end cannot reference the chassis")
            }
            val rotation = native.getPhysicsRotation(null).toRotationMatrix()
            return rotation.mult(inFrame.toBullet(), Vector3f()).normalize()
        }

        private fun rotate(point: Vec3, rotation: CompiledRotation): Vec3 {
            val q = com.jme3.math.Quaternion(rotation.x, rotation.y, rotation.z, rotation.w).normalizeLocal()
            val out = q.toRotationMatrix().mult(point.toBullet(), Vector3f())
            return Vec3(out.x.toDouble(), out.y.toDouble(), out.z.toDouble())
        }

        private fun worldOffset(body: PhysicsRigidBody, local: Vec3): Vector3f {
            val rotation = body.getPhysicsRotation(null).toRotationMatrix()
            return rotation.mult(local.toBullet(), Vector3f())
        }

        private fun worldPoint(body: PhysicsRigidBody, local: Vec3): Vector3f =
            body.getPhysicsLocation(null).add(worldOffset(body, local))
    }

    /** Counts contacts by the body classes a B2 load-path test has to separate. */
    class ContactTally {
        companion object {
            /**
             * Separation at or below which a manifold point counts as touching.
             *
             * Bullet keeps a manifold while the AABBs overlap, so a wheel hovering
             * ~90 mm above the floor can still have one. This is the same order as
             * the shapes' 1 mm margin plus the solver's contact slop.
             */
            const val CONTACT_DISTANCE_EPS_M = 0.005f
        }

        var runTerrain = 0
        var wheelTerrain = 0
        var chassisTerrain = 0
        var runWheel = 0
        var runChassis = 0
        var runRun = 0
        var other = 0
        /** Total normal impulse over run/terrain points in the last sample, N*s. */
        var runTerrainImpulseNs = 0f
        /** Total normal impulse over wheel/terrain points in the last sample, N*s. */
        var wheelTerrainImpulseNs = 0f
        /** Total normal impulse over chassis/terrain points in the last sample, N*s. */
        var chassisTerrainImpulseNs = 0f

        /**
         * Counts accumulated over every sample since [resetWindow], not just the
         * last one.
         *
         * A single trailing sample cannot prove "no wheel ever touched the
         * ground" over a 40 s window: a bypass may appear and disappear between
         * the samples a test happens to read. The window counters are what the
         * long-window assertions use.
         */
        var windowSamples = 0
        var windowRunTerrain = 0
        var windowWheelTerrain = 0
        var windowChassisTerrain = 0
        var windowRunWheel = 0
        var windowRunChassis = 0
        var windowRunRun = 0
        /** Per-wheel-body terrain contact counts accumulated over the window. */
        val windowWheelTerrainByIndex = HashMap<Int, Int>()
        /** Terrain normal impulse accumulated over the window, N*s. */
        var windowRunTerrainImpulseNs = 0.0
        var windowWheelTerrainImpulseNs = 0.0
        var windowChassisTerrainImpulseNs = 0.0

        /**
         * The same window totals, split by a caller-supplied body group.
         *
         * A whole-vehicle total cannot show that **each** side carries its own
         * share: one side could carry everything while the other merely hangs on
         * its mounts, and the summed impulse would look fine. B2.4 needs the
         * split, so the tally keeps per-group counters instead of making the test
         * re-derive them.
         */
        val windowRunTerrainByGroup = HashMap<String, Int>()
        val windowWheelTerrainByGroup = HashMap<String, Int>()
        val windowRunTerrainImpulseByGroup = HashMap<String, Double>()
        val windowWheelTerrainImpulseByGroup = HashMap<String, Double>()

        fun reset() {
            runTerrain = 0
            wheelTerrain = 0
            chassisTerrain = 0
            runWheel = 0
            runChassis = 0
            runRun = 0
            other = 0
            runTerrainImpulseNs = 0f
            wheelTerrainImpulseNs = 0f
            chassisTerrainImpulseNs = 0f
        }

        /** Start a fresh accumulation window; does not touch the per-sample fields. */
        fun resetWindow() {
            windowSamples = 0
            windowRunTerrain = 0
            windowWheelTerrain = 0
            windowChassisTerrain = 0
            windowRunWheel = 0
            windowRunChassis = 0
            windowRunRun = 0
            windowWheelTerrainByIndex.clear()
            windowRunTerrainImpulseNs = 0.0
            windowWheelTerrainImpulseNs = 0.0
            windowChassisTerrainImpulseNs = 0.0
            windowRunTerrainByGroup.clear()
            windowWheelTerrainByGroup.clear()
            windowRunTerrainImpulseByGroup.clear()
            windowWheelTerrainImpulseByGroup.clear()
        }

        /**
         * Sample the manifolds currently in [space].
         *
         * Bullet's manifold list is only meaningful inside the substep, so this
         * is called immediately after `update`, never after a batch of steps.
         * Contact classification is by body identity, not by shape size.
         */
        fun sample(
            space: PhysicsSpace,
            wheels: List<PhysicsRigidBody>,
            runs: List<PhysicsRigidBody>,
            chassis: PhysicsRigidBody? = null,
            groups: Map<PhysicsRigidBody, String> = emptyMap(),
        ) {
            reset()
            val runSet = runs.toHashSet()
            val wheelSet = wheels.toHashSet()
            for (manifold in space.listManifoldIds()) {
                val a = PhysicsCollisionObject.findInstance(PersistentManifolds.getBodyAId(manifold)) ?: continue
                val b = PhysicsCollisionObject.findInstance(PersistentManifolds.getBodyBId(manifold)) ?: continue
                val contact = contactOf(manifold) ?: continue
                val impulse = contact.second
                val aStatic = (a as? PhysicsRigidBody)?.isStatic == true
                val bStatic = (b as? PhysicsRigidBody)?.isStatic == true
                when {
                    a in runSet && bStatic -> {
                        runTerrain++
                        runTerrainImpulseNs += impulse
                        noteGroup(windowRunTerrainByGroup, windowRunTerrainImpulseByGroup, groups[a], impulse)
                    }
                    b in runSet && aStatic -> {
                        runTerrain++
                        runTerrainImpulseNs += impulse
                        noteGroup(windowRunTerrainByGroup, windowRunTerrainImpulseByGroup, groups[b], impulse)
                    }
                    a in wheelSet && bStatic -> {
                        wheelTerrain++
                        wheelTerrainImpulseNs += impulse
                        noteWheelContact(wheels.indexOf(a))
                        noteGroup(windowWheelTerrainByGroup, windowWheelTerrainImpulseByGroup, groups[a], impulse)
                    }
                    b in wheelSet && aStatic -> {
                        wheelTerrain++
                        wheelTerrainImpulseNs += impulse
                        noteWheelContact(wheels.indexOf(b))
                        noteGroup(windowWheelTerrainByGroup, windowWheelTerrainImpulseByGroup, groups[b], impulse)
                    }
                    chassis != null && a === chassis && bStatic -> {
                        chassisTerrain++
                        chassisTerrainImpulseNs += impulse
                    }
                    chassis != null && b === chassis && aStatic -> {
                        chassisTerrain++
                        chassisTerrainImpulseNs += impulse
                    }
                    a in runSet && b in wheelSet || b in runSet && a in wheelSet -> runWheel++
                    chassis != null && (a in runSet && b === chassis || b in runSet && a === chassis) -> runChassis++
                    a in runSet && b in runSet -> runRun++
                    else -> other++
                }
            }
            windowSamples++
            windowRunTerrain += runTerrain
            windowWheelTerrain += wheelTerrain
            windowChassisTerrain += chassisTerrain
            windowRunWheel += runWheel
            windowRunChassis += runChassis
            windowRunRun += runRun
            windowRunTerrainImpulseNs += runTerrainImpulseNs.toDouble()
            windowWheelTerrainImpulseNs += wheelTerrainImpulseNs.toDouble()
            windowChassisTerrainImpulseNs += chassisTerrainImpulseNs.toDouble()
        }

        /**
         * Decide whether a manifold is a real contact, and sum its normal impulse.
         *
         * Bullet keeps a manifold while the two **AABBs** overlap, which is wider
         * than the shapes: a wheel 90 mm above the floor still has a manifold.
         * Counting manifolds therefore reports phantom terrain contacts. A point
         * counts only when it is actually touching (distance at or below the
         * tolerance) or when the solver applied a normal impulse.
         */
        private fun contactOf(manifold: Long): Pair<Boolean, Float>? {
            var impulse = 0f
            var touching = false
            for (point in PersistentManifolds.listPointIds(manifold)) {
                val distance = com.jme3.bullet.collision.ManifoldPoints.getDistance1(point)
                val applied = com.jme3.bullet.collision.ManifoldPoints.getAppliedImpulse(point)
                if (distance <= CONTACT_DISTANCE_EPS_M || applied > 1.0e-4f) touching = true
                if (applied > 0f) impulse += applied
            }
            return if (touching) true to impulse else null
        }

        private fun noteWheelContact(index: Int) {
            if (index < 0) return
            windowWheelTerrainByIndex[index] = (windowWheelTerrainByIndex[index] ?: 0) + 1
        }

        /** Attribute one terrain contact to its group, if the caller declared one. */
        private fun noteGroup(
            counts: HashMap<String, Int>,
            impulses: HashMap<String, Double>,
            group: String?,
            impulse: Float,
        ) {
            if (group == null) return
            counts[group] = (counts[group] ?: 0) + 1
            impulses[group] = (impulses[group] ?: 0.0) + impulse.toDouble()
        }
    }

    /**
     * Build a mechanism from [memberCount] consecutive belt members of one side.
     *
     * [memberCount] equal to the side's member count builds the complete side;
     * a smaller value is a diagnostic mechanism and must be labelled as such by
     * the caller. The mass budget is still the side's own authored budget, so a
     * two-member diagnostic does not silently get a different mass policy.
     */
    fun build(
        spec: TrackedVehicleSpec,
        side: TrackedSide,
        memberCount: Int,
        lockSecondEnd: Boolean = false,
        gravity: Vector3f = Vector3f(),
        contactResponse: Boolean = false,
        withTerrain: Boolean = false,
        kinematicWheels: Boolean = false,
    ): Mechanism {
        val compiled = spec.side(side)
        require(memberCount in 2..compiled.members.size) {
            "memberCount $memberCount is outside 2..${compiled.members.size}"
        }
        val members = compiled.members.take(memberCount)
        val wheels = members.map { member -> compiled.wheels.single { it.index == member.wheelIndex } }
        val belt = compiled.belt
        val byIndex = wheels.associateBy { it.index }
        val masses = TrackedMassAllocator.allocateRuns(members, byIndex, sideBudgetKg = compiled.runMassTotalKg)
        val runs = TrackedRunGeometry.compile(members, byIndex, belt, masses)

        val view = CompiledTrackSide(
            side = side,
            wheels = wheels,
            belt = belt,
            beltOrder = members.map { it.wheelIndex },
            members = members,
            runs = runs,
            mounts = compiled.mounts.filter { mount -> byIndex.containsKey(mount.wheelIndex) },
            suspension = compiled.suspension,
            legacy = null,
        )

        val space = PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT)
        space.setGravity(gravity)

        val host = object : PhysicsHost {
            override val physicsLevel: cn.solarmoon.spark_core.physics.level.PhysicsLevel
                get() = throw UnsupportedOperationException("native fixture has no PhysicsLevel")
            override val allPhysicsBodies: MutableMap<String, PhysicsCollisionObject> =
                java.util.concurrent.ConcurrentHashMap()
        }

        val tally = ContactTally()
        val wheelBodies = wheels.map { wheel ->
            val shape = CylinderCollisionShape(
                Vector3f(wheel.halfWidth.toFloat(), wheel.radius.toFloat(), wheel.radius.toFloat()),
                0,
            ).apply { margin = .001f }
            val body = PhysicsRigidBody(shape, if (kinematicWheels) 0f else wheel.massKg.toFloat())
            if (kinematicWheels) body.isKinematic = true
            body.isContactResponse = contactResponse
            body.setPhysicsLocation(wheel.assemblyCenter.toBullet())
            body.owner = host
            body.collisionGroup = CollisionGroups.WHEEL
            body.setCollideWithGroups(TrackedTerrainCollision.TERRAIN_MESH or CollisionGroups.TRACK_LINK)
            body.collideWithOwnerGroups = CollisionGroups.TRACK_LINK
            body.friction = spec.drivetrain.friction.toFloat()
            body.setSleepingThresholds(0f, 0f)
            space.addCollisionObject(body)
            body
        }
        val byWheelIndex = wheels.indices.associate { wheels[it].index to wheelBodies[it] }

        if (withTerrain) {
            val floor = PhysicsRigidBody(BoxCollisionShape(60f, 0.5f, 60f), 0f)
            floor.setPhysicsLocation(Vector3f(0f, -8f, 0f))
            floor.friction = .9f
            floor.collisionGroup = TrackedTerrainCollision.TERRAIN_MESH
            floor.setCollideWithGroups(CollisionGroups.NONE)
            space.addCollisionObject(floor)
        }

        val specs = TrackedRunSpecs.compileRuns(view, spec.vehicleId, spec.drivetrain.friction)
        TrackRunAssemblyProbe.requireMatches(specs, view)

        val runBodies = ArrayList<PhysicsRigidBody>(runs.size)
        val joints = ArrayList<New6Dof>(runs.size * 2)
        val jointPlans = ArrayList<CompiledJointPlan>(runs.size * 2)
        for ((index, run) in runs.withIndex()) {
            // The production assembly creates the link with the spec's shape,
            // ownership, masks, friction, damping and pose. Reusing it is what
            // makes this fixture a test of the adapter rather than of a copy.
            val body = TrackRunAssembly.createBody(
                specs[index],
                0,
                host,
                TrackRunAssembly.bodyName(specs[index], 0),
            )
            // A fixture without terrain is testing constraints, not contacts.
            // The Core assembly enables contact response for production; here it
            // is turned off so sibling run boxes cannot push each other apart.
            body.isContactResponse = contactResponse
            space.addCollisionObject(body)
            runBodies += body

            for (attachment in run.attachments) {
                val plan = if (lockSecondEnd && attachment.end == RunEnd.SECOND) {
                    // Negative control: a fully welded second end, which is the
                    // mistake the single-normal constraint exists to avoid.
                    attachment.joint.copy(linear = List(3) { CompiledAxisLimit.Locked })
                } else {
                    attachment.joint
                }
                val bodyA = resolve(plan.bodyA, run, body, byWheelIndex)
                val bodyB = resolve(plan.bodyB, run, body, byWheelIndex)
                val joint = TrackedJointFactory.create(plan, bodyA, bodyB)
                space.addJoint(joint)
                joints += joint
                jointPlans += plan
            }
        }

        return Mechanism(space, view, wheelBodies, runBodies, joints, jointPlans, host, tally)
    }

    private fun resolve(
        ref: CompiledBodyRef,
        run: CompiledRun,
        runBody: PhysicsRigidBody,
        byWheelIndex: Map<Int, PhysicsRigidBody>,
    ): PhysicsRigidBody = when (ref.kind) {
        CompiledBodyKind.WHEEL -> byWheelIndex.getValue(ref.ordinal)
        CompiledBodyKind.RUN -> runBody
        CompiledBodyKind.CHASSIS -> error("a run end cannot reference the chassis")
    }

    private fun Vec3.toBullet(): Vector3f = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
}
