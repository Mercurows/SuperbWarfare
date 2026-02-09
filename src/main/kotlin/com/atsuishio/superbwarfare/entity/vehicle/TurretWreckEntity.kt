package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.level.Level

class TurretWreckEntity(type: EntityType<TurretWreckEntity>, world: Level) : GeoVehicleEntity(type, world) {
    var wreckageType: String = "superbwarfare:lav_150"

    override fun baseTick() {
        super.baseTick()

        this.move(MoverType.SELF, this.deltaMovement)
        var f = 0.98f
        if (this.onGround()) {
            val pos = this.blockPosBelowThatAffectsMyMovement
            f = level().getBlockState(pos).getFriction(this.level(), pos, this) * 0.98f
        }

        this.deltaMovement = deltaMovement.multiply(f.toDouble(), 0.98, f.toDouble())
        if (this.onGround()) {
            this.deltaMovement = deltaMovement.multiply(1.0, -0.9, 1.0)
        }
    }
}
