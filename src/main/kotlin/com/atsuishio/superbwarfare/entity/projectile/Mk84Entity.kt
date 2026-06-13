package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level

open class Mk84Entity(type: EntityType<out Mk84Entity>, level: Level) : AerialBombEntity(type, level),
    BasicGeoProjectileEntity {

    init {
        this.explosionRadiusValue = 32f
        this.explosionDamageValue = 1300f
    }

    override fun getDefaultItem(): Item {
        return ModItems.LARGE_AERIAL_BOMB.get()
    }

    override val maxHealth: Float
        get() = 90f
}
