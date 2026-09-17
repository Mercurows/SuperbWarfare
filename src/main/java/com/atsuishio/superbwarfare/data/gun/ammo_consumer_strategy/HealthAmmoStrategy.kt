package com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy

import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.AmmoSource
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.tools.forceHurt
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.items.IItemHandler
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
        source: AmmoSource,
        count: Int,
        matchedString: String
    ) {
        super.init(source, count, matchedString)
        val extracted = matchedString.lowercase().substringAfter("health").trimEnd().toFloatOrNull()?.coerceAtLeast(0F)
            ?: 1F
        ammoPerHealth = if (extracted.isNaN() || extracted == 0F || extracted.isInfinite()) 1F else extracted
    }

    override fun consume(data: GunData, source: AmmoSource, shooter: Entity?, count: Int): Int {
        if (shooter == null) return 0
        shooter.invulnerableTime = 0

        shooter.forceHurt(
            ModDamageTypes.causeAmmoConsumptionDamage(shooter.level().registryAccess(), shooter),
            count / ammoPerHealth
        )
        return 1
    }

    override fun consume(data: GunData, source: AmmoSource, handler: IItemHandler, count: Int) = 0
    override fun count(data: GunData, source: AmmoSource, entity: Entity?) =
        floor(((entity as? LivingEntity)?.health ?: 0F) * ammoPerHealth - 0.00001).toInt()

    override fun count(data: GunData, source: AmmoSource, handler: IItemHandler?) = 0
    override fun withdraw(source: AmmoSource, ammoSupplier: Entity, count: Int): Int {
        (ammoSupplier as? LivingEntity)?.heal(count / ammoPerHealth) ?: return 0
        return count
    }

    override fun withdraw(source: AmmoSource, handler: IItemHandler, count: Int) = 0

    @OnlyIn(Dist.CLIENT)
    override fun getDisplayName(source: AmmoSource) = "Health"
}
