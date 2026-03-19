package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class Sc250Entity(type: EntityType<out Sc250Entity>, level: Level) : AerialBombEntity(type, level),
    BasicGeoProjectileEntity {
    override fun getModel() = BedrockModelLoader.SC_250_MODEL

    init {
        this.noCulling = true
        this.explosionRadiusValue = 20f
        this.explosionDamageValue = 500f
    }
}
