package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f

class LavAdRenderer<T>(manager: EntityRendererProvider.Context) :
    SbmVehicleRenderer<T>(manager) where T : VehicleEntity, T : BasicGeoVehicleEntity {

    override fun hideForTurretControllerWhileZooming(): Boolean {
        return true
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

        // 获取实体在PoseStack中的相机相对参考位置，用于后续推算世界坐标
        val entityCamRelPos = Vec3(
            poseStack.last().pose().m30().toDouble(),
            poseStack.last().pose().m31().toDouble(),
            poseStack.last().pose().m32().toDouble()
        )
        val entityWorldPos = vehicle.position()
        // 相机相对坐标 → 世界绝对坐标的转换偏移量
        val camToWorld = entityWorldPos.subtract(entityCamRelPos)

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

        // 调试：渲染rocket_right骨骼的朝向线条 + 粒子
        val bone = model.getBone("rocket_right")
        if (bone != null) {
            val (camRelPos, direct) = getBoneWorldPosAndDirection(poseStack, bone)

            bone.globalTransform
            // 线段渲染：直接使用相机相对坐标（与模型在相同坐标空间）
            renderBoneDebugLine(camRelPos, direct, buffer)

            // 粒子：相机相对坐标 + camToWorld = 世界绝对坐标
            val worldPos = camRelPos.add(camToWorld)
            vehicle.level().addParticle(
                ModParticleTypes.FIRE_STAR.get(),
                worldPos.x, worldPos.y, worldPos.z,
                direct.x, direct.y, direct.z
            )
        }
    }

    // ===================== 骨骼世界坐标工具方法 =====================

    /**
     * 获取骨骼在当前渲染PoseStack下的世界位置和朝向。
     * 手动遍历骨骼层级（root → 目标骨骼），顺序与 BedrockBone.translateAndRotateAndScale 一致。
     *
     * @return Pair<Vec3, Vec3> — (世界位置, 世界朝向单位向量)
     */
    private fun getBoneWorldPosAndDirection(
        poseStack: PoseStack,
        bone: BedrockBone
    ): Pair<Vec3, Vec3> {
        poseStack.pushPose()
        applyBoneHierarchy(poseStack, bone)

        val matrix = poseStack.last().pose()
        val pos = Vec3(matrix.m30().toDouble(), matrix.m31().toDouble(), matrix.m32().toDouble())

        val localForward = Vector3f(0f, 0f, 1f)
        val worldForward = Vector3f()
        matrix.transformDirection(localForward, worldForward)
        val direct = Vec3(
            worldForward.x.toDouble(),
            worldForward.y.toDouble(),
            worldForward.z.toDouble()
        )

        poseStack.popPose()
        return Pair(pos, direct)
    }

    /**
     * 从root到目标骨骼依次对PoseStack应用层级变换：
     * 平移 → 旋转 → 缩放（与BedrockBone渲染时完全一致）
     */
    private fun applyBoneHierarchy(poseStack: PoseStack, bone: BedrockBone) {
        val chain = mutableListOf<BedrockBone>()
        var b: BedrockBone? = bone
        while (b != null) {
            chain.add(b)
            b = b.parent
        }
        for (boneInChain in chain.reversed()) {
            poseStack.translate(
                boneInChain.x / 16.0,
                boneInChain.y / 16.0,
                boneInChain.z / 16.0
            )
            poseStack.last().pose().rotate(boneInChain.rotation)
            poseStack.last().normal().rotate(boneInChain.rotation)
            if (boneInChain.xScale != 0f || boneInChain.yScale != 0f || boneInChain.zScale != 0f) {
                poseStack.scale(boneInChain.xScale, boneInChain.yScale, boneInChain.zScale)
            }
        }
    }

    // ===================== 调试渲染 =====================

    /**
     * 在给定世界位置沿给定方向渲染一条蓝色调试线段（类似F3+B的实体视线线）
     */
    private fun renderBoneDebugLine(
        pos: Vec3,
        direct: Vec3,
        buffer: MultiBufferSource,
        lineLength: Double = 2.0
    ) {
        val endPos = pos.add(direct.scale(lineLength))
        val lineConsumer: VertexConsumer = buffer.getBuffer(RenderType.LINES)

        // 蓝色半透明线段（RGB: 0, 0.5, 1, 模仿F3+B效果）
        lineConsumer.vertex(pos.x, pos.y, pos.z)
            .color(0f, 0.5f, 1f, 1f)
            .normal(0f, 1f, 0f)
            .endVertex()
        lineConsumer.vertex(endPos.x, endPos.y, endPos.z)
            .color(0f, 0.5f, 1f, 1f)
            .normal(0f, 1f, 0f)
            .endVertex()
    }

    companion object {
        val HEAT = loc("textures/bedrock/vehicle/lav_ad_heat.png")
    }
}
