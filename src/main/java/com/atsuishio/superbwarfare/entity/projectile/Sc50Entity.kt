package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class Sc50Entity(type: EntityType<out Sc50Entity>, level: Level) : AerialBombEntity(type, level),
    BasicGeoProjectileEntity {
    override fun getModel() = BedrockModelLoader.SC_50_MODEL

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
