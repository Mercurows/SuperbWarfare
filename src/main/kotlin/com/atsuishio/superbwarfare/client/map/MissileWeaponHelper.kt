package com.atsuishio.superbwarfare.client.map

import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.google.gson.JsonObject
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

/**
 * 导弹武器聚合工具。
 */
object MissileWeaponHelper {
    fun JsonObject.gsonBool(vararg keys: String) =
        keys.firstNotNullOfOrNull { if (has(it)) get(it).asBoolean else null } ?: false

    fun JsonObject.gsonDouble(vararg keys: String) =
        keys.firstNotNullOfOrNull { if (has(it)) get(it).asDouble else null }

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
     */
    fun getSelectedVehicles(selectedEntities: List<Entity>, localPlayer: Player?): List<VehicleEntity> {
        val sel = selectedEntities.filterIsInstance<VehicleEntity>()
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
            val gd = vehicle.gunDataMap[weaponName] ?: return@firstOrNull false
            val ammoCost = gd.get(GunProp.AMMO_COST_PER_SHOOT)
            val available = if (ammoCost <= 0) 999 else gd.currentAvailableAmmo(player) / ammoCost
            available > 0
        }
    }

    /**
     * 汇总所有选中载具中指定武器的总弹药数。
     */
    fun currentAttackAmmo(weaponName: String, vehicles: List<VehicleEntity>, player: Player?): Int {
        if (vehicles.isEmpty()) return 0
        return vehicles.sumOf { vehicle ->
            val gd = vehicle.gunDataMap[weaponName] ?: return@sumOf 0
            val ammoCost = gd.get(GunProp.AMMO_COST_PER_SHOOT)
            if (ammoCost <= 0) 999 else gd.currentAvailableAmmo(player) / ammoCost
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
    ): List<AggregatedWeapon> {
        if (vehicles.isEmpty()) return emptyList()
        if (targetEntity != null && vehicles.any { it === targetEntity || it.id == targetEntity.id }) return emptyList()

        val level = vehicles.first().level()
        val heightAboveGround = if (targetEntity != null) {
            ClientSyncedEntityHandler.getSyncedEntry(level, targetEntity.id)?.heightAboveGround ?: -1.0
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
                if (!canLockEntity && !canGroundStrike) continue

                val ammoCost = gunData.get(GunProp.AMMO_COST_PER_SHOOT)
                val available = if (ammoCost <= 0) 999 else gunData.currentAvailableAmmo(null) / ammoCost
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
