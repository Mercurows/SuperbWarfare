package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.config.server.SyncConfig
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.entity.projectile.MissileProjectile
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.message.receive.BeyondVisualEntitySyncMessage
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.ForgeRegistries
import java.util.concurrent.ConcurrentHashMap

/**
 * Server-side entity registry and Beyond Visual Range (BVR) sync handler.
 *
 * Vehicles, missiles, and radar sources register themselves every tick via [register].
 * Periodically cleans up expired entries and broadcasts lightweight BVR render snapshots
 * to all tracking players in the dimension without allocations during idle state.
 *
 * @author superbwarfare contributors
 * @since 0.8.9.1
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
object ServerSyncedEntityHandler {

    /**
     * Cache container holding spatial and network properties for a registered BVR entity.
     */
    data class Entry(
        val entityRef: Entity,
        val entityId: Int,
        val pos: Vec3,
        val eyePos: Vec3,
        val yRot: Float,
        val xRot: Float,
        val entityType: ResourceLocation,
        val nbt: CompoundTag,
        /** Registration timestamp (system time ms) for expiration checks. */
        val timeStamp: Long,
        val targetPos: Vec3?,
        /** Stealth distance tracking multiplier (1.0 for non-vehicles). */
        val trackDistanceMultiply: Double,
        /** Distance above ground surface. */
        val heightAboveGround: Double,
    )

    // Dimension String -> EntityId -> Entry
    private val entities = ConcurrentHashMap<String, ConcurrentHashMap<Int, Entry>>()

    /** Queue of entity removals waiting to be broadcasted to clients on the next tick. */
    private val pendingRemovals = ConcurrentHashMap<String, MutableSet<Pair<Int, ResourceLocation>>>()

    /**
     * Registers or updates an entity for BVR long-range synchronization.
     *
     * @param entity the target entity to register or update.
     * @param targetPos optional target position (used by guided missiles).
     */
    @JvmStatic
    @JvmOverloads
    fun register(entity: Entity, targetPos: Vec3? = null) {
        if (!SyncConfig.SYNC_ENTITY_OVER_RANGE.get()) return
        val level = entity.level()
        if (level.isClientSide) return
        level.server ?: return

        if (entity !is VehicleEntity && entity !is MissileProjectile && entity !is Player
            && entity !is LivingEntity && !VehicleConfig.inScanList(entity.type)
        ) return

        val dim = level.dimension().location().toString()
        val now = System.currentTimeMillis()

        // Uses lightweight fast-path if IBvrSyncableEntity is implemented
        val nbt = entity.getBvrSyncNbt()

        val td = if (entity is VehicleEntity) entity.computed().trackDistanceMultiply else 1.0
        val hag = computeHeightAboveGround(entity)

        val entry = Entry(
            entityRef = entity,
            entityId = entity.id,
            pos = entity.position(),
            eyePos = entity.eyePosition,
            yRot = entity.yRot,
            xRot = entity.xRot,
            entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.type) ?: return,
            nbt = nbt,
            timeStamp = now,
            targetPos = targetPos,
            trackDistanceMultiply = td,
            heightAboveGround = hag,
        )

        entities.getOrPut(dim) { ConcurrentHashMap() }[entity.id] = entry
    }

    /**
     * Unregisters an entity and queues a removal notification packet for connected clients.
     *
     * @param entity the entity to remove.
     */
    @JvmStatic
    fun unregister(entity: Entity) {
        if (entity.level().isClientSide) return
        val dim = entity.level().dimension().location().toString()
        entities[dim]?.remove(entity.id)

        val entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.type) ?: return
        pendingRemovals.getOrPut(dim) { ConcurrentHashMap.newKeySet() }.add(Pair(entity.id, entityType))
    }

    /**
     * Retrieves all active BVR sync entries for a dimension.
     *
     * @param dim dimension resource location.
     * @return collection of active entries.
     */
    @JvmStatic
    fun getEntries(dim: ResourceLocation): Collection<Entry> {
        return entities[dim.toString()]?.values ?: emptyList()
    }

    /**
     * Calculates entity height above ground using surface heightmap.
     * Safe against queries in unloaded chunks.
     */
    private fun computeHeightAboveGround(entity: Entity): Double {
        val level = entity.level()
        val blockX = entity.blockX
        val blockZ = entity.blockZ

        if (!level.hasChunk(blockX shr 4, blockZ shr 4)) return 0.0

        val surfaceY = level.getHeight(
            Heightmap.Types.WORLD_SURFACE,
            blockX,
            blockZ
        )
        return (entity.y - surfaceY).coerceAtLeast(0.0)
    }

    /**
     * Checks if an entity is currently below ground level.
     *
     * @param entity entity to test.
     * @return `true` if entity bounding box top is below world surface.
     */
    @JvmStatic
    fun isUnderground(entity: Entity): Boolean {
        val level = entity.level()
        val blockX = entity.blockX
        val blockZ = entity.blockZ

        if (!level.hasChunk(blockX shr 4, blockZ shr 4)) return false

        val surfaceY = level.getHeight(
            Heightmap.Types.WORLD_SURFACE,
            blockX,
            blockZ
        )
        return entity.y + entity.bbHeight < surfaceY
    }

    /**
     * Purges expired entity entries from dim maps.
     *
     * @param server active MinecraftServer instance.
     */
    @JvmStatic
    fun cleanAll(server: MinecraftServer) {
        val now = System.currentTimeMillis()
        for (dimLevel in server.allLevels) {
            val dimKey = dimLevel.dimension().location().toString()
            val dimEntries = entities[dimKey] ?: continue
            val expireTime = SyncConfig.SERVER_SYNC_EXPIRE_TIME.get()

            val toRemove = dimEntries.values.filter { entry ->
                (!entry.entityRef.isAlive || entry.entityRef.isRemoved) && (now - entry.timeStamp > expireTime)
            }

            if (toRemove.isNotEmpty()) {
                dimEntries.values.removeAll(toRemove.toSet())
                val pending = pendingRemovals.getOrPut(dimKey) { ConcurrentHashMap.newKeySet() }
                for (entry in toRemove) {
                    pending.add(Pair(entry.entityId, entry.entityType))
                }
            }
        }
    }

    @SubscribeEvent
    fun tick(event: TickEvent.ServerTickEvent) {
        if (event.phase == TickEvent.Phase.START) return
        val server = event.server
        if (server.tickCount % SyncConfig.SERVER_SYNC_CLEAN_INTERVAL.get() == 0) {
            cleanAll(server)
        }
        broadcastWorldRender(server)
    }

    /**
     * Broadcasts BVR entity positions unconditionally to all players in the same dimension.
     *
     * Highly optimized to avoid list filtering, set allocations, and concatenation when
     * players are not mounted in vehicles or when removal lists are empty.
     */
    private fun broadcastWorldRender(server: MinecraftServer) {
        for (dimLevel in server.allLevels) {
            val dim = dimLevel.dimension().location()
            val dimStr = dim.toString()
            val dimEntries = entities[dimStr] ?: continue

            // Collect queued removals
            val removedList = mutableListOf<BeyondVisualEntitySyncMessage.SyncedEntity>()
            val pending = pendingRemovals.remove(dimStr)
            if (pending != null) {
                for ((id, type) in pending) {
                    removedList.add(
                        BeyondVisualEntitySyncMessage.SyncedEntity(
                            id = id,
                            type = type,
                            pos = Vec3.ZERO,
                            targetPos = null,
                            tag = CompoundTag(),
                            removed = true,
                        )
                    )
                }
            }

            if (dimEntries.isEmpty() && removedList.isEmpty()) continue

            val syncedList = mutableListOf<BeyondVisualEntitySyncMessage.SyncedEntity>()
            val deadIds = mutableListOf<Int>()

            for (entry in dimEntries.values) {
                val entity = entry.entityRef
                if (!entity.isAlive || entity.isRemoved) {
                    deadIds.add(entry.entityId)
                    removedList.add(
                        BeyondVisualEntitySyncMessage.SyncedEntity(
                            id = entry.entityId,
                            type = entry.entityType,
                            pos = Vec3.ZERO,
                            targetPos = null,
                            tag = CompoundTag(),
                            removed = true,
                        )
                    )
                    continue
                }

                if (entity !is VehicleEntity && entity !is MissileProjectile && entity !is LivingEntity) continue

                syncedList.add(
                    BeyondVisualEntitySyncMessage.SyncedEntity(
                        entry.entityId, entry.entityType, entry.pos, entry.targetPos, entry.nbt,
                        entry.yRot, entry.xRot,
                        heightAboveGround = entry.heightAboveGround,
                    )
                )
            }

            // Clean up removed dead IDs from active tracking map
            for (id in deadIds) {
                dimEntries.remove(id)
            }

            if (syncedList.isEmpty() && removedList.isEmpty()) continue

            val players = dimLevel.players()
            if (players.isEmpty()) continue

            for (player in players) {
                // OPTIMIZATION FAST PATH:
                // 95% of players walk on foot (player.vehicle == null).
                // Avoid allocating Set<Int>, running filter lists, or concatenating arrays for non-mounted players.
                val vehicle = player.vehicle
                val payload: List<BeyondVisualEntitySyncMessage.SyncedEntity> = if (vehicle == null) {
                    if (removedList.isEmpty()) {
                        syncedList
                    } else {
                        val combined = ArrayList<BeyondVisualEntitySyncMessage.SyncedEntity>(syncedList.size + removedList.size)
                        combined.addAll(syncedList)
                        combined.addAll(removedList)
                        combined
                    }
                } else {
                    // Slow path: Player is mounted in a vehicle hierarchy. Filter out ridden vehicles.
                    val ridingIds = mutableSetOf<Int>()
                    var currentRiding: Entity? = vehicle
                    while (currentRiding != null) {
                        ridingIds.add(currentRiding.id)
                        currentRiding = currentRiding.vehicle
                    }

                    val filtered = syncedList.filter { it.id !in ridingIds }
                    if (removedList.isEmpty()) {
                        filtered
                    } else {
                        val combined = ArrayList<BeyondVisualEntitySyncMessage.SyncedEntity>(filtered.size + removedList.size)
                        combined.addAll(filtered)
                        combined.addAll(removedList)
                        combined
                    }
                }

                if (payload.isNotEmpty()) {
                    sendPacketTo(player, BeyondVisualEntitySyncMessage(dim, payload))
                }
            }
        }
    }
}