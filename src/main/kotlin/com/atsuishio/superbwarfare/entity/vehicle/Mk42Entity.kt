package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class Mk42Entity(type: EntityType<Mk42Entity>, world: Level) : ArtilleryEntity(type, world) {
    override fun canBind() = true
}
