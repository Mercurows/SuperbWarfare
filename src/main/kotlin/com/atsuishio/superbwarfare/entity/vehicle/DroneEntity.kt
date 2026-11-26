package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.RCEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

open class DroneEntity(type: EntityType<out DroneEntity>, level: Level) : RCEntity(type, level)
