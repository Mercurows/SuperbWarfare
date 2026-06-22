package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class Bl132Entity(type: EntityType<Bl132Entity>, world: Level) : ArtilleryEntity(type, world) {
    override fun canBind() = true
}
