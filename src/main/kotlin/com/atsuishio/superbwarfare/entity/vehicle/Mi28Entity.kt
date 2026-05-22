package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

open class Mi28Entity(type: EntityType<Mi28Entity>, world: Level) : VehicleEntity(type, world), BasicGeoVehicleEntity
