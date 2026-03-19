package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import java.util.*

open class WireGuideMissileEntity(type: EntityType<out WireGuideMissileEntity>, level: Level) :
    MissileProjectile(type, level), BasicGeoProjectileEntity {

    var launcherVehicleUUID: UUID? = null

    init {
        this.noCulling = true
    }

    override fun getDefaultItem(): Item {
        return ModItems.MEDIUM_ANTI_GROUND_MISSILE.get()
    }

    override fun tick() {
        super.tick()
        mediumTrail()

        val owner = this.owner
        val vehicle = owner?.vehicle
        if (tickCount > 0 && owner != null && vehicle is VehicleEntity) {
            var toVec = deltaMovement
            this.deltaMovement = this.deltaMovement.scale(0.5).add(lookAngle.scale(2.0))

            if (launcherVehicleUUID == vehicle.uuid) {
                val lookVec =
                    if ((vehicle.vehicleType == VehicleType.AIRPLANE || vehicle.vehicleType == VehicleType.HELICOPTER)
                        && owner == vehicle.getFirstPassenger()
                    ) {
                        vehicle.getViewVector(1f).scale(1.6)
                    } else {
                        vehicle.getBarrelVector(1f).scale(1.6)
                    }
                val missileVec = vehicle.getShootPosForHud(owner, 1f).vectorTo(position()).normalize()
                toVec = missileVec.vectorTo(lookVec)
            }

            turn(toVec, ((tickCount - 1) * 0.4f).coerceIn(0f, 6f))
        }
    }

    override fun getSound(): SoundEvent {
        return ModSounds.ROCKET_FLY.get()
    }

    override fun getVolume(): Float {
        return 0.4f
    }

    fun setLauncherVehicle(uuid: UUID?) {
        this.launcherVehicleUUID = uuid
    }

    override val maxHealth: Float
        get() = 20f

    override fun getModel() = BedrockModelLoader.WIRE_GUIDE_MISSILE_MODEL
}
