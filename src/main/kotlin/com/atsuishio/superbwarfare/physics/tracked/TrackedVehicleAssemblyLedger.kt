package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.level.PhysicsLevel
import cn.solarmoon.spark_core.physics.track.TrackRunRuntime
import com.jme3.bullet.joints.New6Dof
import com.jme3.bullet.objects.PhysicsRigidBody
import java.util.concurrent.CompletableFuture

/**
 * Rollback ledger for one tracked assembly.
 *
 * The runtime creates native objects in several stages, and every stage can
 * fail: a cylinder shape, a mount joint, a run body, an external end joint. A
 * `CompletableFuture` chain only reports that a stage failed; it does not tell
 * the runtime which objects already exist. Without a ledger, a mid-assembly
 * failure leaves live bodies in the world that nothing owns.
 *
 * Every object is recorded **before** it is configured or registered, so a
 * failure during configuration still leaves it in the ledger. Removal is
 * joint-first, then run runtimes, then bodies, matching the documented teardown
 * order. An object that could not be removed stays in the ledger so a later
 * close can retry instead of reporting success over live native state.
 *
 * This class only holds references. All native calls go through the owning
 * [PhysicsLevel] and must run on its physics thread.
 */
internal class TrackedVehicleAssemblyLedger(private val level: PhysicsLevel) {

    private val lock = Any()
    private val joints = ArrayList<New6Dof>()
    private val bodies = ArrayList<PhysicsRigidBody>()
    private val runtimes = ArrayList<TrackRunRuntime>()

    /** True when at least one native object is still owned by the ledger. */
    val hasRemaining: Boolean
        get() = synchronized(lock) { joints.isNotEmpty() || bodies.isNotEmpty() || runtimes.isNotEmpty() }

    val jointCount: Int get() = synchronized(lock) { joints.size }
    val bodyCount: Int get() = synchronized(lock) { bodies.size }
    val runtimeCount: Int get() = synchronized(lock) { runtimes.size }

    /** Record a joint as soon as it exists, before any limit is configured. */
    fun recordJoint(joint: New6Dof) {
        synchronized(lock) { joints += joint }
    }

    /** Record a body as soon as it exists, before it is added to the world. */
    fun recordBody(body: PhysicsRigidBody) {
        synchronized(lock) { bodies += body }
    }

    /** Record a run runtime before its own `create()` is submitted. */
    fun recordRuntime(runtime: TrackRunRuntime) {
        synchronized(lock) { runtimes += runtime }
    }

    /**
     * Remove every recorded object, in dependency order.
     *
     * Runs asynchronously because [TrackRunRuntime.closeAsync] queues its own
     * physics task. The returned future completes only after the last body is
     * gone, or fails with the first removal error plus the rest suppressed.
     * Objects that could not be removed remain recorded.
     */
    fun rollback(): CompletableFuture<Void> {
        val result = CompletableFuture<Void>()
        val endStage = level.submitTask("tracked rollback joints") {
            level.assertPhysicsThread()
            removeAll(joints, "joint") { level.removeJoint(it) }
        }

        val runStage = endStage.thenCompose {
            val snapshot = synchronized(lock) { ArrayList(runtimes) }
            val closers = snapshot.map { runtime ->
                runtime.closeAsync().whenComplete { _, error ->
                    if (error == null) synchronized(lock) { runtimes.removeAll { it === runtime } }
                }
            }
            if (closers.isEmpty()) CompletableFuture.completedFuture(null)
            else CompletableFuture.allOf(*closers.toTypedArray())
        }

        val bodyStage = runStage.thenCompose {
            level.submitTask("tracked rollback bodies") {
                level.assertPhysicsThread()
                removeAll(bodies, "body") { level.removeBody(it) }
            }
        }

        bodyStage.whenComplete { _, error ->
            if (error == null) result.complete(null) else result.completeExceptionally(error)
        }
        return result
    }

    /**
     * Same dependency order as [rollback], but on the physics thread and without
     * submitting another task. Shutdown participants call this after the level
     * has stopped accepting work.
     */
    fun rollbackNow() {
        level.assertPhysicsThread()
        removeAll(joints, "joint") { level.removeJoint(it) }
        val snapshot = synchronized(lock) { ArrayList(runtimes) }
        val failures = ArrayList<Throwable>()
        for (runtime in snapshot) {
            try {
                runtime.closeNow()
                synchronized(lock) { runtimes.removeAll { it === runtime } }
            } catch (error: Throwable) {
                failures += error
            }
        }
        if (failures.isEmpty()) {
            removeAll(bodies, "body") { level.removeBody(it) }
            return
        }
        val primary = failures.first()
        failures.drop(1).forEach { primary.addSuppressed(it) }
        throw IllegalStateException("run rollback left ${failures.size} runtime(s)", primary)
    }

    /**
     * Remove every item, but only drop it from [items] after its own removal
     * succeeds. A failure keeps that item and every not-yet-attempted item.
     */
    private fun <T> removeAll(items: MutableList<T>, label: String, remove: (T) -> Unit) {
        val snapshot = synchronized(lock) { ArrayList(items) }
        val failures = ArrayList<Throwable>()
        for (item in snapshot.asReversed()) {
            try {
                remove(item)
                synchronized(lock) { items.removeAll { it === item } }
            } catch (error: Throwable) {
                failures += error
            }
        }
        if (failures.isNotEmpty()) {
            val primary = failures.first()
            failures.drop(1).forEach { primary.addSuppressed(it) }
            throw IllegalStateException("$label rollback left ${failures.size} object(s)", primary)
        }
    }
}
