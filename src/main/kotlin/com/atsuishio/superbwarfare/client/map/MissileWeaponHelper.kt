package com.atsuishio.superbwarfare.client.map

import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler
import com.atsuishio.superbwarfare.client.map.MissileWeaponHelper.getSelectedVehicles
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.google.gson.JsonObject
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap

/**
 * 远程打击武器聚合工具。
 *
 * 统一管理战术地图中所有远程打击手段（导弹、火炮、火箭弹等）的武器查询、
 * 弹药统计和目标筛选逻辑。所有战术地图备弹量显示都应使用此类提供的方法。
 */
object MissileWeaponHelper {
    fun JsonObject.gsonBool(vararg keys: String) =
        keys.firstNotNullOfOrNull { if (has(it)) get(it).asBoolean else null } ?: false

    fun JsonObject.gsonDouble(vararg keys: String) =
        keys.firstNotNullOfOrNull { if (has(it)) get(it).asDouble else null }

    /** 对客户端 level 中已存在的实体（非超视距同步），使用高度图实时计算离地高度 */
    private fun computeEntityHeightAboveGround(level: Level, entity: Entity): Double {
        val surfaceY = level.getHeight(
            Heightmap.Types.WORLD_SURFACE,
            entity.blockX,
            entity.blockZ
        )
        return (entity.y - surfaceY).coerceAtLeast(0.0)
    }

    /** 单种武器聚合结果 */
    data class AggregatedWeapon(
        val weaponName: String,
        val displayNameBase: String,
        val totalAmmo: Int,
        val canLockEntity: Boolean,
        val maxGuidedRange: Double = 2048.0,
        val inRange: Boolean = true,
    )

    /**
     * 获取所有被选中的载具（用于遥控打击），若未选中则回退到当前骑乘载具。
     *
     * **注意**：战术地图渲染/选中的是超视距同步实体（[ClientSyncedEntityHandler.SYNCED_WORLD_RENDER]），
     * 这类假实体的 `GUN_DATA_MAP` 未随 BVR 包同步，武器弹药数据为空，导致
     * [queryWeaponAmmo] / [aggregateWeapons] 得到空列表。因此这里会优先把同步实体
     * 解析回客户端 level 中的真实实体（同一 ID），真实实体的弹药数据才是权威来源。
     */
    fun getSelectedVehicles(selectedEntities: List<Entity>, localPlayer: Player?): List<VehicleEntity> {
        val level = localPlayer?.level()
        val sel = selectedEntities.filterIsInstance<VehicleEntity>().mapNotNull { vehicle ->
            // 真实实体存在时优先使用真实实体，否则回退到同步实体
            val real = level?.getEntity(vehicle.id) as? VehicleEntity
            real ?: vehicle
        }
        if (sel.isNotEmpty()) return sel
        val ridden = localPlayer?.vehicle as? VehicleEntity
        return if (ridden != null) listOf(ridden) else emptyList()
    }

    /**
     * 在所有选中载具中查找第一个拥有指定武器且有弹药的载具。
     */
    fun findFirstVehicleWithWeapon(
        weaponName: String,
        vehicles: List<VehicleEntity>,
        player: Player?
    ): VehicleEntity? {
        return vehicles.firstOrNull { vehicle ->
            queryWeaponAmmo(weaponName, listOf(vehicle)) > 0
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  通用弹药查询（所有战术地图备弹显示的权威入口）
    // ═══════════════════════════════════════════════════════════════

    /**
     * **客户端实时查询指定武器在载具列表中的可用射击次数。**
     *
     * 战术地图中所有远程打击武器（导弹、火炮、火箭弹等）的备弹量显示
     * **必须**使用此方法。仅读取载具同步的 [virtualAmmo][GunData.virtualAmmo]，
     * 不读取玩家背包弹药，避免因背包缓存导致的数值卡住问题。
     *
     * 调用方不需要手动处理弹药消耗系数——本方法已内置 `AMMO_COST_PER_SHOOT` 除法。
     *
     * @param weaponName 武器注册名（对应 [VehicleEntity.gunDataMap] 的 key）
     * @param vehicles   载具列表（通常来自 [getSelectedVehicles]）
     * @return 可用射击次数。若弹药消耗系数 ≤0 则返回 999（视为无限弹药），
     *         若指定武器不存在或无弹药则返回 0
     */
    fun queryWeaponAmmo(weaponName: String, vehicles: List<VehicleEntity>): Int {
        if (vehicles.isEmpty()) return 0
        return vehicles.sumOf { vehicle ->
            val gd = vehicle.gunDataMap[weaponName] ?: return@sumOf 0
            val ammoCost = gd.get(GunProp.AMMO_COST_PER_SHOOT)
            if (ammoCost <= 0) 999 else gd.currentAvailableAmmo(null) / ammoCost
        }
    }

    /**
     * 汇总选中载具中所有具备指定目标能力的导弹武器。
     *
     * @param vehicles         选中载具列表
     * @param targetEntity     目标实体（null 表示对地打击）
     * @param requireLockEntity 是否筛选可锁定实体的武器
     * @param requireLockBlock  是否筛选可对地打击的武器
     * @return 聚合后的武器列表
     */
    fun aggregateWeapons(
        vehicles: List<VehicleEntity>,
        targetEntity: Entity?,
        requireLockEntity: Boolean,
        requireLockBlock: Boolean,
        /** 目标离地高度（无实体时手动指定，-1 表示未知） */
        targetHeight: Double = -1.0,
    ): List<AggregatedWeapon> {
        if (vehicles.isEmpty()) return emptyList()
        if (targetEntity != null && vehicles.any { it === targetEntity || it.id == targetEntity.id }) return emptyList()

        val level = vehicles.first().level()
        val heightAboveGround = if (targetHeight >= 0) targetHeight
        else if (targetEntity != null) {
            ClientSyncedEntityHandler.getSyncedEntry(level, targetEntity.id)?.heightAboveGround
                ?: computeEntityHeightAboveGround(level, targetEntity)
        } else -1.0

        data class InnerAgg(
            val totalAmmo: Int, val canLockEntity: Boolean, val displayNameBase: String,
            val maxGuidedRange: Double
        )

        val weaponMap = linkedMapOf<String, InnerAgg>()

        for (vehicle in vehicles) {
            for ((name, gunData) in vehicle.gunDataMap) {
                val currentSeek = gunData.get(GunProp.SEEK_WEAPON_INFO)
                // 若 CanGuidedByRadar 为 false，该武器不可由雷达引导，不应出现在可用列表中
                var canGuidedByRadar = currentSeek?.canGuidedByRadar ?: true
                var canLockEntity = currentSeek?.onlyLockEntity == true
                var canGroundStrike = currentSeek?.onlyLockBlock == true || currentSeek?.inputBlockPos == true
                var minH = if (canLockEntity) currentSeek?.minTargetHeight ?: 0.0 else Double.MAX_VALUE
                var maxH = if (canLockEntity) currentSeek?.maxTargetHeight ?: 114514.0 else -1.0
                var maxGuidedRange = currentSeek?.maxGuidedRange ?: 2048.0

                val consumers = gunData.get(GunProp.AMMO_CONSUMER)
                for (c in consumers) {
                    val o = c.override ?: continue
                    val seekObj = o.getAsJsonObject("SeekWeaponInfo")
                        ?: o.getAsJsonObject("seekWeaponInfo") ?: continue
                    // 检查 override 中是否显式禁用了雷达引导
                    if (seekObj.has("CanGuidedByRadar")) {
                        canGuidedByRadar = seekObj.get("CanGuidedByRadar").asBoolean
                    }
                    if (!canLockEntity) {
                        canLockEntity = seekObj.gsonBool("OnlyLockEntity", "onlyLockEntity")
                        if (canLockEntity) {
                            minH = seekObj.gsonDouble("MinTargetHeight", "minTargetHeight") ?: 0.0
                            maxH = seekObj.gsonDouble("MaxTargetHeight", "maxTargetHeight") ?: 114514.0
                        }
                    }
                    if (!canGroundStrike) {
                        canGroundStrike = seekObj.gsonBool("OnlyLockBlock", "onlyLockBlock")
                                || seekObj.gsonBool("InputBlockPos", "inputBlockPos")
                    }
                    // override 中的 MaxGuidedRange
                    val overrideMgr = seekObj.gsonDouble("MaxGuidedRange", "maxGuidedRange")
                    if (overrideMgr != null) maxGuidedRange = overrideMgr
                }

                // 不可由雷达引导的武器直接跳过
                if (!canGuidedByRadar) continue

                // Apply filters
                if (requireLockEntity && !canLockEntity && !(requireLockBlock && canGroundStrike)) continue
                if (requireLockBlock && !canGroundStrike && !(requireLockEntity && canLockEntity)) continue
                if (!requireLockEntity && !requireLockBlock) continue
                if (!canLockEntity && !canGroundStrike) continue

                if (requireLockEntity && canLockEntity && heightAboveGround >= 0) {
                    if (heightAboveGround !in minH..maxH) canLockEntity = false
                }
                // 若实体锁定因目标高度超出上限而被禁用，且目标是实体（非地面坐标），
                // 则同时禁用对地打击——对高空实体发射对地导弹没有意义
                if (targetEntity != null && !canLockEntity && maxH > 0 && heightAboveGround > maxH) {
                    canGroundStrike = false
                }
                if (!canLockEntity && !canGroundStrike) continue

                val available = queryWeaponAmmo(name, listOf(vehicle))
                if (available <= 0) continue

                val rawName = gunData.get(GunProp.NAME) ?: name
                val translated = try {
                    Component.translatable(rawName).string
                } catch (_: Exception) {
                    rawName
                }

                val existing = weaponMap[name]
                if (existing != null) {
                    weaponMap[name] = InnerAgg(
                        existing.totalAmmo + available, existing.canLockEntity,
                        existing.displayNameBase, minOf(existing.maxGuidedRange, maxGuidedRange)
                    )
                } else {
                    weaponMap[name] = InnerAgg(available, canLockEntity, translated, maxGuidedRange)
                }
            }
        }

        // Check if at least one selected vehicle is within MaxGuidedRange of the target
        val targetDist = targetEntity?.let { target ->
            vehicles.minOfOrNull { it.distanceTo(target) }
        }

        return weaponMap.map { (name, agg) ->
            val inRange = targetDist == null || targetDist <= agg.maxGuidedRange
            val ammoStr = if (inRange)
                Component.translatable("context.superbwarfare.tactical_map.missile_ammo_count", agg.totalAmmo).string
            else
                Component.translatable("context.superbwarfare.tactical_map.out_of_range").string
            val displayName = if (agg.displayNameBase.contains("%1\$s"))
                agg.displayNameBase.replace("%1\$s", ammoStr) else "${agg.displayNameBase}  $ammoStr"
            AggregatedWeapon(name, displayName, agg.totalAmmo, agg.canLockEntity, agg.maxGuidedRange, inRange)
        }
    }
}
