package com.atsuishio.superbwarfare.entity.vehicle

import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

open class FpvDroneEntity(type: EntityType<out FpvDroneEntity>, level: Level) : DroneEntity(type, level)