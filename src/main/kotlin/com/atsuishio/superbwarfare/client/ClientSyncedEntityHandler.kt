package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.client.sound.VehicleSoundInstance
import com.atsuishio.superbwarfare.config.server.SyncConfig
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.message.receive.BeyondVisualEntitySyncMessage.SyncedEntity
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
        /** Per-tick velocity computed from successive sync positions, used for extrapolation. */
        val velocity: Vec3 = Vec3.ZERO,
        /** Whether this entity should be rendered in the 3D world beyond view distance. */
        val shouldWorldRender: Boolean = false,
    )

    data class SyncedPlayerKey(val dim: ResourceLocation, val uuid: UUID)

    data class ClientSyncedPlayer(
        val timeStamp: Long, val uuid: UUID, val pos: Vec3, val name: String,
        val onVehicle: Boolean, val isDriver: Boolean,
        /** 关系标识："friendly" / "hostile" / "neutral" */
        val relation: String = "friendly",
        /** 服务端实体 ID，用于管理员清除等操作（-1 表示未知） */
        val entityId: Int = -1,
    )

    // ── 轻量级 ID 池：dim string → entityId → timestamp ──

    /** 友方实体 ID 池，由载具/导弹/IFF 每 tick 发送 */
    @JvmField
    val FRIENDLY_IDS = ConcurrentHashMap<String, ConcurrentHashMap<Int, Long>>()

    /** 敌对实体 ID 池，由雷达扫描发送 */
    @JvmField
    val HOSTILE_IDS = ConcurrentHashMap<String, ConcurrentHashMap<Int, Long>>()

    /** 中立实体 ID 池，由雷达扫描发送 */
    @JvmField
    val NEUTRAL_IDS = ConcurrentHashMap<String, ConcurrentHashMap<Int, Long>>()

    /** 超视距世界渲染：ServerSyncedEntityHandler 无条件同步，不依赖雷达/IFF。
     *  这是所有超视距实体状态数据的唯一来源。敌我关系由 ID 池判定。 */
    @JvmField
    val SYNCED_WORLD_RENDER = ConcurrentHashMap<SyncedKey, ClientSyncedEntity>()

    @JvmField
    val SYNCED_PLAYERS = ConcurrentHashMap<SyncedPlayerKey, ClientSyncedPlayer>()

    /**
     * 同步实体身份关系（轻量级，只传 ID 列表）。
     * 载具/导弹/玩家每 tick 将自身 ID 以 friendlyIds 发送给友方；
     * 雷达将探测到的敌对/中立实体 ID 发送给友方。
     */
    @JvmStatic
    fun syncEntityRelations(dim: ResourceLocation, friendly: List<Int>, hostile: List<Int>, neutral: List<Int>) {
        val dimStr = dim.toString()
        val now = System.currentTimeMillis()
        fun addTo(pool: ConcurrentHashMap<String, ConcurrentHashMap<Int, Long>>, ids: List<Int>) {
            if (ids.isEmpty()) return
            val map = pool.getOrPut(dimStr) { ConcurrentHashMap() }
            for (id in ids) {
                map[id] = now
            }
        }
        addTo(FRIENDLY_IDS, friendly)
        addTo(HOSTILE_IDS, hostile)
        addTo(NEUTRAL_IDS, neutral)
    }

    @JvmStatic
    fun syncPlayerInfo(dim: ResourceLocation, list: List<SyncedPlayerInfo>) {
        if (mc.level == null) return
        val time = System.currentTimeMillis()
        for (info in list) {
            SYNCED_PLAYERS[SyncedPlayerKey(dim, info.uuid)] =
                ClientSyncedPlayer(time, info.uuid, info.pos, info.name, info.onVehicle, info.isDriver, info.relation, info.entityId)
        }
    }

    @JvmStatic
    fun clean() {
        val tick = System.currentTimeMillis()
        val expire = SyncConfig.CLIENT_SYNC_EXPIRE_TIME.get()
        SYNCED_WORLD_RENDER.values.removeIf { tick - it.timeStamp > expire }
        SYNCED_PLAYERS.values.removeIf { tick - it.timeStamp > expire }
        // 雷达过期清理：超过 2 个 sync 周期未更新则移除
        val radarExpire = expire * 2L
        SYNCED_RADARS.values.removeIf { tick - it.timeStamp > radarExpire }
        // 清理过期 ID
        for (pool in listOf(FRIENDLY_IDS, HOSTILE_IDS, NEUTRAL_IDS)) {
            for ((_, idMap) in pool) {
                idMap.values.removeIf { tick - it > expire }
            }
            pool.values.removeIf { it.isEmpty() }
        }
        // 清理假实体引擎音效：移除已不在 SYNCED_WORLD_RENDER 中的实体的音效
        val activeIds = SYNCED_WORLD_RENDER.keys.map { it.id }.toSet()
        phantomEngineSounds.entries.removeIf { (id, sound) ->
            if (id !in activeIds) {
                mc.soundManager.stop(sound)
                true
            } else false
        }
        phantomStukaSounds.entries.removeIf { (id, sound) ->
            if (id !in activeIds) {
                mc.soundManager.stop(sound)
                true
            } else false
        }
    }

    /**
     * 从 SYNCED_WORLD_RENDER 获取当前维度的友方实体。
     * 实体状态数据来源于超视距世界渲染池，敌我关系由 FRIENDLY_IDS 判定。
     */
    @JvmStatic
    fun getSyncedFriendlyEntities(level: Level): List<Entity> {
        val dim = level.dimension().location()
        val ids = FRIENDLY_IDS[dim.toString()] ?: return emptyList()
        return SYNCED_WORLD_RENDER
            .filterKeys { it.dim == dim && ids.containsKey(it.id) }
            .map { it.value.entity }
    }

    /**
     * 从 SYNCED_WORLD_RENDER 获取当前维度的敌对实体。
     */
    @JvmStatic
    fun getSyncedHostileEntities(level: Level): List<Entity> {
        val dim = level.dimension().location()
        val ids = HOSTILE_IDS[dim.toString()] ?: return emptyList()
        return SYNCED_WORLD_RENDER
            .filterKeys { it.dim == dim && ids.containsKey(it.id) }
            .map { it.value.entity }
    }

    /**
     * 从 SYNCED_WORLD_RENDER 获取当前维度的中立实体。
     */
    @JvmStatic
    fun getSyncedNeutralEntities(level: Level): List<Entity> {
        val dim = level.dimension().location()
        val ids = NEUTRAL_IDS[dim.toString()] ?: return emptyList()
        return SYNCED_WORLD_RENDER
            .filterKeys { it.dim == dim && ids.containsKey(it.id) }
            .map { it.value.entity }
    }

    /**
     * 无条件同步实体到超视距世界渲染池。
     */
    @JvmStatic
    fun syncWorldRender(dim: ResourceLocation, list: List<SyncedEntity>) {
        val level = mc.level ?: return
        val time = System.currentTimeMillis()
        for (syncedEntity in list) {
            val key = SyncedKey(dim, syncedEntity.id)
            if (syncedEntity.removed) {
                SYNCED_WORLD_RENDER.remove(key)
                phantomEngineSounds.remove(syncedEntity.id)?.let { mc.soundManager.stop(it) }
                phantomStukaSounds.remove(syncedEntity.id)?.let { mc.soundManager.stop(it) }
                continue
            }

            // 若真实实体已被服务端通过原版路径同步到客户端，
            // 假实体本身保留在 SYNCED_WORLD_RENDER 中（供 IFF / 战术地图使用），仅停止假实体引擎音效
            val realEntityExists = level.getEntity(syncedEntity.id) != null

            val existedEntry = SYNCED_WORLD_RENDER[key]
            val vel = if (existedEntry != null) {
                val dt = ((time - existedEntry.timeStamp) / 50.0).coerceAtLeast(0.5)
                Vec3(
                    (syncedEntity.pos.x - existedEntry.entity.x) / dt,
                    (syncedEntity.pos.y - existedEntry.entity.y) / dt,
                    (syncedEntity.pos.z - existedEntry.entity.z) / dt,
                )
            } else {
                Vec3.ZERO
            }

            val entity: Entity
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
            entity.xRot = syncedEntity.xRot
            entity.yRot = syncedEntity.yRot

            // 把服务器随 BVR 包同步的武器备弹写入假实体，使战术地图能正确聚合
            // 远程打击武器（超视距假实体本身没有同步的 GUN_DATA_MAP）。
            if (entity is VehicleEntity) {
                val tag = syncedEntity.tag as? CompoundTag
                val gunAmmoTag = tag?.getCompound("GunAmmo")
                if (gunAmmoTag != null && !gunAmmoTag.isEmpty) {
                    for (name in gunAmmoTag.allKeys) {
                        val gd = entity.gunDataMap[name] ?: continue
                        val shots = gunAmmoTag.getInt(name)
                        val ammoCost = gd.get(GunProp.AMMO_COST_PER_SHOOT)
                        // 无限弹药武器由 queryWeaponAmmo 直接返回 999，无需写入
                        if (ammoCost <= 0) continue
                        if (gd.useBackpackAmmo()) gd.virtualAmmo.set(shots * ammoCost)
                        else gd.ammo.set(shots * ammoCost)
                    }
                }
            }

            SYNCED_WORLD_RENDER[key] = ClientSyncedEntity(
                entity, time, syncedEntity.targetPos, syncedEntity.heightAboveGround, vel,
                shouldWorldRender = true
            )

            // 为超视距载具假实体管理引擎音效和斯图卡音效
            // 若真实实体已存在，只停止残留的假实体音效（真实实体会通过 baseTick 自行创建音效）
            if (entity is VehicleEntity) {
                if (realEntityExists) {
                    phantomEngineSounds.remove(syncedEntity.id)?.let { mc.soundManager.stop(it) }
                    phantomStukaSounds.remove(syncedEntity.id)?.let { mc.soundManager.stop(it) }
                } else {
                    managePhantomEngineSound(entity, syncedEntity.id)
                    managePhantomStukaSound(entity, syncedEntity.id)
                }
            }
        }
    }

    /** 为超视距载具假实体创建或移除引擎音效 */
    private fun managePhantomEngineSound(vehicle: VehicleEntity, id: Int) {
        val existingSound = phantomEngineSounds[id]
        val shouldPlay = vehicle.engineRunning()

        if (shouldPlay && existingSound == null) {
            val sound = VehicleSoundInstance.EngineSound(vehicle)
            phantomEngineSounds[id] = sound
            mc.soundManager.play(sound)
        } else if (!shouldPlay && existingSound != null) {
            mc.soundManager.stop(existingSound)
            phantomEngineSounds.remove(id)
        }
    }

    /** 为超视距载具假实体创建或移除斯图卡尖啸音效 */
    private fun managePhantomStukaSound(vehicle: VehicleEntity, id: Int) {
        val hasStukaConfig = vehicle.computed().engineInfo.get("HasStukaSound")?.asBoolean ?: false
        if (!hasStukaConfig) {
            phantomStukaSounds.remove(id)?.let { mc.soundManager.stop(it) }
            return
        }
        val existingSound = phantomStukaSounds[id]
        val shouldPlay = vehicle.engineRunning() && vehicle.stuka()

        if (shouldPlay && existingSound == null) {
            val sound = VehicleSoundInstance.StukaSound(vehicle)
            phantomStukaSounds[id] = sound
            mc.soundManager.play(sound)
        } else if (!shouldPlay && existingSound != null) {
            mc.soundManager.stop(existingSound)
            phantomStukaSounds.remove(id)
        }
    }

    /** 返回超视距世界渲染池中当前维度的所有实体 */
    @JvmStatic
    fun getSyncedWorldRenderEntities(level: Level): List<Entity> =
        SYNCED_WORLD_RENDER.filterKeys { it.dim == level.dimension().location() }.map { it.value.entity }

    /** 按 ID 从世界渲染池中查找条目 */
    @JvmStatic
    fun getWorldRenderEntry(level: Level, entityId: Int): ClientSyncedEntity? =
        SYNCED_WORLD_RENDER[SyncedKey(level.dimension().location(), entityId)]

    @JvmStatic
    fun getSyncedPlayerInfo(level: Level): List<ClientSyncedPlayer> =
        SYNCED_PLAYERS.filterKeys { it.dim == level.dimension().location() }.map { it.value }

    /**
     * 获取实体的外推位置（速度 × 距离上次同步的时间），用于平滑渲染。
     * 如果实体已在客户端 level 中则返回原位置（由原版插值处理）。
     */
    @JvmStatic
    fun getExtrapolatedPos(level: Level, entity: Entity): Vec3 {
        if (level.getEntity(entity.id) != null) return entity.position()
        val entry = getSyncedEntry(level, entity.id) ?: return entity.position()
        if (entry.velocity.lengthSqr() <= 0.0) return entity.position()
        val elapsed = ((System.currentTimeMillis() - entry.timeStamp) / 50.0).coerceIn(0.0, 2.0)
        return Vec3(
            entity.x + entry.velocity.x * elapsed,
            entity.y + entry.velocity.y * elapsed,
            entity.z + entry.velocity.z * elapsed
        )
    }

    /** 按 ID 从世界渲染池中查找条目 */
    @JvmStatic
    fun getSyncedEntry(level: Level, entityId: Int): ClientSyncedEntity? {
        return SYNCED_WORLD_RENDER[SyncedKey(level.dimension().location(), entityId)]
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

    /** 超视距假实体的引擎音效实例，key 为 entityId */
    private val phantomEngineSounds = ConcurrentHashMap<Int, VehicleSoundInstance>()

    /** 超视距假实体的斯图卡尖啸音效实例，key 为 entityId */
    private val phantomStukaSounds = ConcurrentHashMap<Int, VehicleSoundInstance>()

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
    }

    @JvmStatic
    fun getSyncedRadars(level: Level): List<SyncedRadar> {
        return SYNCED_RADARS.filterKeys { it.dim == level.dimension().location() }.values.toList()
    }
}
