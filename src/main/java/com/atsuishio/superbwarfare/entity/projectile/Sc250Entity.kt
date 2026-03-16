package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class Sc250Entity(type: EntityType<out Sc250Entity>, level: Level) : AerialBombEntity(type, level), BasicGeoProjectileEntity {
    override val model: ResourceLocation
        get() = BedrockModelLoader.SC_250_MODEL

    init {
        this.noCulling = true
        this.explosionRadius = 20f
        this.explosionDamage = 500f
    }
}
