package com.atsuishio.superbwarfare.physics.tracked

import com.jme3.bullet.joints.motors.MotorParam
import com.jme3.math.Vector3f
import net.minecraft.world.phys.Vec3
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs

/**
 * B2.1: two wheel bodies, two runs, and the four end constraints between them.
 *
 * The mechanism is the smallest one that exercises every rule:
 *
 * ```text
 *   wheelA --first end--> run A->B --second end--> wheelB
 *   wheelB --first end--> run B->A --second end--> wheelA
 * ```
 *
 * Run bodies are created by `TrackRunAssembly` from specs produced by
 * `TrackedRunSpecs.compileRuns`, and the ends by `TrackedJointFactory`, so this
 * fixture tests the same adapters the runtime uses. It still does not exercise
 * `TrackRunRuntime`'s lifecycle: that is the separate runtime test.
 *
 * Two-member belts are diagnostic mechanisms, not a reduced acceptance claim:
 * the acceptance mechanism is the complete side in B2.2.
 */
class TrackedTwoWheelRunNativeTest {

    private val resources = File("src/main/resources")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun spec(): TrackedVehicleSpec = TrackedVehicleCompiler.compile(
        "superbwarfare:ztz_99a",
        json.decodeFromString(
            TrackedVehicleDefinition.serializer(),
            File(resources, "data/superbwarfare/sbw/tracked/ztz_99a.json").readText(),
        ),
        File(resources, "assets/superbwarfare/models/bedrock/vehicle/ztz_99a.geo.json").readText(),
    ).requireSpec()

    private fun mechanism(lockSecondEnd: Boolean = false) =
        TrackedMechanismFixture.build(spec(), TrackedSide.LEFT, 2, lockSecondEnd = lockSecondEnd)

    /**
     * The first end must hold its two anchors together.
     *
     * The run is also turned about its free axle: a "revolute" that cannot turn
     * is a welded pair, and an anchor gap test alone would not see that.
     */
    @Test
    fun theFirstEndHoldsItsAnchorsWhileTheRunTurns() {
        mechanism().use { mechanism ->
            val before = mechanism.firstEndAnchorGap()
            assertTrue(before < 1e-4, "assembly anchor gap $before m")

            // A torque about the free axle, on the run body only. The wheel is
            // held by its first-end revolute, so the run must rotate.
            val startAngle = mechanism.runs[0].getPhysicsRotation(null)
            mechanism.runs.forEach {
                it.applyTorqueImpulse(Vector3f(60f, 0f, 0f))
            }
            mechanism.step(B2Sampling.SHORT_CHAIN_SUBSTEPS)

            val after = mechanism.firstEndAnchorGap()
            assertTrue(
                after < 5e-3,
                "the first end must hold its anchors over ${B2Sampling.SHORT_CHAIN_SUBSTEPS} substeps, gap $after m",
            )
            val axis = mechanism.firstEndAxisError()
            assertTrue(axis < 5e-3, "first-end locked axes must stay aligned, worst $axis rad")

            val endAngle = mechanism.runs[0].getPhysicsRotation(null)
            val dot = abs(
                (startAngle.x * endAngle.x + startAngle.y * endAngle.y +
                    startAngle.z * endAngle.z + startAngle.w * endAngle.w).toDouble(),
            ).coerceIn(0.0, 1.0)
            val turned = 2.0 * kotlin.math.acos(dot)
            assertTrue(turned > 1e-3, "the first end must allow the run to turn about its axle, turned $turned rad")
        }
    }

    /**
     * The second end must let the run slide tangentially, on both runs.
     *
     * The push is applied along each run's own tangent, not a world axis: a
     * world-Y push can be mostly normal for one of the two runs, which would
     * make the test silently measure the wrong direction.
     */
    @Test
    fun theSecondEndAllowsTangentialSlidingOnBothRuns() {
        val spec = spec()
        val free = TrackedMechanismFixture.build(spec, TrackedSide.LEFT, 2).use { mechanism ->
            mechanism.step(120)
            val before = mechanism.view.runs.associateWith { mechanism.secondEndTangentialSlip(it) }
            mechanism.view.runs.forEach { run ->
                mechanism.runBody(run.index).applyCentralForce(mechanism.tangentForce(run, 400f))
            }
            mechanism.step(120)
            mechanism.view.runs.map { run ->
                abs(mechanism.secondEndTangentialSlip(run) - before.getValue(run))
            }
        }
        free.forEachIndexed { index, moved ->
            assertTrue(moved > 1e-4, "run $index must slide tangentially, moved $moved m")
        }

        val welded = TrackedMechanismFixture.build(spec, TrackedSide.LEFT, 2, lockSecondEnd = true).use { mechanism ->
            mechanism.step(120)
            val before = mechanism.view.runs.associateWith { mechanism.secondEndTangentialSlip(it) }
            mechanism.view.runs.forEach { run ->
                mechanism.runBody(run.index).applyCentralForce(mechanism.tangentForce(run, 400f))
            }
            mechanism.step(120)
            mechanism.view.runs.map { run ->
                abs(mechanism.secondEndTangentialSlip(run) - before.getValue(run))
            }
        }
        welded.forEachIndexed { index, moved ->
            assertTrue(
                moved < free[index] * 0.1,
                "a fully locked second end must not slide like the real one at run $index: " +
                    "welded $moved m vs free ${free[index]} m",
            )
        }
    }

    /**
     * The second end must not let the run leave along its declared normal.
     *
     * The perturbation is along that same normal, so a missing lock shows up
     * directly instead of being diluted by tangential motion.
     */
    @Test
    fun theSecondEndHoldsItsNormalOnBothRuns() {
        mechanism().use { mechanism ->
            mechanism.step(120)
            val before = mechanism.view.runs.associateWith { mechanism.secondEndNormalResidual(it) }
            mechanism.view.runs.forEach { run ->
                mechanism.runBody(run.index).applyCentralForce(mechanism.normalForce(run, 400f))
            }
            mechanism.step(120)
            mechanism.view.runs.forEach { run ->
                val after = mechanism.secondEndNormalResidual(run)
                assertTrue(
                    after < 0.02,
                    "run ${run.index} normal residual must stay bounded, was $after m " +
                        "(assembly ${before.getValue(run)} m)",
                )
            }
        }
    }

    /** A mechanism with no load must not drift on its own. */
    @Test
    fun theMechanismDoesNotDriftWithoutLoad() {
        mechanism().use { mechanism ->
            val start = mechanism.runs.map { it.getPhysicsLocation(null).clone() }
            mechanism.step(B2Sampling.SHORT_CHAIN_SUBSTEPS)
            mechanism.runs.forEachIndexed { index, body ->
                val moved = start[index].distance(body.getPhysicsLocation(null))
                assertTrue(moved < 0.01, "run $index drifted $moved m with no load")
            }
        }
    }

    /**
     * Every built joint must carry exactly the plan's six axis states.
     *
     * This replaces an earlier assertion that was a tautology: it checked that a
     * list was either non-empty or empty, which cannot fail. Each joint is now
     * paired with the plan it was created from.
     */
    @Test
    fun theBuiltJointsCarryTheCompiledAxisStates() {
        mechanism().use { mechanism ->
            assertEquals(4, mechanism.joints.size, "two runs, two ends each")
            assertEquals(4, mechanism.jointPlans.size, "every joint must retain its plan")
            mechanism.joints.forEachIndexed { index, joint ->
                val plan = mechanism.jointPlans[index]
                for (axis in 0..2) assertLimitMatches(joint, axis, plan.linear[axis], "${plan.name} linear $axis")
                for (axis in 0..2) {
                    assertLimitMatches(
                        joint,
                        cn.solarmoon.spark_core.physics.body.PhysicsJoints.ANG_X + axis,
                        plan.angular[axis],
                        "${plan.name} angular $axis",
                    )
                }
            }
        }
    }

    private fun assertLimitMatches(
        joint: com.jme3.bullet.joints.New6Dof,
        axis: Int,
        limit: CompiledAxisLimit,
        label: String,
    ) {
        val lower = joint.get(MotorParam.LowerLimit, axis)
        val upper = joint.get(MotorParam.UpperLimit, axis)
        when (limit) {
            is CompiledAxisLimit.Free -> assertTrue(lower > upper, "$label must be free, was $lower..$upper")
            is CompiledAxisLimit.Locked -> {
                assertEquals(0f, lower, 0f, "$label lower")
                assertEquals(0f, upper, 0f, "$label upper")
            }
            is CompiledAxisLimit.Limited -> {
                assertEquals(limit.lower.toFloat(), lower, 1e-5f, "$label lower")
                assertEquals(limit.upper.toFloat(), upper, 1e-5f, "$label upper")
            }
        }
    }

    /** Force along the run's tangential direction, world space, newtons. */
    private fun TrackedMechanismFixture.Mechanism.tangentForce(run: CompiledRun, magnitude: Float): Vector3f {
        val normal = run.second.lockedNormalLocal!!.normalize()
        // The run only rotates about X, so any direction perpendicular to the
        // declared normal in the run's local frame is tangential. X is the axle
        // and is orthogonal to the normal by construction.
        val tangent = Vec3(1.0, 0.0, 0.0).cross(normal).normalize()
        val world = runBody(run.index).getPhysicsRotation(null).toRotationMatrix()
            .mult(Vector3f(tangent.x.toFloat(), tangent.y.toFloat(), tangent.z.toFloat()), Vector3f())
            .normalize()
        return world.mult(magnitude)
    }

    /** Force along the run's declared normal, world space, newtons. */
    private fun TrackedMechanismFixture.Mechanism.normalForce(run: CompiledRun, magnitude: Float): Vector3f {
        val normal = run.second.lockedNormalLocal!!.normalize()
        val world = runBody(run.index).getPhysicsRotation(null).toRotationMatrix()
            .mult(Vector3f(normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat()), Vector3f())
            .normalize()
        return world.mult(magnitude)
    }

    private fun Vec3.cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )

    private fun Vec3.normalize(): Vec3 {
        val length = kotlin.math.sqrt(x * x + y * y + z * z)
        return if (length < 1e-9) Vec3.ZERO else Vec3(x / length, y / length, z / length)
    }
}
