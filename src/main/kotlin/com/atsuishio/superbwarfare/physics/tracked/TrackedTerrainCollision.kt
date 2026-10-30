package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.event.NeedsCollisionEvent
import cn.solarmoon.spark_core.physics.PhysicsHost
import cn.solarmoon.spark_core.physics.body.CollisionGroups
import cn.solarmoon.spark_core.physics.body.owner
import com.jme3.bullet.collision.PhysicsCollisionObject
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.SubscribeEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The one terrain group a tracked vehicle may collide with.
 *
 * It is deliberately distinct from [CollisionGroups.TERRAIN]. The raw block
 * AABB bodies remain available to other Spark objects, but a tracked vehicle's
 * masks never include TERRAIN.
 */
object TrackedTerrainCollision {
    const val TERRAIN_MESH: Int = 1 shl 7

    private val owners = ConcurrentHashMap<Long, PhysicsHost>()
    private val registered = AtomicBoolean(false)

    fun ensureRegistered() {
        if (registered.compareAndSet(false, true)) {
            MinecraftForge.EVENT_BUS.register(this)
        }
    }

    fun register(body: PhysicsCollisionObject, host: PhysicsHost) {
        ensureRegistered()
        owners[body.nativeId()] = host
    }

    fun unregister(body: PhysicsCollisionObject) {
        owners.remove(body.nativeId())
    }

    /**
     * A per-vehicle terrain mesh is private to that vehicle.
     *
     * The owner is the vehicle host, so the same-owner allowlist lets its
     * chassis/wheels/links collide with it. Every other physics host is
     * rejected here, before contact generation: another tracked vehicle must
     * not ride this vehicle's local terrain copy.
     */
    @SubscribeEvent
    fun onNeedsCollision(event: NeedsCollisionEvent) {
        val ownerA = owners[event.bodyA.nativeId()]
        val ownerB = owners[event.bodyB.nativeId()]
        if (ownerA != null && event.bodyB.owner !== ownerA) {
            event.shouldCollide = false
            return
        }
        if (ownerB != null && event.bodyA.owner !== ownerB) {
            event.shouldCollide = false
        }
    }
}
