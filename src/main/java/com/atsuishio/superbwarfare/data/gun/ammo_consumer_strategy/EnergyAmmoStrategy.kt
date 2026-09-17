package com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy

import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.AmmoSource
import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.world.entity.Entity
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandler

/**
 * 能量弹药策略 — ammo 字符串形如 "fe"、 "rf"、 "energy"
 *
 * 作为附加来源时（如泰瑟枪的 `"400 fe"`），前缀即每发消耗的 FE 数量。
 */
object EnergyAmmoStrategy : AmmoConsumeStrategy() {

    override val defaultType = AmmoConsumer.AmmoConsumeType.ENERGY

    override fun match(ammo: String) = ammo.lowercase() in setOf("fe", "rf", "energy")

    override fun consume(data: GunData, source: AmmoSource, shooter: Entity?, count: Int): Int {
        val energyStorage = data.getEnergyProvider(shooter) ?: return 0
        return energyStorage.extractEnergy(count, false)
    }

    override fun consume(data: GunData, source: AmmoSource, handler: IItemHandler, count: Int): Int {
        val energyStorage = data.stack.getCapability(Capabilities.EnergyStorage.ITEM) ?: return 0
        return energyStorage.extractEnergy(count, false)
    }

    override fun count(data: GunData, source: AmmoSource, entity: Entity?): Int {
        // 枪械自身的能量存储与持有者无关，entity 为 null 时依然可以读取
        val energyStorage = data.getEnergyProvider(entity) ?: return 0
        return energyStorage.energyStored
    }

    override fun count(data: GunData, source: AmmoSource, handler: IItemHandler?): Int {
        if (handler == null) return 0
        val energyStorage = data.stack.getCapability(Capabilities.EnergyStorage.ITEM) ?: return 0
        return energyStorage.energyStored
    }

    override fun withdraw(source: AmmoSource, ammoSupplier: Entity, count: Int) = 0
    override fun withdraw(source: AmmoSource, handler: IItemHandler, count: Int) = 0

    @OnlyIn(Dist.CLIENT)
    override fun getDisplayName(source: AmmoSource) = "Energy"
}
