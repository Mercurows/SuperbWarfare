package com.atsuishio.superbwarfare.entity.projectile

import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class Sc50Entity(type: EntityType<out Sc50Entity>, level: Level) : AerialBombEntity(type, level),
    BasicGeoProjectileEntity {
    init {
        this.noCulling = true
        this.explosionRadiusValue = 11f
        this.explosionDamageValue = 120f
    }

    override fun getVolume(): Float {
        return 0.4f
    }

    override val maxHealth: Float
        get() = 25f
}
