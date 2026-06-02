package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import org.joml.Math

open class Ju87Entity(type: EntityType<Ju87Entity>, world: Level) : VehicleEntity(type, world) {
    override var turretYRot = 180f
    override var turretYRotO = 180f

    var smallPropellerO = 0f
    var smallPropeller = 180f

    override fun baseTick() {
        smallPropellerO = smallPropeller
        super.baseTick()
        if (level().isClientSide) {
            smallPropeller += deltaMovement.dot(lookAngle).toFloat()

            val delta = Math.abs(smallPropeller - smallPropellerO)
            while (smallPropeller > 180f) {
                smallPropeller -= 360f
                smallPropellerO = smallPropeller - delta
            }
            while (smallPropeller <= -180f) {
                smallPropeller += 360f
                smallPropellerO = delta + smallPropeller
            }

        }
    }
}
