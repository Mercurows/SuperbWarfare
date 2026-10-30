package com.atsuishio.superbwarfare.physics.tracked

import net.minecraft.world.phys.Vec3

/**
 * Executable description of one constraint, in terms every consumer can agree on.
 *
 * A mount or a run end is not "a revolute" or "a normal lock" as a label: it is
 * a pair of logical bodies, a pair of body-local frames, and six explicit axis
 * states. A runtime can then build the `New6Dof` without re-deriving geometry,
 * and a fixture can assert the same plan without guessing an axis from a body's
 * world orientation.
 *
 * These are **logical** references. A native Bullet body id only exists once the
 * runtime has created the body, so it cannot appear here.
 */
enum class CompiledBodyKind { CHASSIS, WHEEL, RUN }

/** Identity of one body inside one vehicle, stable across a recompile. */
data class CompiledBodyRef(
    val kind: CompiledBodyKind,
    val side: TrackedSide,
    /** Wheel index for [CompiledBodyKind.WHEEL], run index for [CompiledBodyKind.RUN], else -1. */
    val ordinal: Int,
) {
    override fun toString(): String = "${side.name.lowercase()}_${kind.name.lowercase()}_$ordinal"
}

/** A joint frame: an origin and a rotation, both in one body's local frame. */
data class CompiledConstraintFrame(
    val origin: Vec3,
    val rotation: CompiledRotation = CompiledRotation.IDENTITY,
)

/** One axis of a six-degree-of-freedom constraint. */
sealed interface CompiledAxisLimit {
    /** Truly free: no bound is imposed by the solver. */
    data object Free : CompiledAxisLimit

    /** Held at its bind value. */
    data object Locked : CompiledAxisLimit

    /** Free between two bounds, metres for a linear axis and radians for an angular one. */
    data class Limited(val lower: Double, val upper: Double) : CompiledAxisLimit {
        init {
            require(lower.isFinite() && upper.isFinite()) { "limit bounds must be finite: $lower..$upper" }
            require(lower <= upper) { "limit lower $lower exceeds upper $upper" }
        }

        val span: Double get() = upper - lower
    }
}

/**
 * One fully specified constraint between two bodies.
 *
 * [linear] and [angular] always hold exactly three entries in `X, Y, Z` order of
 * the joint frame, so a consumer cannot silently transpose an axis.
 */
data class CompiledJointPlan(
    val name: String,
    val bodyA: CompiledBodyRef,
    val bodyB: CompiledBodyRef,
    val frameInA: CompiledConstraintFrame,
    val frameInB: CompiledConstraintFrame,
    val linear: List<CompiledAxisLimit>,
    val angular: List<CompiledAxisLimit>,
    /** False only when the pair is deliberately excluded from contact. */
    val collisionEnabled: Boolean = true,
) {
    init {
        require(linear.size == 3) { "$name linear axis count ${linear.size}" }
        require(angular.size == 3) { "$name angular axis count ${angular.size}" }
        require(bodyA != bodyB) { "$name connects a body to itself: $bodyA" }
    }

    /** Indices of linear axes that are not locked; empty for a rigid mount. */
    val freeLinearAxes: List<Int> get() = linear.indices.filter { linear[it] !is CompiledAxisLimit.Locked }

    /** Indices of angular axes that are not locked. */
    val freeAngularAxes: List<Int> get() = angular.indices.filter { angular[it] !is CompiledAxisLimit.Locked }

    /**
     * The one translation the second run end holds. Null for any other plan.
     *
     * A second end must have exactly one non-free linear axis and no angular
     * lock; anything else is a different mechanism and is rejected here rather
     * than at assembly time.
     */
    val singleLockedTranslationAxis: Int?
        get() {
            val locked = linear.indices.filter { linear[it] is CompiledAxisLimit.Locked }
            return locked.singleOrNull()
        }

    /** True when the plan is the run's first end: all translation locked, one free rotation. */
    val isFirstRunEnd: Boolean
        get() = linear.all { it is CompiledAxisLimit.Locked } && freeAngularAxes == listOf(0)

    /** True when the plan is the run's second end: one locked translation, everything else free. */
    val isSecondRunEnd: Boolean
        get() = singleLockedTranslationAxis != null && freeAngularAxes.size == 3

    fun withCollision(enabled: Boolean): CompiledJointPlan = copy(collisionEnabled = enabled)
}

/** Axis indices, named so call sites never spell a bare integer. */
object CompiledAxis {
    const val LIN_X = 0
    const val LIN_Y = 1
    const val LIN_Z = 2
    const val ANG_X = 0
    const val ANG_Y = 1
    const val ANG_Z = 2
}
