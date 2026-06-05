package com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.world.entity.Entity
import net.minecraftforge.items.IItemHandler

/**
 * 无效弹药策略 — 兜底策略，匹配所有未被其他策略匹配的 ammo 字符串。
 *
 * match: 始终返回 true（最后顺位）
 * init: 记录无效弹药警告
 * consume / count / withdraw: 均返回 0
 */
object InvalidAmmoStrategy : AmmoConsumeStrategy() {

    override val defaultType = AmmoConsumer.AmmoConsumeType.INVALID

    override fun match(ammo: String) = true

    override fun init(consumer: AmmoConsumer, count: Int, matchedString: String) {
        Mod.LOGGER.warn("invalid ammo value: {}", consumer.ammo)
    }

    override fun consume(data: GunData, consumer: AmmoConsumer, shooter: Entity, count: Int) = 0

    override fun consume(data: GunData, consumer: AmmoConsumer, handler: IItemHandler, count: Int) = 0

    override fun count(data: GunData, consumer: AmmoConsumer, entity: Entity?) = 0

    override fun count(data: GunData, consumer: AmmoConsumer, handler: IItemHandler?) = 0

    override fun withdraw(consumer: AmmoConsumer, ammoSupplier: Entity, count: Int) = 0

    override fun withdraw(consumer: AmmoConsumer, handler: IItemHandler, count: Int) = 0
}
