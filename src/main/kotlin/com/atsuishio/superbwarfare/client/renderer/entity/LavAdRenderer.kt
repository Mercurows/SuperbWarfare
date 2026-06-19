package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.LavAdEntity
import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.Mth

class LavAdRenderer(manager: EntityRendererProvider.Context) : SbmVehicleRenderer<LavAdEntity>(manager) {
    override fun hideForTurretControllerWhileZooming(): Boolean {
        return true
    }

    override fun renderCustomPart(
        vehicle: LavAdEntity,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        super.renderCustomPart(vehicle, model, poseStack, entityYaw, partialTicks, buffer, packedLight)

        val heat = Mth.clamp(vehicle.getWeaponHeat(0).toFloat(), 0f, 100f)

        if (heat > 0) {
            model.renderToBuffer(
                poseStack,
                buffer.getBuffer(RenderType.eyes(HEAT)),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                heat / 100,
                heat / 100,
                heat / 100,
                1f
            )
        }

        // 在rocket_right骨骼位置渲染粒子
        val bone = model.getBone("rocket_right")
        if (bone != null) {
            // 直接计算世界坐标和朝向（与相机无关）
            val (worldPos, worldDir) = getBoneWorldPosAndDirection(vehicle, bone, entityYaw, partialTicks)

            // 粒子：直接使用世界坐标
            vehicle.level().addParticle(
                ModParticleTypes.FIRE_STAR.get(),
                worldPos.x, worldPos.y, worldPos.z,
                worldDir.x, worldDir.y, worldDir.z
            )
        }
    }

    companion object {
        val HEAT = loc("textures/bedrock/vehicle/lav_ad_heat.png")
    }
}
