package com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy

import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.world.entity.Entity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.items.IItemHandler

/**
 * 无限弹药策略 — ammo 字符串形如 "infinite"、 "infinity"
 */
object InfiniteAmmoStrategy : AmmoConsumeStrategy() {

    override val defaultType = AmmoConsumer.AmmoConsumeType.INFINITE

    override fun match(ammo: String) =
        ammo.equals("infinite", ignoreCase = true) || ammo.equals("infinity", ignoreCase = true)

    override fun consume(data: GunData, consumer: AmmoConsumer, shooter: Entity, count: Int) = 0

    override fun consume(data: GunData, consumer: AmmoConsumer, handler: IItemHandler, count: Int) = 0

    override fun count(data: GunData, consumer: AmmoConsumer, entity: Entity?) = Int.MAX_VALUE

    override fun count(data: GunData, consumer: AmmoConsumer, handler: IItemHandler?) = Int.MAX_VALUE

    override fun withdraw(consumer: AmmoConsumer, ammoSupplier: Entity, count: Int) = 0

    override fun withdraw(consumer: AmmoConsumer, handler: IItemHandler, count: Int) = 0

    @OnlyIn(Dist.CLIENT)
    override fun getDisplayName(consumer: AmmoConsumer) = "Infinite"
}
