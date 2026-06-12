package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level

open class Mk84Entity(type: EntityType<out Mk84Entity>, level: Level) : AerialBombEntity(type, level),
    BasicGeoProjectileEntity {

    init {
        this.noCulling = true
        this.explosionRadiusValue = 32f
        this.explosionDamageValue = 1300f
    }

    override fun getDefaultItem(): Item {
        return ModItems.LARGE_AERIAL_BOMB.get()
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        val entity = source.directEntity
        if (entity is Mk84Entity && entity.owner == this.owner) {
            return false
        }

        return super.hurt(source, amount)
    }

    override val maxHealth: Float
        get() = 90f
}
