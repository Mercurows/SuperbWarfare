package com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy

import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.tools.forceHurt
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.items.IItemHandler
import kotlin.math.floor

/**
 * health
 * health 10 (每1血=10弹药)
 */
class HealthAmmoStrategy : AmmoConsumeStrategy() {

    override val defaultType = AmmoConsumer.AmmoConsumeType.ITEM

    var ammoPerHealth: Float = 1F

    override fun create(): AmmoConsumeStrategy = HealthAmmoStrategy()

    override fun match(ammo: String) = ammo.lowercase().startsWith("health", true)
            && ammo.lowercase() == "health"
            || ammo.lowercase().substringAfter("health").trimEnd().toFloatOrNull() != null

    override fun init(
        consumer: AmmoConsumer,
        count: Int,
        matchedString: String
    ) {
        super.init(consumer, count, matchedString)
        val extracted = matchedString.lowercase().substringAfter("health").trimEnd().toFloatOrNull()?.coerceAtLeast(0F)
            ?: 1F
        ammoPerHealth = if (extracted.isNaN() || extracted == 0F || extracted.isInfinite()) 1F else extracted
    }

    override fun consume(data: GunData, consumer: AmmoConsumer, shooter: Entity, count: Int): Int {
        shooter.invulnerableTime = 0

        shooter.forceHurt(
            ModDamageTypes.causeAmmoConsumptionDamage(shooter.level().registryAccess(), shooter),
            count / ammoPerHealth
        )
        return 1
    }

    override fun consume(data: GunData, consumer: AmmoConsumer, handler: IItemHandler, count: Int) = 0

    override fun count(data: GunData, consumer: AmmoConsumer, entity: Entity?) =
        floor(((entity as? LivingEntity)?.health ?: 0F) * ammoPerHealth - 0.00001).toInt()

    override fun count(data: GunData, consumer: AmmoConsumer, handler: IItemHandler?) = 0

    override fun withdraw(consumer: AmmoConsumer, ammoSupplier: Entity, count: Int): Int {
        (ammoSupplier as? LivingEntity)?.heal(count / ammoPerHealth) ?: return 0
        return count
    }

    override fun withdraw(consumer: AmmoConsumer, handler: IItemHandler, count: Int) = 0

    @OnlyIn(Dist.CLIENT)
    override fun getDisplayName(consumer: AmmoConsumer) = "Health"
}
