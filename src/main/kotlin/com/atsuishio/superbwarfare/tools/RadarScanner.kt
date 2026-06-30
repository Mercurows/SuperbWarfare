package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.entity.projectile.MissileProjectile
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.message.receive.EntitySyncMessage
import com.atsuishio.superbwarfare.network.message.receive.EntitySyncMessage.SyncedEntity
import com.atsuishio.superbwarfare.network.message.receive.RadarSyncMessage
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.OwnableEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.minecraftforge.registries.ForgeRegistries

/**
 * 通用雷达扫描器 —— 双端一致的扇面搜索逻辑。
 *
 * 使用方式：
 * ```
 * val config = RadarConfig(
 *     owner = player,
 *     center = radarBlockPos.center,
 *     radius = 1024.0,
 *     sweepAngle = 60.0,
 *     rotationSpeed = 1.0,    // 每 tick 旋转 1 度
 *     searchType = SearchType.VEHICLES,
 * )
 * val result = RadarScanner.scan(level, config, currentTick)
 * // 发送结果：
 * result.sendToClients()
 * ```
 */
object RadarScanner {

    enum class SearchType {
        /** 只搜索载具和导弹（从 ServerSyncedEntityHandler 查询，快速） */
        VEHICLES,
        /** 只搜索生物（遍历 level.allEntities，慢） */
        LIVING,
        /** 两者都搜索 */
        ALL,
    }

    data class RadarConfig(
        val owner: Player,
        val center: Vec3,
        val radius: Double,
        /** 搜索扇面角度（度），360 表示全向 */
        val sweepAngle: Double,
        /** 雷达面基础朝向 YRot（度），自旋模式下作为初始偏移 */
        val yRot: Double = 0.0,
        /** 自旋速度（度/tick），0 表示由外部控制朝向，正值顺时针，负值逆时针 */
        val rotationSpeed: Double = 0.0,
        val searchType: SearchType,
        /** 目标最小高度（仅载具搜索生效，null 表示不限制） */
        val minTargetHeight: Double? = null,
        /** 目标最大高度（仅载具搜索生效，null 表示不限制） */
        val maxTargetHeight: Double? = null,
        /** 是否将雷达信息同步给队友，false 则仅拥有者可见 */
        val shareWithTeammates: Boolean = true,
        /** 是否在战术地图上显示雷达图标，雷达方块为 true，载具为 false */
        val showIcon: Boolean = true,
        /** 雷达源唯一标识，用于客户端去重。方块用 "block_<pos>"，载具用 "vehicle_<id>" */
        val sourceId: String = "",
        /** 是否会被隐身目标影响 */
        val affectedByStealthTarget: Boolean = true,
    ) {
        /** 计算当前 tick 的有效朝向 */
        fun effectiveYRot(currentTick: Int): Double {
            if (rotationSpeed == 0.0) return yRot
            return (yRot + currentTick * rotationSpeed) % 360.0
        }
    }

    data class ScanResult(
        val dim: net.minecraft.resources.ResourceLocation,
        val hostile: List<SyncedEntity>,
        val neutral: List<SyncedEntity>,
        val friendly: List<SyncedEntity>,
    ) {
        fun sendToClients(owner: Player, level: ServerLevel, shareWithTeammates: Boolean = true) {
            val recipients = level.players()
                .asSequence()
                .filter {
                    it == owner || (shareWithTeammates && SeekTool.IS_FRIENDLY.test(owner, it))
                }
                .toList()

            if (hostile.isNotEmpty()) {
                val msg = EntitySyncMessage(dim, hostile, friendly = false, neutral = false)
                recipients.forEach { sendPacketTo(it, msg) }
            }
            if (neutral.isNotEmpty()) {
                val msg = EntitySyncMessage(dim, neutral, friendly = false, neutral = true)
                recipients.forEach { sendPacketTo(it, msg) }
            }
        }
    }

    /**
     * 同步雷达配置到客户端（每 tick 调用以保证旋转流畅）。
     * 独立于实体扫描，即使未到扫描间隔也会发送雷达位置和朝向。
     */
    fun sendRadarConfig(config: RadarConfig, level: ServerLevel) {
        val recipients = level.players()
            .asSequence()
            .filter {
                it == config.owner || (config.shareWithTeammates && SeekTool.IS_FRIENDLY.test(config.owner, it))
            }
            .toList()
        val effectiveYRot = config.effectiveYRot(level.server.tickCount)
        val msg = RadarSyncMessage(level.dimension().location(), listOf(
            RadarSyncMessage.SyncedRadar(
                pos = config.center,
                radius = config.radius,
                sweepAngle = config.sweepAngle,
                yRot = effectiveYRot,
                ownerName = config.owner.displayName.string,
                showIcon = config.showIcon,
                sourceId = config.sourceId,
            )
        ))
        recipients.forEach { sendPacketTo(it, msg) }
    }

    /**
     * 执行一次雷达扫描。
     * @param level       服务端世界
     * @param config      雷达配置
     */
    fun scan(level: ServerLevel, config: RadarConfig): ScanResult {
        val dim = level.dimension().location()
        val radiusSq = config.radius * config.radius
        val effectiveYRot = config.effectiveYRot(level.server.tickCount)

        val angleRad = effectiveYRot * Math.PI / 180.0
        val sweepDir = Vec3(
            Mth.sin(angleRad.toFloat()).toDouble(),
            0.0,
            -Mth.cos(angleRad.toFloat()).toDouble()
        )

        val hostileList = mutableListOf<SyncedEntity>()
        val neutralList = mutableListOf<SyncedEntity>()

        // ── 搜索载具和导弹 ──
        if (config.searchType == SearchType.VEHICLES || config.searchType == SearchType.ALL) {
            for (entry in ServerSyncedEntityHandler.getEntries(dim)) {
                // 跳过 self
                if (entry.entityId == config.owner.id) continue

                // 距离检查（隐身载具的 trackDistanceMultiply 减益其被探测距离）
                val effectiveRangeSq = radiusSq * (if (config.affectedByStealthTarget) entry.trackDistanceMultiply * entry.trackDistanceMultiply else 1.0)
                if (entry.pos.distanceToSqr(config.center) > effectiveRangeSq) continue

                // 查找真实实体
                val entity = level.getEntity(entry.entityId) ?: continue

                // 忽略载具残骸
                if (entity is VehicleEntity && entity.isWreck) continue

                // 地下检查：实体顶部低于地表则跳过
                if (ServerSyncedEntityHandler.isUnderground(entity)) continue

                // 扇面角度检查
                val toEntity = config.center.vectorTo(entry.pos).multiply(1.0, 0.0, 1.0)
                if (VectorTool.calculateAngle(toEntity, sweepDir) > config.sweepAngle / 2.0) continue

                // 烟雾检查
                if (!SeekTool.NOT_IN_SMOKE.test(entity)) continue

                // 高度范围检查
                if (config.minTargetHeight != null && config.maxTargetHeight != null) {
                    if (!SeekTool.IN_HEIGHT_RANGE.test(entity, config.minTargetHeight, config.maxTargetHeight))
                        continue
                }

                val synced = SyncedEntity(
                    entry.entityId, entry.entityType, entry.pos, entry.targetPos, entry.nbt, entry.yRot, entry.xRot,
                    heightAboveGround = entry.heightAboveGround,
                )

                // 友方导弹由 MissileProjectile.tick() 自行同步，雷达只上报敌方导弹
                if (entity is MissileProjectile) {
                    val missileOwner = entity.owner
                    if (missileOwner == null || !SeekTool.IS_FRIENDLY.test(config.owner, missileOwner)) {
                        hostileList.add(synced)
                    }
                    continue
                }
                when {
                    // 中立：无驾驶员、无主人、lastDriverUUID 为空
                    isNeutral(entity) -> neutralList.add(synced)
                    // 敌对
                    !SeekTool.IS_FRIENDLY.test(config.owner, entity) -> hostileList.add(synced)
                    // 友方不在此处处理（由 IffItem 单独同步）
                }
            }
        }

        // ── 搜索生物 ──
        if (config.searchType == SearchType.LIVING) {
            level.allEntities.asSequence()
                .filter { it is LivingEntity }
                .filter { !ServerSyncedEntityHandler.isUnderground(it) }
                .filter { it.id != config.owner.id }
                .filter { it.distanceToSqr(config.center) <= radiusSq }
                .filter {
                    val toEntity = config.center.vectorTo(it.position()).multiply(1.0, 0.0, 1.0)
                    VectorTool.calculateAngle(toEntity, sweepDir) <= config.sweepAngle / 2.0
                }
                .filter { SeekTool.NOT_IN_SMOKE.test(it) }
                .filter { !SeekTool.IS_FRIENDLY.test(config.owner, it) }
                .forEach {
                    val surfaceY = it.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, it.blockX, it.blockZ)
                    val hag = it.y - surfaceY
                    val synced = SyncedEntity(
                        it.id,
                        ForgeRegistries.ENTITY_TYPES.getKey(it.type)!!,
                        it.position(),
                        null,
                        it.serializeNBT(),
                        it.yRot,
                        it.xRot,
                        heightAboveGround = hag.coerceAtLeast(0.0),
                    )
                    // 根据生物类别分类：敌对生物（怪物）→ hostile，被动生物（动物等）→ neutral
                    if (it.type.category == MobCategory.MONSTER) {
                        hostileList.add(synced)
                    } else {
                        neutralList.add(synced)
                    }
                }
        }

        return ScanResult(
            dim = dim,
            hostile = hostileList,
            neutral = neutralList,
            friendly = emptyList(),
        )
    }

    /**
     * 判定载具是否为中立（无驾驶员、无主人、lastDriverUUID 为空）。
     * 中立载具不属于任何一方，显示为白色图标。
     */
    fun isNeutral(entity: Entity): Boolean {
        if (entity !is VehicleEntity) return false
        if (entity.isWreck) return false
        val noDriver = entity.firstPassenger == null
        val noOwner = entity !is OwnableEntity || entity.ownerUUID == null
        val noLastDriver = entity.lastDriverUUID == "undefined" || entity.lastDriverUUID.isEmpty()
        return noDriver && noOwner && noLastDriver
    }
}
