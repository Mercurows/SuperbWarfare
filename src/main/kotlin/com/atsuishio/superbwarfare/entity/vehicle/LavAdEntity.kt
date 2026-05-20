package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class LavAdEntity(type: EntityType<LavAdEntity>, world: Level) : VehicleEntity(type, world), BasicGeoVehicleEntity
