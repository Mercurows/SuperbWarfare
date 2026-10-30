package com.atsuishio.superbwarfare.physics.tracked

import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Static geometry of the physical belt runs.
 *
 * One run spans each adjacent pair of belt members, **including the last back to
 * the first**. That is the topology recovered from MCSP: the run is a long box
 * that lies on the belt's own centre line, attached to the first wheel centre by
 * a revolute and to the second by a single normal constraint.
 *
 * Everything here is pure data in the chassis frame. No Bullet object, no
 * Minecraft access, no rendering: the same list feeds the geometry audit, the
 * mass audit and the `TrackRunSpec` adapter so those three cannot disagree.
 *
 * ## Where the box sits
 *
 * The belt is a band of thickness `t` on the outside of the wheel. **SBW's
 * current choice** is: the inner face touches the wheel collider at radius `R`,
 * the outer face is at `R + t`, so the run's centre line is at `R + t/2` and its
 * radial half extent is `t/2`.
 *
 * This is a **candidate mapping, not a recovered MCSP native formula.** It holds
 * only if the authored radius is the wheel collision radius and `t` is the outer
 * belt thickness. MCSP's Java bridge passes `contactRadius = R + t` into its
 * visible path solver, but the visible path is not the native run cuboid and its
 * `t/2` step cannot be transferred as proof. R0 calibration of the collision
 * shape, radius source and thickness is still the gate before this mapping can
 * be called audited. A box centred at `R - t/2` would overlap the wheel by a full
 * thickness, which is what an earlier version of this file did.
 *
 * The lateral extent is the authored belt width and the lateral centre is the
 * audited belt plane, not the mean of the wheel centres: on 99A those differ by
 * more than 160 mm.
 */
object TrackedRunGeometry {

    /**
     * A run's local frame: the box's local `+Z` follows the run direction,
     * `+X` stays lateral and `+Y` is the belt's outward normal. Rotation is
     * therefore about `X` only.
     */
    private const val LATERAL_AXIS = 0

    /** One tangent solution, in the chassis frame. */
    private data class Tangent(
        /** Unit normal from the wheel centre towards the belt centre line. */
        val normal: Vec3,
        /** Belt centre line at the first member, chassis frame. */
        val firstContact: Vec3,
        /** Belt centre line at the second member, chassis frame. */
        val secondContact: Vec3,
    )

    /**
     * Build one run per adjacent member pair, in traversal order.
     *
     * [members] must be in traversal order and cover each wheel exactly once.
     * [runMasses] comes from [TrackedMassAllocator]: this object places boxes and
     * does not decide how the belt budget is shared.
     */
    fun compile(
        members: List<CompiledBeltMember>,
        wheels: Map<Int, CompiledWheel>,
        belt: CompiledBeltGeometry,
        runMasses: List<Double>,
    ): List<CompiledRun> {
        require(members.size >= 2) { "a belt needs at least two members, had ${members.size}" }
        require(runMasses.size == members.size) {
            "expected ${members.size} run masses, got ${runMasses.size}"
        }
        require(members.map { it.ordinal } == members.indices.toList()) {
            "belt member ordinals must be 0..${members.lastIndex} in order, were ${members.map { it.ordinal }}"
        }
        require(members.map { it.wheelIndex }.distinct().size == members.size) {
            "a wheel may appear only once in the belt order: ${members.map { it.wheelIndex }}"
        }
        runMasses.forEachIndexed { index, mass ->
            require(mass > 0.0 && mass.isFinite()) { "run $index mass must be positive and finite, was $mass" }
        }

        val orientation = loopOrientation(members, wheels)
        val runs = ArrayList<CompiledRun>(members.size)
        for (i in members.indices) {
            val member = members[i]
            val next = members[(i + 1) % members.size]
            val first = wheels[member.wheelIndex]
                ?: throw TrackedGeometryException("belt member ${member.bone} has no compiled wheel")
            val second = wheels[next.wheelIndex]
                ?: throw TrackedGeometryException("belt member ${next.bone} has no compiled wheel")

            val tangent = tangent(member, next, first, second, belt, orientation)
            val axis = tangent.secondContact.subtract(tangent.firstContact)
            // Length and orientation must share one basis. The wheels may sit at
            // different lateral positions, so the 3D span is longer than the
            // in-plane run; using it with a rotation that only consumes (y, z)
            // would inflate the box along an axis it does not follow. The lateral
            // difference is carried by the run-local anchors instead.
            val span = hypot(axis.y, axis.z)
            require(span > 1.0e-6) {
                "run ${member.bone} -> ${next.bone} has a degenerate span $span m"
            }
            val halfLength = span * 0.5 + belt.halfThicknessM
            // The box lives on the audited belt plane. The wheel centres are not
            // averaged into it: on 99A that would shift the belt by >160 mm. The
            // wheels keep their own lateral positions, and the difference shows
            // up in each run-local anchor instead.
            val centre = Vec3(
                belt.centerPlaneX,
                (tangent.firstContact.y + tangent.secondContact.y) * 0.5,
                (tangent.firstContact.z + tangent.secondContact.z) * 0.5,
            )
            val angle = atan2(-axis.y, axis.z).toFloat()
            val rotation = TrackedAssemblyGeometry.rotationQuaternion(angle)
            val halfExtents = Vec3(belt.widthM * 0.5, belt.halfThicknessM, halfLength)

            runs += CompiledRun(
                index = i,
                firstWheelIndex = member.wheelIndex,
                secondWheelIndex = next.wheelIndex,
                closesLoop = i == members.lastIndex,
                center = centre,
                rotation = rotation,
                halfExtents = halfExtents,
                massKg = runMasses[i],
                first = attachment(
                    end = RunEnd.FIRST,
                    wheel = first,
                    runCenter = centre,
                    rotation = rotation,
                    runIndex = i,
                ),
                second = attachment(
                    end = RunEnd.SECOND,
                    wheel = second,
                    runCenter = centre,
                    rotation = rotation,
                    normal = tangent.normal,
                    runIndex = i,
                ),
            )
        }
        return runs
    }

    /**
     * Both anchors are the **wheel centres**, not the tangent points: the first
     * end's revolute sits on the wheel centre and the second end holds the run's
     * normal at the next wheel's centre. The anchor is expressed in the run's
     * local frame so a runtime can build the joint without re-deriving geometry.
     */
    private fun attachment(
        end: RunEnd,
        wheel: CompiledWheel,
        runCenter: Vec3,
        rotation: CompiledRotation,
        runIndex: Int,
        normal: Vec3? = null,
    ): CompiledRunAttachment {
        val anchorInRun = inverseRotateAboutX(wheel.assemblyCenter.subtract(runCenter), rotation)
        val wheelRef = CompiledBodyRef(CompiledBodyKind.WHEEL, wheel.side, wheel.index)
        val runRef = CompiledBodyRef(CompiledBodyKind.RUN, wheel.side, runIndex)
        val name = "${wheel.side.name.lowercase()}_run_${runIndex}_${end.name.lowercase()}"

        return when (end) {
            RunEnd.FIRST -> CompiledRunAttachment(
                end = end,
                wheelIndex = wheel.index,
                anchorInWheel = Vec3.ZERO,
                anchorInRun = anchorInRun,
                frameInWheel = CompiledRotation.IDENTITY,
                frameInRun = CompiledRotation.IDENTITY,
                joint = CompiledJointPlan(
                    name = name,
                    bodyA = wheelRef,
                    bodyB = runRef,
                    frameInA = CompiledConstraintFrame(Vec3.ZERO),
                    frameInB = CompiledConstraintFrame(anchorInRun),
                    linear = List(3) { CompiledAxisLimit.Locked },
                    // The run's local X is the wheel axle (the box only rotates
                    // about X), so freeing ANG_X is a true revolute about the
                    // axle. A frame that instead put the axle on Y would have to
                    // be rotated here; leaving both frames identity is correct
                    // only because X is already the axle.
                    angular = listOf(
                        CompiledAxisLimit.Free,
                        CompiledAxisLimit.Locked,
                        CompiledAxisLimit.Locked,
                    ),
                ),
            )

            RunEnd.SECOND -> {
                val normalInRun = inverseRotateAboutX(
                    requireNotNull(normal) { "the second end needs the run normal" },
                    rotation,
                ).normalize()
                // Orient the joint frame so its local Y is the run normal; the
                // plan then reads "lock LIN_Y" with no ambiguity about which
                // physical direction that is. The run only rotates about X, so
                // the normal always stays in the (y, z) plane.
                val frameRotation = rotationTakingYTo(normalInRun)
                CompiledRunAttachment(
                    end = end,
                    wheelIndex = wheel.index,
                    anchorInWheel = Vec3.ZERO,
                    anchorInRun = anchorInRun,
                    frameInWheel = CompiledRotation.IDENTITY,
                    frameInRun = CompiledRotation.IDENTITY,
                    lockedNormalLocal = normalInRun,
                    joint = CompiledJointPlan(
                        name = name,
                        bodyA = runRef,
                        bodyB = wheelRef,
                        frameInA = CompiledConstraintFrame(anchorInRun, frameRotation),
                        frameInB = CompiledConstraintFrame(Vec3.ZERO, frameRotation),
                        linear = listOf(
                            CompiledAxisLimit.Free,
                            CompiledAxisLimit.Locked,
                            CompiledAxisLimit.Free,
                        ),
                        angular = List(3) { CompiledAxisLimit.Free },
                    ),
                )
            }
        }
    }

    /** A rotation about `X` that maps `+Y` onto [direction]. */
    private fun rotationTakingYTo(direction: Vec3): CompiledRotation {
        require(abs(direction.x) < 1.0e-6) {
            "the planar belt model expects the run normal in the (y, z) plane, was $direction"
        }
        val angle = atan2(direction.z, direction.y)
        return CompiledRotation(
            x = sin(angle * 0.5).toFloat(),
            y = 0f,
            z = 0f,
            w = cos(angle * 0.5).toFloat(),
        )
    }

    /**
     * Tangent between two belt members, using signed radii.
     *
     * Folding the wrap into a signed radius is what makes the four wrap
     * combinations fall out of one equation instead of an `inside || inside`
     * special case: with `rho = +r` for an outside wrap and `rho = -r` for an
     * inside one, the contact points `P = C + rho * n` are perpendicular to the
     * segment only when
     *
     * ```text
     * n · (C_j - C_i) = rho_i - rho_j
     * ```
     *
     * which is the external tangent for two outside wraps and the internal
     * tangent as soon as one wrap is inside. Putting both contact points on the
     * same side, as an earlier version did, satisfies neither.
     *
     * The two algebraic solutions are mirror images. The outward side follows
     * the traversal direction and the loop winding, never a polygon centroid: a
     * two-member belt has no interior, so its centroid coincides with both
     * pairs' midpoints and a centroid test cannot produce the two opposite runs
     * that a two-wheel belt requires.
     */
    private fun tangent(
        firstMember: CompiledBeltMember,
        secondMember: CompiledBeltMember,
        first: CompiledWheel,
        second: CompiledWheel,
        belt: CompiledBeltGeometry,
        orientation: Double,
    ): Tangent {
        val d = Vec3(
            0.0,
            second.assemblyCenter.y - first.assemblyCenter.y,
            second.assemblyCenter.z - first.assemblyCenter.z,
        )
        val length = sqrt(d.y * d.y + d.z * d.z)
        require(length > 1.0e-6) {
            "wheels ${first.bone} and ${second.bone} share a centre in the belt plane"
        }
        if (firstMember.wrap == TrackWrap.OUTSIDE && secondMember.wrap == TrackWrap.OUTSIDE) {
            require(length > first.radius + second.radius) {
                "wheels ${first.bone} (r=${first.radius}) and ${second.bone} (r=${second.radius}) overlap: " +
                    "centre distance $length m"
            }
        }
        val rhoFirst = belt.runCenterOffset(first.radius) * wrapSign(firstMember.wrap)
        val rhoSecond = belt.runCenterOffset(second.radius) * wrapSign(secondMember.wrap)
        val along = (rhoFirst - rhoSecond) / length
        require(abs(along) < 1.0 - 1.0e-9) {
            "no belt run from ${first.bone} to ${second.bone}: the circles overlap or are nested " +
                "(rho $rhoFirst / $rhoSecond, centre distance $length)"
        }
        val across = sqrt(1.0 - along * along)

        // Unit centre-line direction and its left perpendicular, both in (z, y).
        val uz = d.z / length
        val uy = d.y / length
        val leftZ = -uy
        val leftY = uz

        val candidates = listOf(
            Vec3(0.0, along * uy + across * leftY, along * uz + across * leftZ),
            Vec3(0.0, along * uy - across * leftY, along * uz - across * leftZ),
        )

        // Away from the loop interior: for a counter-clockwise loop in the
        // `(z, y)` plane the interior is to the left of travel, so "outside" is
        // the right normal `(u_y, -u_z)`. An inside wrap puts the belt on the
        // other side of that.
        val outward = Vec3(0.0, -orientation * uz, orientation * uy)
        val preferred = if (firstMember.wrap == TrackWrap.OUTSIDE) outward else outward.scale(-1.0)
        val normal = candidates.maxByOrNull { it.dot(preferred) } ?: candidates.first()
        require(normal.dot(preferred) > 0.0) {
            "tangent normal for ${first.bone} -> ${second.bone} points into the belt"
        }
        return Tangent(
            normal = normal,
            firstContact = first.assemblyCenter.add(normal.scale(rhoFirst)),
            secondContact = second.assemblyCenter.add(normal.scale(rhoSecond)),
        )
    }

    private fun wrapSign(wrap: TrackWrap): Double = if (wrap == TrackWrap.OUTSIDE) 1.0 else -1.0

    /**
     * Winding of the belt order in the `(z, y)` plane: `+1` counter-clockwise,
     * `-1` clockwise.
     *
     * A two-member belt is a degenerate polygon with zero area, so there is no
     * winding to read. Its two runs cover the two tangents by construction,
     * because each run's outward side follows its own travel direction, so the
     * `+1` default is harmless rather than a guess about which tangent is first.
     */
    private fun loopOrientation(members: List<CompiledBeltMember>, wheels: Map<Int, CompiledWheel>): Double {
        if (members.size < 3) return 1.0
        var area2 = 0.0
        for (i in members.indices) {
            val a = wheels[members[i].wheelIndex]?.assemblyCenter ?: continue
            val b = wheels[members[(i + 1) % members.size].wheelIndex]?.assemblyCenter ?: continue
            area2 += a.z * b.y - b.z * a.y
        }
        require(abs(area2) > 1.0e-9) {
            "belt wheel centres are collinear in the (z, y) plane; the loop has no winding"
        }
        return if (area2 > 0.0) 1.0 else -1.0
    }

    /** Inverse of a rotation about `X`; used to express a chassis point in a run frame. */
    fun inverseRotateAboutX(point: Vec3, rotation: CompiledRotation): Vec3 {
        val angle = 2.0 * atan2(rotation.x.toDouble(), rotation.w.toDouble())
        val c = cos(-angle)
        val s = sin(-angle)
        return Vec3(
            point.x,
            point.y * c - point.z * s,
            point.y * s + point.z * c,
        )
    }

    /** The axis a run box rotates about; kept explicit so audits can assert it. */
    val runRotationAxis: Int get() = LATERAL_AXIS
}
