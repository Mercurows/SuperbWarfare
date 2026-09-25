package com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy

import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.AmmoSource
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy.MobAmmoStrategy.count
import com.atsuishio.superbwarfare.data.mob_guns.MobGunState
import net.minecraft.world.entity.Entity
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.items.IItemHandler
import kotlin.math.min

/**
 * 生物弹药池策略 — ammo 字符串形如 `"mob"`、`"mob_ammo"`。
 *
 * 生物持枪时的备弹保存在**生物自己身上**（[MobGunState]），而不是枪的物品 NBT 里：
 * 枪只携带弹匣内那点弹药，所以生物死亡掉落枪械时不会连几百发备弹一起掉出来。
 *
 * 需要说明的是，持枪生物默认**不需要**声明这个来源：池子本身已经作为「备弹」参与
 * [GunData.countBackupAmmo] 的求和与消耗（见 `GunData.consumeBackupAmmo`），
 * 于是无论原枪声明的是玩家弹药、物品弹药还是能量，生物都能从池子里上膛。
 * 这个策略存在的意义是给数据包一个显式写法（例如「这把枪对生物而言只吃池子」）；
 * 显式声明时隐式的那份会自动让位，不会重复计数。
 *
 * 玩家与载具身上没有这个池子，[count] 恒为 0，因此同一个弹种字符串对它们是一个空来源。
 */
object MobAmmoStrategy : AmmoConsumeStrategy() {

    /** 数据包里写的弹药来源关键字 */
    const val KEY = "mob"

    private const val ALIAS = "mob_ammo"

    override val defaultType = AmmoConsumer.AmmoConsumeType.ITEM

    override fun match(ammo: String) = ammo.equals(KEY, true) || ammo.equals(ALIAS, true)

    /** 从生物弹药池扣除，返回实际消耗的池内数量 */
    override fun consume(data: GunData, source: AmmoSource, shooter: Entity?, count: Int): Int {
        if (shooter == null || count <= 0) return 0

        val consumed = min(MobGunState.ammo(shooter), count)
        if (consumed > 0) MobGunState.addAmmo(shooter, -consumed)
        return consumed
    }

    override fun consume(data: GunData, source: AmmoSource, handler: IItemHandler, count: Int) = 0

    override fun count(data: GunData, source: AmmoSource, entity: Entity?) =
        if (entity == null) 0 else MobGunState.ammo(entity)

    override fun count(data: GunData, source: AmmoSource, handler: IItemHandler?) = 0

    /** 退弹时把弹药还进池子（生物没有物品栏可以接收弹药） */
    override fun withdraw(source: AmmoSource, ammoSupplier: Entity, count: Int): Int {
        if (count <= 0) return 0
        MobGunState.addAmmo(ammoSupplier, count)
        return count
    }

    override fun withdraw(source: AmmoSource, handler: IItemHandler, count: Int) = 0

    @OnlyIn(Dist.CLIENT)
    override fun getDisplayName(source: AmmoSource) = "Mob Ammo"
}
