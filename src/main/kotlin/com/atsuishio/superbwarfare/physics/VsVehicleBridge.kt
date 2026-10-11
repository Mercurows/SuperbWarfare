@file:Suppress("DEPRECATION")
@file:OptIn(org.valkyrienskies.core.api.VsBeta::class)

package com.atsuishio.superbwarfare.physics

import cn.solarmoon.spark_core.physics.SparkPhysics
import cn.solarmoon.spark_core.physics.body.*
import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.compat.CompatHolder
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.collision.shapes.CompoundCollisionShape
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Vector3f
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.fml.ModList
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.primitives.AABBd
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.mod.api.getShipsIntersecting
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.*

internal data class ShipPose(val position: Vec3, val rotation: Quaternionf, val velocity: Vec3, val omega: Vec3)
internal fun org.joml.Vector3dc.mc() = Vec3(x(), y(), z())

/** VS calls this attachment on its own worker. No Minecraft or Bullet objects cross this boundary. */
class VehicleShipReaction : ShipPhysicsListener {
    private val impulses = ConcurrentLinkedQueue<Pair<Vec3, Vec3>>()
    @Volatile private var pose: ShipPose? = null
    internal fun currentPose() = pose
    internal fun offer(impulse: Vec3, point: Vec3) { if (impulse.finite()) impulses.add(impulse to point) }
    override fun physTick(physShip: PhysShip, physLevel: PhysLevel) = physTick(physShip, physLevel, 1.0/60.0)
    override fun physTick(physShip: PhysShip, physLevel: PhysLevel, delta: Double) {
        if (delta <= 0 || !delta.isFinite()) return
        val k = physShip.kinematics
        pose = ShipPose(k.position.mc(), Quaternionf(k.rotation), k.velocity.mc(), k.angularVelocity.mc())
        while (true) {
            val (impulse, point) = impulses.poll() ?: break
            if (!physShip.isStatic) physShip.applyWorldForce(Vector3d(impulse.x/delta, impulse.y/delta, impulse.z/delta), Vector3d(point.x,point.y,point.z))
        }
    }
}

/** Loaded only when VS is present. */
object VsVehicleBridge {
    private data class Key(val level: Level, val ship: Long)
    private class Proxy(val key: Key) {
        var body: PhysicsRigidBody? = null
        var geometryHash = 0
        @Volatile var pose: ShipPose? = null
        var appliedPose: ShipPose? = null
        var listener: VehicleShipReaction? = null
    }
    private data class Geometry(val boxes: List<AABB>, val center: Vec3, val scaling: Vec3, val mass: Float, val inertia: Vec3, val fixed: Boolean)
    private val proxies = ConcurrentHashMap<Key, Proxy>()

    fun tick(level: Level) {
        // Capture the side before queuing work on Spark's physics thread. The
        // Level object itself must never be read by that worker.
        val clientSide = level.isClientSide
        val vehicles = VehiclePhysics.bodies.keys.filterIsInstance<VehicleEntity>().filter {
            it.level() === level && it.computed().engineType != EngineType.FIXED
        }
        val regions = linkedMapOf<Ship, AABB>()
        vehicles.forEach { vehicle ->
            val box = vehicle.getCombinedAABB().expandTowards(vehicle.deltaMovement.scale(10.0)).inflate(4.0)
            for (ship in level.getShipsIntersecting(AABBd(box.minX,box.minY,box.minZ,box.maxX,box.maxY,box.maxZ))) {
                val corners = listOf(box.minX,box.maxX).flatMap { x -> listOf(box.minY,box.maxY).flatMap { y -> listOf(box.minZ,box.maxZ).map { z ->
                    ship.worldToShip.transformPosition(Vector3d(x,y,z))
                } } }
                val local = AABB(corners.minOf { it.x },corners.minOf { it.y },corners.minOf { it.z },
                    corners.maxOf { it.x },corners.maxOf { it.y },corners.maxOf { it.z }).inflate(2.0)
                regions[ship] = regions[ship]?.minmax(local) ?: local
            }
        }
        val alive = regions.keys.map { Key(level,it.id) }.toSet()
        proxies.keys.filter { it.level === level && it !in alive }.forEach { key ->
            val old = proxies.remove(key) ?: return@forEach
            SparkPhysics.get(level).submitImmediateTask { old.body?.let { SparkPhysics.get(level).removeBody(it) } }
        }
        regions.forEach { (ship, region) ->
            val key = Key(level,ship.id)
            val proxy = proxies.computeIfAbsent(key) { Proxy(key) }
            if (ship is LoadedServerShip) {
                proxy.listener = ship.getOrPutAttachment(VehicleShipReaction::class.java) { VehicleShipReaction() }
            }
            val t = ship.transform
            proxy.pose = proxy.listener?.currentPose() ?: ShipPose(t.positionInWorld.mc(), Quaternionf(t.shipToWorldRotation), ship.velocity.mc(), ship.angularVelocity.mc())
            val limits = ship.shipAABB ?: return@forEach
            val boxes = ArrayList<AABB>()
            // Only geometry close to vehicles is copied, never the entire shipyard.
            val pos = BlockPos.MutableBlockPos()
            for (x in max(floor(region.minX).toInt(),limits.minX())..min(ceil(region.maxX).toInt(),limits.maxX()))
                for (y in max(floor(region.minY).toInt(),limits.minY())..min(ceil(region.maxY).toInt(),limits.maxY()))
                    for (z in max(floor(region.minZ).toInt(),limits.minZ())..min(ceil(region.maxZ).toInt(),limits.maxZ())) {
                        // [x,y,z] are ship-local coordinates. Convert the block
                        // sample to world space only for Minecraft shape lookup,
                        // then transform each returned shape back to ship-local
                        // coordinates before building the Spark proxy.
                        val world = ship.shipToWorld.transformPosition(Vector3d(x + .5, y + .5, z + .5))
                        pos.set(floor(world.x).toInt(), floor(world.y).toInt(), floor(world.z).toInt())
                        if (!level.hasChunkAt(pos)) continue
                        val state = level.getBlockState(pos)
                        if (state.isAir) continue
                        state.getCollisionShape(level, pos).toAabbs().forEach { shape ->
                            val localCorners = listOf(
                                Triple(shape.minX, shape.minY, shape.minZ),
                                Triple(shape.minX, shape.minY, shape.maxZ),
                                Triple(shape.minX, shape.maxY, shape.minZ),
                                Triple(shape.minX, shape.maxY, shape.maxZ),
                                Triple(shape.maxX, shape.minY, shape.minZ),
                                Triple(shape.maxX, shape.minY, shape.maxZ),
                                Triple(shape.maxX, shape.maxY, shape.minZ),
                                Triple(shape.maxX, shape.maxY, shape.maxZ),
                            ).map { (xx, yy, zz) ->
                                ship.worldToShip.transformPosition(Vector3d(pos.x + xx, pos.y + yy, pos.z + zz))
                            }
                            boxes.add(AABB(
                                localCorners.minOf { it.x }, localCorners.minOf { it.y }, localCorners.minOf { it.z },
                                localCorners.maxOf { it.x }, localCorners.maxOf { it.y }, localCorners.maxOf { it.z }
                            ))
                        }
                    }
            val inertia = (ship as? ServerShip)?.inertiaData
            val geometry = Geometry(boxes, t.positionInShip.mc(), t.shipToWorldScaling.mc(),
                inertia?.mass?.toFloat()?.coerceAtLeast(.1f) ?: 1f,
                inertia?.inertiaTensor?.let { Vec3(it.m00(),it.m11(),it.m22()) } ?: Vec3(1.0,1.0,1.0),
                (ship as? ServerShip)?.isStatic ?: true)
            val hash = geometry.hashCode()
            val physics = SparkPhysics.get(level)
            physics.submitDeduplicatedTask("sbw_ship_${ship.id}") {
                if (proxies[key] !== proxy) return@submitDeduplicatedTask
                if (proxy.body == null || proxy.geometryHash != hash) {
                    proxy.body?.let(physics::removeBody)
                    val shape = CompoundCollisionShape()
                    boxes.forEach { b ->
                        val half = Vec3(b.xsize*.5,b.ysize*.5,b.zsize*.5).multiply(geometry.scaling)
                        shape.addChildShape(BoxCollisionShape(half.bullet()), b.center.subtract(geometry.center).multiply(geometry.scaling).bullet())
                    }
                    if (boxes.isEmpty()) { proxy.body = null; return@submitDeduplicatedTask }
                    val body = PhysicsRigidBody(shape, if (geometry.fixed) 0f else geometry.mass).apply {
                        name = "sbw_vs_${key.ship}"
                        collisionGroup = CollisionGroups.PHYSICS_BODY
                        // The proxy is kinematic and never solves VS terrain or
                        // ship contacts. It only participates in chassis contact
                        // queries so the resolved impulse can be sent back to VS.
                        setCollideWithGroups(CollisionGroups.CHASSIS)
                        isKinematic = geometry.fixed || clientSide
                        setProtectGravity(true)
                        setGravity(Vector3f())
                        setDamping(0f,0f)
                        friction = .8f
                        if (!geometry.fixed) setInverseInertiaLocal(Vector3f((1/geometry.inertia.x.coerceAtLeast(.01)).toFloat(),
                            (1/geometry.inertia.y.coerceAtLeast(.01)).toFloat(),(1/geometry.inertia.z.coerceAtLeast(.01)).toFloat()))
                    }
                    body.onPrePhysicsTick {
                        val value = proxy.pose ?: return@onPrePhysicsTick
                        if (value !== proxy.appliedPose) {
                            body.setPhysicsLocation(value.position.bullet()); body.setPhysicsRotation(value.rotation.bullet())
                            body.setLinearVelocity(value.velocity.bullet()); body.setAngularVelocity(value.omega.bullet())
                            body.activate(true); proxy.appliedPose = value
                        }
                    }
                    body.onResolvedContact { contact ->
                        if (!clientSide && contact.snapshot.normalImpulse > 0f &&
                            contact.other.collisionGroup == CollisionGroups.CHASSIS)
                            proxy.listener?.offer(contact.snapshot.impulse, contact.snapshot.point)
                    }
                    proxy.appliedPose = null
                    proxy.body = body; proxy.geometryHash = hash
                    physics.addBody(body)
                }
            }
        }
    }

    fun unload(level: Level) {
        proxies.keys.filter { it.level === level }.forEach { key ->
            val proxy = proxies.remove(key) ?: return@forEach
            SparkPhysics.get(level).submitImmediateTask { proxy.body?.let { SparkPhysics.get(level).removeBody(it) } }
        }
    }
}

/** This class has no VS types in its signatures and is safe without the optional mod. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID)
object VsVehicleEvents {
    @SubscribeEvent fun tick(event: TickEvent.LevelTickEvent) {
        if (event.phase == TickEvent.Phase.END && ModList.get().isLoaded(CompatHolder.VALKYRIEN_SKIES)) VsVehicleBridge.tick(event.level)
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) fun unload(event: LevelEvent.Unload) {
        if (ModList.get().isLoaded(CompatHolder.VALKYRIEN_SKIES)) (event.level as? Level)?.let(VsVehicleBridge::unload)
    }
}
