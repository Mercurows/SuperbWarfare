package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation

class LavAdRenderer<T>(manager: EntityRendererProvider.Context) :
    SbmVehicleRenderer<T>(manager) where T : VehicleEntity, T : BasicGeoVehicleEntity {

    override fun hideForTurretControllerWhileZooming(): Boolean {
        return true
    }

    override fun transformCustomModelPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)

        val rotBarrel = model.getBone("rot_barrel")

        val gunData = vehicle.getGunData(0, 0)
        if (gunData != null) {
            rotBarrel.rotation.rotationZ(-0.5f * (gunData.shootTimer.get() * System.currentTimeMillis() % 36000000) / 75f)
        }

        val flare = model.getBone("flare")

        if (gunData != null) {
            flare.visible = gunData.shootTimer.get() > 2
            flare.xScale = (2 + 0.8 * (Math.random() - 0.5)).toFloat()
            flare.yScale = (2 + 0.8 * (Math.random() - 0.5)).toFloat()
            flare.rotation.rotationZ((0.5 * (Math.random() - 0.5)).toFloat())
        } else {
            flare.visible = false
        }
    }

    override fun renderCustomPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        super.renderCustomPart(vehicle, model, poseStack, entityYaw, partialTicks, buffer, packedLight)

        val gunData = vehicle.getGunData(0, 0)
        if (gunData != null && gunData.shootTimer.get() > 2) {
            model.renderToBuffer(
                poseStack,
                buffer,
                ModRenderTypes.MUZZLE_FLASH_TYPE.apply(getMuzzleFlareTextureLocation()),
                BedrockModelRenderTypes.polyMeshCutout(getMuzzleFlareTextureLocation()),
                packedLight,
                OverlayTexture.NO_OVERLAY
            )
        }

        val heat = vehicle.getWeaponHeat(0).toFloat()

        if (heat > 0) {
            model.renderToBuffer(
                poseStack,
                buffer.getBuffer(RenderType.eyes(getBarrelHeatTextureLocation())),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                heat / 100,
                heat / 100,
                heat / 100,
                1f
            )
        }
    }

    fun getMuzzleFlareTextureLocation(): ResourceLocation {
        return Mod.loc("textures/bedrock/vehicle/hpj_11_e.png")
    }

    fun getBarrelHeatTextureLocation(): ResourceLocation {
        return Mod.loc("textures/bedrock/vehicle/lav_ad_heat.png")
    }
}
