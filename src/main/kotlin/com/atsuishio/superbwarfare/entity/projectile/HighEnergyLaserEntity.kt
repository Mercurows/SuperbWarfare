package com.atsuishio.superbwarfare.entity.projectile

import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class HighEnergyLaserEntity(type: EntityType<out HighEnergyLaserEntity>, level: Level) : LaserBeamEntity(type, level) {
    init {
        beamWidth = 0.24
        beamColor = 0x55BFFF
    }
}
