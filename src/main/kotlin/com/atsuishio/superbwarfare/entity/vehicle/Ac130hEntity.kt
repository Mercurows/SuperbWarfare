package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

open class Ac130hEntity(type: EntityType<Ac130hEntity>, world: Level) : VehicleEntity(type, world) {
    private var wasGearUp = false

    override fun baseTick() {
        super.baseTick()

        val gearUp = (this.gearUp && synchedGearRot > 0 && synchedGearRot < 1) || synchedGearRot == 1f
        val gearDown = (!this.gearUp && synchedGearRot > 0 && synchedGearRot < 1) || synchedGearRot == 0f

        if (level().isClientSide) {
            val ctx = anim?.context ?: return
            if (gearUp && !wasGearUp) {
                ctx.playAnimation("animation.ac_130h.gear_up", AnimationPlayType.LOOP,
                    fadeInTicks = 260)
            } else if (gearDown && wasGearUp) {
                ctx.stopAnimation("animation.ac_130h.idle",
                    fadeOutTicks = 260)
            }
            wasGearUp = gearUp
        }

        if (!onGround() && engineStartOver && firstPassenger == null && energy > 0 && !isWreck && getPassengers().isNotEmpty()) {
            deltaRot += 0.12f
            mouseMoveSpeedX = -1f
            power = Mth.lerp(0.05f, power, 1.1f)
            xRot *= 0.995f
        }
    }
}
