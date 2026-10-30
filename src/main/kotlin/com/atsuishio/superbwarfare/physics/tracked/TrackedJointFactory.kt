package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.body.PhysicsJoints
import com.jme3.bullet.RotationOrder
import com.jme3.bullet.joints.New6Dof
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Matrix3f
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f

/**
 * Turns a [CompiledJointPlan] into a real Bullet constraint.
 *
 * This is the **only** place a compiled joint becomes a `New6Dof`. B1, B2 and S2
 * all call it, so a mount or a run end cannot mean one thing in a fixture and a
 * different thing in the vehicle runtime. The plan already carries both local
 * frames and six explicit axis states; this factory only has to translate them,
 * never to decide them.
 *
 * It must run on the physics thread: creating a joint touches native objects.
 * The caller owns the returned joint and is responsible for removing it before
 * either body is destroyed.
 */
object TrackedJointFactory {

    /**
     * Build the constraint described by [plan].
     *
     * [bodyA] and [bodyB] must be the native bodies the plan's logical references
     * resolve to. The mapping from [CompiledBodyRef] to a native body belongs to
     * the caller because only it knows the runtime that created them; passing the
     * bodies in keeps this factory free of any body registry.
     */
    fun create(
        plan: CompiledJointPlan,
        bodyA: PhysicsRigidBody,
        bodyB: PhysicsRigidBody,
        /**
         * Invoked with the native joint as soon as it exists, before its axis
         * limits are configured.
         *
         * `New6Dof` construction and limit setup are separate native calls. If
         * a limit call throws, the caller would otherwise never see the joint it
         * must destroy. Production callers record the joint in a rollback ledger
         * here; fixtures that cannot fail mid-setup may leave it empty.
         */
        onCreated: (New6Dof) -> Unit = {},
    ): New6Dof {
        require(plan.frameInA.origin.isFiniteVector() && plan.frameInB.origin.isFiniteVector()) {
            "joint ${plan.name}: frame origins must be finite"
        }
        val joint = New6Dof(
            bodyA,
            bodyB,
            plan.frameInA.origin.toBullet(),
            plan.frameInB.origin.toBullet(),
            plan.frameInA.rotation.toMatrix(),
            plan.frameInB.rotation.toMatrix(),
            RotationOrder.XYZ,
        )
        // Hand ownership over before any configuration that can throw.
        onCreated(joint)
        applyLimits(joint, plan)
        return joint
    }

    /**
     * Apply the six axis states to an already created joint.
     *
     * Kept separate so a caller that must create the joint first (for rollback
     * bookkeeping) can still route every limit decision through this factory.
     */
    fun applyLimits(joint: New6Dof, plan: CompiledJointPlan) {
        for (axis in 0..2) apply(joint, axis, plan.linear[axis])
        for (axis in 0..2) apply(joint, PhysicsJoints.ANG_X + axis, plan.angular[axis])

        // The plan describes limits, not actuation. A joint that inherited a
        // motor or a spring from an earlier configuration would silently move a
        // passive mechanism, so every row is explicitly de-energised.
        for (axis in 0..2) {
            joint.enableSpring(axis, false)
            joint.enableSpring(PhysicsJoints.ANG_X + axis, false)
        }
    }

    private fun apply(joint: New6Dof, axis: Int, limit: CompiledAxisLimit) {
        when (limit) {
            is CompiledAxisLimit.Free -> PhysicsJoints.free(joint, axis)
            is CompiledAxisLimit.Locked -> PhysicsJoints.lock(joint, axis)
            is CompiledAxisLimit.Limited -> PhysicsJoints.limit(
                joint,
                axis,
                limit.lower.toFloat(),
                limit.upper.toFloat(),
            )
        }
    }

    private fun CompiledRotation.toMatrix(): Matrix3f {
        val quaternion = Quaternion(x, y, z, w).normalizeLocal()
        return Matrix3f().set(quaternion)
    }

    private fun net.minecraft.world.phys.Vec3.toBullet(): Vector3f =
        Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
}
