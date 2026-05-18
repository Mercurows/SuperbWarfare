package com.atsuishio.superbwarfare.entity.projectile

import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class Sc250Entity(type: EntityType<out Sc250Entity>, level: Level) : AerialBombEntity(type, level),
    BasicGeoProjectileEntity {
    init {
        this.noCulling = true
        this.explosionRadiusValue = 20f
        this.explosionDamageValue = 500f
    }
}
