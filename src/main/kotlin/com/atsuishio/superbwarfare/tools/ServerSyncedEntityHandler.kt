package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.entity.projectile.MissileProjectile
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.message.receive.BeyondVisualEntitySyncMessage
import com.atsuishio.superbwarfare.network.message.receive.EntitySyncMessage
import com.atsuishio.superbwarfare.tools.ServerSyncedEntityHandler.cleanAll
import com.atsuishio.superbwarfare.tools.ServerSyncedEntityHandler.getEntries
import com.atsuishio.superbwarfare.tools.ServerSyncedEntityHandler.register
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.ForgeRegistries
import java.util.concurrent.ConcurrentHashMap

/**
 * 服务端同步实体处理器 —— ClientSyncedEntityHandler 的服务端镜像。
 *
 * 载具和导弹每 tick 主动调用 [register] 将自身加入此列表。
 * 雷达/IFF 等消费者调用 [getEntries] 从此列表查询，避免遍历 level.allEntities。
 * 定期调用 [cleanAll] 清理已消失实体的过期条目。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
object ServerSyncedEntityHandler {

    data class Entry(
        val entityId: Int,
        val pos: Vec3,
        val eyePos: Vec3,
        val yRot: Float,
        val xRot: Float,
        val entityType: ResourceLocation,
        val nbt: CompoundTag,
        /** 实体注册/更新时间戳（系统时间 ms），用于 NBT 序列化间隔判定和过期清理，不受服务器重启影响 */
        val timeStamp: Long,
        val targetPos: Vec3?,
        /** 隐身减益系数，非载具实体为 1.0 */
        val trackDistanceMultiply: Double,
        /** 实体离地高度 */
        val heightAboveGround: Double,
    )

    // dim string → entityId → Entry
    private val entities = ConcurrentHashMap<String, ConcurrentHashMap<Int, Entry>>()

    /**
     * 注册或更新实体。每 tick 由 VehicleEntity / MissileProjectile / IffItem 调用。
     * NBT 仅在与上次同步间隔 >= SYNC_ENTITY_INTERVAL 时重新序列化。
     */
    @JvmStatic
    @JvmOverloads
    fun register(entity: Entity, targetPos: Vec3? = null) {
        if (!MiscConfig.SYNC_ENTITY_OVER_RANGE.get()) return
        val level = entity.level()
        if (level.isClientSide) return
        level.server ?: return
        if (entity is VehicleEntity && entity.isWreck) return
        if (entity !is VehicleEntity && entity !is MissileProjectile && entity !is Player
            && !VehicleConfig.inScanList(entity.type)
        ) return

        val dim = level.dimension().location().toString()
        val now = System.currentTimeMillis()
        val existing = entities[dim]?.get(entity.id)

        val interval = MiscConfig.SYNC_ENTITY_INTERVAL.get()
        val intervalMs = interval * 50L // ticks → ms
        val nbt = if (existing == null || now - existing.timeStamp >= intervalMs) {
            entity.serializeNBT()
        } else {
            existing.nbt
        }

        val td = if (entity is VehicleEntity && !entity.isWreck)
            entity.computed().trackDistanceMultiply else 1.0
        val hag = computeHeightAboveGround(entity)

        val entry = Entry(
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

    @JvmStatic
    fun unregister(entity: Entity) {
        if (entity.level().isClientSide) return
        entities[entity.level().dimension().location().toString()]?.remove(entity.id)
    }

    @JvmStatic
    fun getEntries(dim: ResourceLocation): Collection<Entry> {
        return entities[dim.toString()]?.values ?: emptyList()
    }

    /** 计算实体离地高度（使用高度图，高效） */
    private fun computeHeightAboveGround(entity: Entity): Double {
        val level = entity.level()
        val surfaceY = level.getHeight(
            Heightmap.Types.WORLD_SURFACE,
            entity.blockX,
            entity.blockZ
        )
        return (entity.y - surfaceY).coerceAtLeast(0.0)
    }

    /** 判定实体是否在地下（实体顶部低于地表） */
    @JvmStatic
    fun isUnderground(entity: Entity): Boolean {
        val level = entity.level()
        val surfaceY = level.getHeight(
            Heightmap.Types.WORLD_SURFACE,
            entity.blockX,
            entity.blockZ
        )
        return entity.y + entity.bbHeight < surfaceY
    }

    /**
     * 清理已消失实体的过期条目
     */
    @JvmStatic
    fun cleanAll(server: MinecraftServer) {
        val now = System.currentTimeMillis()
        for (dimLevel in server.allLevels) {
            val dimKey = dimLevel.dimension().location().toString()
            val dimEntries = entities[dimKey] ?: continue
            dimEntries.values.removeIf { entry ->
                dimLevel.getEntity(entry.entityId) == null && now - entry.timeStamp > MiscConfig.SERVER_SYNC_EXPIRE_TIME.get()
            }
        }
    }

    @SubscribeEvent
    fun tick(event: TickEvent.ServerTickEvent) {
        if (event.phase == TickEvent.Phase.START) return
        val server = event.server
        if (server.tickCount % MiscConfig.SERVER_SYNC_CLEAN_INTERVAL.get() == 0) {
            cleanAll(server)
        }
        broadcastWorldRender(server)
    }

    /**
     * 将 [ServerSyncedEntityHandler] 中所有实体无条件发送给同维度的每个玩家。
     * 超视距渲染不依赖雷达/IFF，所有载具和导弹都应能被看见。
     */
    private fun broadcastWorldRender(server: MinecraftServer) {
        for (dimLevel in server.allLevels) {
            val dim = dimLevel.dimension().location()
            val dimEntries = entities[dim.toString()] ?: continue
            if (dimEntries.isEmpty()) continue

            val syncedList = dimEntries.values.mapNotNull { entry ->
                dimLevel.getEntity(entry.entityId) ?: return@mapNotNull null
                EntitySyncMessage.SyncedEntity(
                    entry.entityId, entry.entityType, entry.pos, entry.targetPos, entry.nbt,
                    entry.yRot, entry.xRot,
                    heightAboveGround = entry.heightAboveGround,
                )
            }

            if (syncedList.isNotEmpty()) {
                val msg = BeyondVisualEntitySyncMessage(dim, syncedList)
                for (player in dimLevel.players()) {
                    sendPacketTo(player, msg)
                }
            }
        }
    }
}
