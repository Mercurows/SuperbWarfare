package com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy

import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.world.entity.Entity
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandler

/**
 * 能量弹药策略 — ammo 字符串形如 "fe"、 "rf"、 "energy"
 */
object EnergyAmmoStrategy : AmmoConsumeStrategy() {

    override val defaultType = AmmoConsumer.AmmoConsumeType.ENERGY

    override fun match(ammo: String) = ammo.lowercase() in setOf("fe", "rf", "energy")

    override fun consume(data: GunData, consumer: AmmoConsumer, shooter: Entity, count: Int): Int {
        val energyStorage = data.getEnergyProvider(shooter) ?: return 0
        return energyStorage.extractEnergy(count, false)
    }

    override fun consume(data: GunData, consumer: AmmoConsumer, handler: IItemHandler, count: Int): Int {
        val energyStorage = data.stack.getCapability(Capabilities.EnergyStorage.ITEM) ?: return 0
        return energyStorage.extractEnergy(count, false)
    }

    override fun count(data: GunData, consumer: AmmoConsumer, entity: Entity?): Int {
        if (entity == null) return 0
        val energyStorage = data.getEnergyProvider(entity) ?: return 0
        return energyStorage.energyStored
    }

    override fun count(data: GunData, consumer: AmmoConsumer, handler: IItemHandler?): Int {
        if (handler == null) return 0
        val energyStorage = data.stack.getCapability(Capabilities.EnergyStorage.ITEM) ?: return 0
        return energyStorage.energyStored
    }

    override fun withdraw(consumer: AmmoConsumer, ammoSupplier: Entity, count: Int) = 0
    override fun withdraw(consumer: AmmoConsumer, handler: IItemHandler, count: Int) = 0
}
