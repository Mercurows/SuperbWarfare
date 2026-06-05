package com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy

import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.items.IItemHandler
import kotlin.math.floor

/**
 * exp
 * exp 10 (每1经验=10弹药)
 */
class ExpAmmoStrategy : AmmoConsumeStrategy() {

    override val defaultType = AmmoConsumer.AmmoConsumeType.ITEM

    var ammoPerXp: Float = 1F

    override fun create(): AmmoConsumeStrategy = ExpAmmoStrategy()

    override fun match(ammo: String) = ammo.lowercase().startsWith("exp", true)
            && ammo.lowercase() == "exp"
            || ammo.lowercase().substringAfter("exp").trimEnd().toFloatOrNull() != null

    override fun init(
        consumer: AmmoConsumer,
        count: Int,
        matchedString: String
    ) {
        super.init(consumer, count, matchedString)
        val extracted = matchedString.lowercase().substringAfter("exp").trimEnd().toFloatOrNull()?.coerceAtLeast(0F)
            ?: 1F
        ammoPerXp = if (extracted.isNaN() || extracted == 0F || extracted.isInfinite()) 1F else extracted
    }

    override fun consume(data: GunData, consumer: AmmoConsumer, shooter: Entity, count: Int): Int {
        val player = (shooter as? Player) ?: return 0
        val xpToConsume = (count / ammoPerXp).toInt()
        player.totalExperience = (player.totalExperience - xpToConsume).coerceAtLeast(0)
        return xpToConsume
    }

    override fun consume(data: GunData, consumer: AmmoConsumer, handler: IItemHandler, count: Int) = 0

    override fun count(data: GunData, consumer: AmmoConsumer, entity: Entity?) =
        floor(((entity as? Player)?.totalExperience?.toFloat() ?: 0F) * ammoPerXp).toInt()

    override fun count(data: GunData, consumer: AmmoConsumer, handler: IItemHandler?) = 0

    override fun withdraw(consumer: AmmoConsumer, ammoSupplier: Entity, count: Int): Int {
        val player = (ammoSupplier as? Player) ?: return 0
        val xpToRestore = (count / ammoPerXp).toInt().coerceAtLeast(1)
        player.giveExperiencePoints(xpToRestore)
        return xpToRestore
    }

    override fun withdraw(consumer: AmmoConsumer, handler: IItemHandler, count: Int) = 0

    @OnlyIn(Dist.CLIENT)
    override fun getDisplayName(consumer: AmmoConsumer) = "Experience"
}
