package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.network.message.receive.EntitySyncMessage.SyncedEntity
import com.atsuishio.superbwarfare.network.message.receive.PlayerInfoSyncMessage.SyncedPlayerInfo
import com.atsuishio.superbwarfare.network.message.receive.RadarSyncMessage
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.minecraftforge.registries.ForgeRegistries
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ClientSyncedEntityHandler {

    data class SyncedKey(val dim: ResourceLocation, val id: Int)

    data class ClientSyncedEntity(
        val entity: Entity,
        val timeStamp: Long,
        val targetPos: Vec3?,
        val heightAboveGround: Double = -1.0,
        /** Previous tick position, used for speed computation. */
        var prevPos: Vec3? = null,
    )

    data class SyncedPlayerKey(val dim: ResourceLocation, val uuid: UUID)

    data class ClientSyncedPlayer(
        val timeStamp: Long, val uuid: UUID, val pos: Vec3, val name: String,
        val onVehicle: Boolean, val isDriver: Boolean
    )

    /** 友方：IffItem / MissileProjectile 同步 */
    @JvmField
    val SYNCED_FRIENDLY = ConcurrentHashMap<SyncedKey, ClientSyncedEntity>()

    /** 敌对：FuMO25 / vehicleRadar 同步 */
    @JvmField
    val SYNCED_HOSTILE = ConcurrentHashMap<SyncedKey, ClientSyncedEntity>()

    /** 中立：FuMO25 / vehicleRadar 同步（无人载具） */
    @JvmField
    val SYNCED_NEUTRAL = ConcurrentHashMap<SyncedKey, ClientSyncedEntity>()

    @JvmField
    val SYNCED_PLAYERS = ConcurrentHashMap<SyncedPlayerKey, ClientSyncedPlayer>()

    @JvmStatic
    @JvmOverloads
    fun sync(dim: ResourceLocation, list: List<SyncedEntity>, friendly: Boolean, neutral: Boolean = false) {
        val level = mc.level ?: return
        val time = System.currentTimeMillis()
        val targetMap = when {
            neutral -> SYNCED_NEUTRAL
            friendly -> SYNCED_FRIENDLY
            else -> SYNCED_HOSTILE
        }
        for (syncedEntity in list) {
            val key = SyncedKey(dim, syncedEntity.id)
            if (syncedEntity.removed) {
                targetMap.remove(key)
                continue
            }
            val existedEntry = targetMap[key]
            var entity: Entity
            val prevPos = existedEntry?.let { Vec3(it.entity.x, it.entity.y, it.entity.z) }
            if (existedEntry != null) {
                entity = existedEntry.entity
                val tag = syncedEntity.tag as? CompoundTag
                if (tag != null) entity.load(tag)
            } else {
                val type = ForgeRegistries.ENTITY_TYPES.getValue(syncedEntity.type) ?: continue
                entity = type.create(level) ?: continue
                val tag = syncedEntity.tag as? CompoundTag ?: continue
                entity.load(tag)
                entity.id = syncedEntity.id
            }
            entity.setPos(syncedEntity.pos)
            entity.deltaMovement = syncedEntity.targetPos ?: Vec3.ZERO
            entity.yRot = syncedEntity.yRot
            val entry = ClientSyncedEntity(entity, time, syncedEntity.targetPos, syncedEntity.heightAboveGround)
            entry.prevPos = prevPos
            targetMap[key] = entry
        }
    }

    @JvmStatic
    fun syncPlayerInfo(dim: ResourceLocation, list: List<SyncedPlayerInfo>) {
        if (mc.level == null) return
        val time = System.currentTimeMillis()
        for (info in list) {
            SYNCED_PLAYERS[SyncedPlayerKey(dim, info.uuid)] =
                ClientSyncedPlayer(time, info.uuid, info.pos, info.name, info.onVehicle, info.isDriver)
        }
    }

    @JvmStatic
    fun clean() {
        val tick = System.currentTimeMillis()
        val expire = MiscConfig.CLIENT_SYNC_EXPIRE_TIME.get()
        SYNCED_FRIENDLY.values.removeIf { tick - it.timeStamp > expire }
        SYNCED_HOSTILE.values.removeIf { tick - it.timeStamp > expire }
        SYNCED_NEUTRAL.values.removeIf { tick - it.timeStamp > expire }
        SYNCED_PLAYERS.values.removeIf { tick - it.timeStamp > expire }
        // 雷达过期清理：超过 2 个 sync 周期未更新则移除
        val radarExpire = expire * 2L
        SYNCED_RADARS.values.removeIf { tick - it.timeStamp > radarExpire }
    }

    @JvmStatic
    fun getSyncedFriendlyEntities(level: Level): List<Entity> =
        SYNCED_FRIENDLY.filterKeys { it.dim == level.dimension().location() }.map { it.value.entity }

    @JvmStatic
    fun getSyncedHostileEntities(level: Level): List<Entity> =
        SYNCED_HOSTILE.filterKeys { it.dim == level.dimension().location() }.map { it.value.entity }

    @JvmStatic
    fun getSyncedNeutralEntities(level: Level): List<Entity> =
        SYNCED_NEUTRAL.filterKeys { it.dim == level.dimension().location() }.map { it.value.entity }

    /** 返回所有已同步实体（友方+中立，用于 IFFOverlay 和 TacticalMapScreen 友好标记） */
    @JvmStatic
    fun getSyncedEntities(level: Level): List<Entity> {
        val dim = level.dimension().location()
        val all = mutableListOf<Entity>()
        SYNCED_FRIENDLY.filterKeys { it.dim == dim }.mapTo(all) { it.value.entity }
        SYNCED_NEUTRAL.filterKeys { it.dim == dim }.mapTo(all) { it.value.entity }
        return all
    }

    @JvmStatic
    fun getSyncedPlayerInfo(level: Level): List<ClientSyncedPlayer> =
        SYNCED_PLAYERS.filterKeys { it.dim == level.dimension().location() }.map { it.value }

    /** 按 ID 查找任一分类中的条目 */
    @JvmStatic
    fun getSyncedEntry(level: Level, entityId: Int): ClientSyncedEntity? {
        val dim = level.dimension().location()
        return SYNCED_FRIENDLY[SyncedKey(dim, entityId)]
            ?: SYNCED_HOSTILE[SyncedKey(dim, entityId)]
            ?: SYNCED_NEUTRAL[SyncedKey(dim, entityId)]
    }

    // ── 雷达配置同步 ──

    data class SyncedRadar(
        val pos: Vec3,
        val radius: Double,
        val sweepAngle: Double,
        val yRot: Double,
        val ownerName: String,
        val showIcon: Boolean,
        val sourceId: String,
        val timeStamp: Long,
    )

    @JvmField
    val SYNCED_RADARS = ConcurrentHashMap<SyncedKey, SyncedRadar>()

    @JvmStatic
    fun syncRadars(dim: ResourceLocation, radars: List<RadarSyncMessage.SyncedRadar>) {
        val time = System.currentTimeMillis()
        for (r in radars) {
            // 用 sourceId 作为 key，同一雷达每次更新覆盖旧位置，避免移动拖影
            val key = SyncedKey(dim, r.sourceId.hashCode())
            SYNCED_RADARS[key] = SyncedRadar(
                pos = r.pos, radius = r.radius, sweepAngle = r.sweepAngle,
                yRot = r.yRot, ownerName = r.ownerName, showIcon = r.showIcon,
                sourceId = r.sourceId, timeStamp = time,
            )
        }
        // 超过 2 倍 sync 间隔未更新的雷达视为已移除
        val expire = MiscConfig.CLIENT_SYNC_EXPIRE_TIME.get() * 2L
        SYNCED_RADARS.values.removeIf { time - it.timeStamp > expire }
    }

    @JvmStatic
    fun getSyncedRadars(level: Level): List<SyncedRadar> {
        return SYNCED_RADARS.filterKeys { it.dim == level.dimension().location() }.values.toList()
    }
}
