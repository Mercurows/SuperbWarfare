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
 * hunger
 * hunger 10 (每1饥饿值=10弹药)
 */
class HungerAmmoStrategy : AmmoConsumeStrategy() {

    override val defaultType = AmmoConsumer.AmmoConsumeType.ITEM

    var ammoPerHunger: Float = 1F

    override fun create(): AmmoConsumeStrategy = HungerAmmoStrategy()

    override fun match(ammo: String) = ammo.lowercase().startsWith("hunger", true)
            && ammo.lowercase() == "hunger"
            || ammo.lowercase().substringAfter("hunger").trimEnd().toFloatOrNull() != null

    override fun init(
        consumer: AmmoConsumer,
        count: Int,
        matchedString: String
    ) {
        super.init(consumer, count, matchedString)
        val extracted = matchedString.lowercase().substringAfter("hunger").trimEnd().toFloatOrNull()?.coerceAtLeast(0F)
            ?: 1F
        ammoPerHunger = if (extracted.isNaN() || extracted == 0F || extracted.isInfinite()) 1F else extracted
    }

    override fun consume(data: GunData, consumer: AmmoConsumer, shooter: Entity, count: Int): Int {
        val foodData = (shooter as? Player)?.foodData ?: return 0
        val hungerToConsume = (count / ammoPerHunger).toInt()
        foodData.foodLevel = (foodData.foodLevel - hungerToConsume).coerceAtLeast(0)
        return hungerToConsume
    }

    override fun consume(data: GunData, consumer: AmmoConsumer, handler: IItemHandler, count: Int) = 0

    override fun count(data: GunData, consumer: AmmoConsumer, entity: Entity?) =
        floor(((entity as? Player)?.foodData?.foodLevel?.toFloat() ?: 0F) * ammoPerHunger).toInt()

    override fun count(data: GunData, consumer: AmmoConsumer, handler: IItemHandler?) = 0

    override fun withdraw(consumer: AmmoConsumer, ammoSupplier: Entity, count: Int): Int {
        val foodData = (ammoSupplier as? Player)?.foodData ?: return 0
        val hungerToRestore = (count / ammoPerHunger).toInt().coerceAtLeast(1)
        foodData.foodLevel = (foodData.foodLevel + hungerToRestore).coerceAtMost(20)
        return hungerToRestore
    }

    override fun withdraw(consumer: AmmoConsumer, handler: IItemHandler, count: Int) = 0

    @OnlyIn(Dist.CLIENT)
    override fun getDisplayName(consumer: AmmoConsumer) = "Hunger"
}
