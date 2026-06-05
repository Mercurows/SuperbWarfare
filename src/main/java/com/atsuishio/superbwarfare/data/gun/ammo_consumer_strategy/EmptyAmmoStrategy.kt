package com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy

import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.world.entity.Entity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.items.IItemHandler

/**
 * 空弹药策略 — ammo 字符串形如 "empty"
 */
object EmptyAmmoStrategy : AmmoConsumeStrategy() {

    override val defaultType = AmmoConsumer.AmmoConsumeType.EMPTY

    override fun match(ammo: String): Boolean = ammo.equals("empty", ignoreCase = true)

    override fun consume(data: GunData, consumer: AmmoConsumer, shooter: Entity, count: Int) = 0

    override fun consume(data: GunData, consumer: AmmoConsumer, handler: IItemHandler, count: Int) = 0

    override fun count(data: GunData, consumer: AmmoConsumer, entity: Entity?) = 0

    override fun count(data: GunData, consumer: AmmoConsumer, handler: IItemHandler?) = 0

    override fun withdraw(consumer: AmmoConsumer, ammoSupplier: Entity, count: Int) = 0

    override fun withdraw(consumer: AmmoConsumer, handler: IItemHandler, count: Int) = 0

    @OnlyIn(Dist.CLIENT)
    override fun getDisplayName(consumer: AmmoConsumer) = "Empty"
}
