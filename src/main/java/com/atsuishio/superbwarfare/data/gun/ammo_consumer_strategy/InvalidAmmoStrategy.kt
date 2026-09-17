package com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.AmmoSource
import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.world.entity.Entity
import net.neoforged.neoforge.items.IItemHandler

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

    override fun init(source: AmmoSource, count: Int, matchedString: String) {
        Mod.LOGGER.warn("invalid ammo value: {}", source.ammo)
    }

    override fun consume(data: GunData, source: AmmoSource, shooter: Entity?, count: Int) = 0
    override fun consume(data: GunData, source: AmmoSource, handler: IItemHandler, count: Int) = 0
    override fun count(data: GunData, source: AmmoSource, entity: Entity?) = 0
    override fun count(data: GunData, source: AmmoSource, handler: IItemHandler?) = 0
    override fun withdraw(source: AmmoSource, ammoSupplier: Entity, count: Int) = 0
    override fun withdraw(source: AmmoSource, handler: IItemHandler, count: Int) = 0
}
