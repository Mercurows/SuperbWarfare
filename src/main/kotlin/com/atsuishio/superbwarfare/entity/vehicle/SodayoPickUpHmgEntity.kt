package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance
import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class SodayoPickUpHmgEntity(type: EntityType<SodayoPickUpHmgEntity>, world: Level) : GeoVehicleEntity(type, world), BasicGeoVehicleEntity {
    override fun baseTick() {
        super.baseTick()
        if (decoyInputDown) {
            horn()
        }
    }

    val anim: VehicleAnimationInstance<SodayoPickUpHmgEntity>? =
        if (world.isClientSide) VehicleAnimationInstance(this) else null
    override fun getAnimationInstance() = anim
    override fun getAnimation() = ANIM
    companion object {
        val ANIM = Mod.loc("animation/bedrock/vehicle/sodayo_pick_up_hmg.animation.json")
    }
}
