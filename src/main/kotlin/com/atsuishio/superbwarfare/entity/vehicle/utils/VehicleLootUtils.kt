package com.atsuishio.superbwarfare.entity.vehicle.utils

import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.data.loot.WreckageLootData
import com.atsuishio.superbwarfare.data.loot.WreckageLootDataManager
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraftforge.registries.ForgeRegistries
import kotlin.random.Random

/**
 * 处理载具残骸战利品生成方法的工具类
 */
object VehicleLootUtils {

    /**
     * 生成残骸战利品
     *
     * @param vehicle 载具
     */
    @JvmStatic
    fun generateWreckageLoot(vehicle: VehicleEntity) {
        val data = WreckageLootDataManager.getLootData(vehicle.type) ?: return
        val pools = data.pools
        if (pools.isEmpty()) return
        pools.forEach poolLoop@{ pool ->
            val type = pool.type
            if (type == WreckageLootData.Pool.Type.TURRET_ONLY) return@poolLoop
            val entries = pool.entries
            if (entries.isEmpty()) return@poolLoop
            val source = pool.source
            if (source != "@Default") {
                val lastSource = vehicle.lastDamageSource ?: return@poolLoop
                val parsedLoc = ResourceLocation.tryParse(source) ?: return@poolLoop
                val damageType = ResourceKey.create(Registries.DAMAGE_TYPE, parsedLoc)
                if (!lastSource.`is`(damageType)) return@poolLoop
            } else if (vehicle.lastDamageSource?.`is`(ModDamageTypes.REPAIR_TOOL) == true) {
                return@poolLoop
            }

            repeat(pool.rolls) {
                entries.forEach { entry ->
                    val random = Random.nextDouble()
                    val chance =
                        if (type == WreckageLootData.Pool.Type.VEHICLE_ONLY) {
                            if (vehicle.hasTurret() && vehicle.sympatheticDetonated) {
                                entry.chance
                            } else return@poolLoop
                        } else if (type == WreckageLootData.Pool.Type.COMPLETE) {
                            if (vehicle.hasTurret()) {
                                if (vehicle.sympatheticDetonated) return@poolLoop
                                else entry.chance
                            } else {
                                entry.chance
                            }
                        } else {
                            entry.chance * if (vehicle.hasTurret() && vehicle.sympatheticDetonated) (1.0 - VehicleConfig.TURRET_WRECKAGE_LOOT_RATE.get()) else 1.0
                        }

                    if (random > chance) return@forEach
                    val name = entry.name
                    val item = ForgeRegistries.ITEMS.getValue(ResourceLocation(name)) ?: return@forEach
                    val count = entry.count
                    val entity = ItemEntity(vehicle.level(), vehicle.x, (vehicle.y + 1), vehicle.z, ItemStack(item, count))
                    entity.setPickUpDelay(10)
                    vehicle.level().addFreshEntity(entity)
                }
            }
        }
    }
}
