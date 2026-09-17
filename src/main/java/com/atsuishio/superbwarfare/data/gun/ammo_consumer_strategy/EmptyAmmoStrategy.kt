package com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy

import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.AmmoSource
import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.world.entity.Entity
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.items.IItemHandler


/**
 * 空弹药策略 — ammo 字符串形如 "empty"
 */
object EmptyAmmoStrategy : AmmoConsumeStrategy() {

    override val defaultType = AmmoConsumer.AmmoConsumeType.EMPTY

    override fun match(ammo: String): Boolean = ammo.equals("empty", ignoreCase = true)

    override fun consume(data: GunData, source: AmmoSource, shooter: Entity?, count: Int) = 0
    override fun consume(data: GunData, source: AmmoSource, handler: IItemHandler, count: Int) = 0
    override fun count(data: GunData, source: AmmoSource, entity: Entity?) = 0
    override fun count(data: GunData, source: AmmoSource, handler: IItemHandler?) = 0
    override fun withdraw(source: AmmoSource, ammoSupplier: Entity, count: Int) = 0
    override fun withdraw(source: AmmoSource, handler: IItemHandler, count: Int) = 0

    @OnlyIn(Dist.CLIENT)
    override fun getDisplayName(source: AmmoSource) = "Empty"
}
