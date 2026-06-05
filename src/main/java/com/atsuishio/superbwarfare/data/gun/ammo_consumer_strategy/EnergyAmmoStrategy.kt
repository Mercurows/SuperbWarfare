package com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy

import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.world.entity.Entity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.items.IItemHandler

/**
 * 能量弹药策略 — ammo 字符串形如 "fe"、 "rf"、 "energy"
 */
object EnergyAmmoStrategy : AmmoConsumeStrategy() {

    override val defaultType = AmmoConsumer.AmmoConsumeType.ENERGY

    override fun match(ammo: String) = ammo.lowercase() in setOf("fe", "rf", "energy")

    override fun consume(data: GunData, consumer: AmmoConsumer, shooter: Entity, count: Int): Int {
        return data.getEnergyProvider(shooter).map { it.extractEnergy(count, false) }.orElseGet { 0 }
    }

    override fun consume(data: GunData, consumer: AmmoConsumer, handler: IItemHandler, count: Int): Int {
        return data.stack.getCapability(ForgeCapabilities.ENERGY).map { it.extractEnergy(count, false) }.orElseGet { 0 }
    }

    override fun count(data: GunData, consumer: AmmoConsumer, entity: Entity?): Int {
        if (entity == null) return 0
        return data.getEnergyProvider(entity).map { it.energyStored }.orElseGet { 0 }
    }

    override fun count(data: GunData, consumer: AmmoConsumer, handler: IItemHandler?): Int {
        if (handler == null) return 0
        return data.stack.getCapability(ForgeCapabilities.ENERGY).map { it.energyStored }.orElseGet { 0 }
    }

    override fun withdraw(consumer: AmmoConsumer, ammoSupplier: Entity, count: Int) = 0

    override fun withdraw(consumer: AmmoConsumer, handler: IItemHandler, count: Int) = 0

    @OnlyIn(Dist.CLIENT)
    override fun getDisplayName(consumer: AmmoConsumer) = "Energy"
}
