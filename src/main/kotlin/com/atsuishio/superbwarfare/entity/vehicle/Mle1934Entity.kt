package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class Mle1934Entity(type: EntityType<Mle1934Entity>, world: Level) : ArtilleryEntity(type, world) {
    override fun canBind() = true
}
