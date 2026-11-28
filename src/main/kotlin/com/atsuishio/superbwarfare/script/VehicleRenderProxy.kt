package com.atsuishio.superbwarfare.script

import com.atsuishio.superbwarfare.entity.vehicle.SodayoPickUpRocketEntity
import com.atsuishio.superbwarfare.entity.vehicle.TowEntity
import com.atsuishio.superbwarfare.entity.vehicle.Type63Entity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import com.atsuishio.superbwarfare.tools.options
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.TntMinecartRenderer
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.registries.ForgeRegistries

/** Client view and synced model state exposed to vehicle render scripts. */
@Suppress("unused")
object VehicleRenderProxy {
    private fun isFirstPersonOrZooming() = options.cameraType.isFirstPerson || ClientEventHandler.zoomVehicle

    fun isTurretControllerInFirstPerson(vehicle: VehicleEntity): Boolean =
        vehicle.turretControllerIndex == vehicle.getSeatIndex(localPlayer) && isFirstPersonOrZooming()

    fun isArmedPassengerInFirstPerson(vehicle: VehicleEntity): Boolean {
        val player = localPlayer ?: return false
        return player.vehicle === vehicle && vehicle.hasWeapon(vehicle.getSeatIndex(player)) && isFirstPersonOrZooming()
    }

    fun isPilotZooming(vehicle: VehicleEntity): Boolean =
        ClientEventHandler.zoomVehicle && vehicle.firstPassenger === localPlayer

    fun isSeatInFirstPerson(vehicle: VehicleEntity, seat: Int): Boolean =
        vehicle.getNthEntity(seat) === localPlayer && isFirstPersonOrZooming()

    fun getHorizontalSpeed(vehicle: VehicleEntity): Float = vehicle.deltaMovement.horizontalDistance().toFloat()

    fun hasLoadedMissile(vehicle: TowEntity): Boolean = vehicle.entityData.get(TowEntity.LOADED)

    fun hasLoadedWeapon(vehicle: VehicleEntity, name: String): Boolean =
        vehicle.getGunData(name)?.hasEnoughAmmoToShoot(vehicle) == true

    fun renderBlock(
        blockId: String, poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int,
        x: Double, y: Double, z: Double, scale: Float
    ) {
        val id = ResourceLocation.tryParse(blockId) ?: return
        val block = ForgeRegistries.BLOCKS.getValue(id) ?: return
        poseStack.pushPose()
        try {
            poseStack.translate(x, y, z)
            poseStack.scale(scale, scale, scale)
            poseStack.translate(-0.5, -0.5, -0.5)
            TntMinecartRenderer.renderWhiteSolidBlock(
                mc.blockRenderer, block.defaultBlockState(), poseStack, buffer, packedLight, false
            )
        } finally {
            poseStack.popPose()
        }
    }

    fun getLoadedAmmo(vehicle: VehicleEntity): List<Int>? = when (vehicle) {
        is Type63Entity -> vehicle.entityData.get(Type63Entity.LOADED_AMMO)
        is SodayoPickUpRocketEntity -> vehicle.entityData.get(SodayoPickUpRocketEntity.LOADED_AMMO)
        else -> null
    }
}
