package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class Ah6Entity(type: EntityType<Ah6Entity>, world: Level) : GeoVehicleEntity(type, world) {

    override fun getDamageModifier() = super.getDamageModifier()
        .custom { _, damage -> damage * if (health > 0.1f) 1f else 0.05f }

    override fun getMouseSensitivity() = 0.25
}
