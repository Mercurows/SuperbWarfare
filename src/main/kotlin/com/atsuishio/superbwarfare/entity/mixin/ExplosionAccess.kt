package com.atsuishio.superbwarfare.entity.mixin

import net.minecraft.world.level.Explosion

@Suppress("FunctionName")
interface ExplosionAccess {
    fun `superbwarfare$getRadius`(): Float

    companion object {
        fun of(explosion: Explosion): ExplosionAccess {
            return explosion as ExplosionAccess
        }
    }
}
