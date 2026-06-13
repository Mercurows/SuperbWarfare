package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level

open class Bor57Entity(type: EntityType<out Bor57Entity>, level: Level) : AerialBombEntity(type, level),
    BasicGeoProjectileEntity {

    init {
        this.noCulling = true
        this.explosionRadiusValue = 42f
        this.explosionDamageValue = 2200f
    }

    override fun getDefaultItem(): Item {
        return ModItems.LARGE_AERIAL_BOMB.get()
    }

    override val maxHealth: Float
        get() = 400f
}
