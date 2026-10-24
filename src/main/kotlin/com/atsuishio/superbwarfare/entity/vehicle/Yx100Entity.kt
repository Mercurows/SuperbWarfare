package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class Yx100Entity(type: EntityType<Yx100Entity>, world: Level) : VehicleEntity(type, world) {
    override fun getTrackAnimationLength() = 80

    override val customTurretMinPitch: Float
        get() = if (Mth.abs(wrappedTurretYRot) > 135) ((Mth.abs(wrappedTurretYRot) - 135) * 0.5f).coerceAtMost(5f) else 0f
}
