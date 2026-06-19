package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.LavAdEntity
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
import org.joml.Matrix4f
import org.joml.Vector3f

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

//        // 实体相机相对位置——仅用于将世界坐标换算回相机空间画调试线
//        val entityCamRelPos = Vec3(
//            poseStack.last().pose().m30().toDouble(),
//            poseStack.last().pose().m31().toDouble(),
//            poseStack.last().pose().m32().toDouble()
//        )
//        val entityWorldPos = vehicle.position()

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

//            // 调试线段：世界坐标 → 相机相对坐标
//            val camRelPos = worldPos.subtract(entityWorldPos).add(entityCamRelPos)
//            renderBoneDebugLine(camRelPos, worldDir, buffer)

            // 粒子：直接使用世界坐标
            vehicle.level().addParticle(
                ModParticleTypes.FIRE_STAR.get(),
                worldPos.x, worldPos.y, worldPos.z,
                worldDir.x, worldDir.y, worldDir.z
            )
        }
    }

    // ===================== 骨骼世界坐标工具方法 =====================

    /**
     * 计算骨骼的世界坐标和朝向。
     *
     * 世界坐标 = 实体世界位置 + pivot + R_world * (modelPos - pivot)
     * 其中 R_world 与 [SbmVehicleRenderer.rotateVehicleAxis] 的旋转顺序一致：
     * Z(roll) → X(pitch) → Y(yaw)，即矩阵 RZ * RX * RY。
     *
     * @return Pair<Vec3, Vec3> — (世界坐标, 世界朝向单位向量)
     */
    private fun getBoneWorldPosAndDirection(
        vehicle: LavAdEntity,
        bone: BedrockBone,
        entityYaw: Float,
        partialTicks: Float
    ): Pair<Vec3, Vec3> {
        // 1. 骨骼在模型空间中的变换（SBM库迭代所有parent累乘得到）
        val boneTransform = Matrix4f(bone.globalTransform)
        val modelPos = Vec3(
            boneTransform.m30().toDouble(),
            boneTransform.m31().toDouble(),
            boneTransform.m32().toDouble()
        )

        // 2. 骨骼在模型空间中的前向（局部Z轴正方向）
        val localForward = Vector3f(0f, 0f, 1f)
        val modelDirVec = Vector3f()
        boneTransform.transformDirection(localForward, modelDirVec)
        val modelDir = Vec3(
            modelDirVec.x().toDouble(),
            modelDirVec.y().toDouble(),
            modelDirVec.z().toDouble()
        )

        // 3. 构建实体世界旋转矩阵 —— 与 rotateVehicleAxis 完全一致
        //    rotateVehicleAxis 依次调用 rotateAround(R_Y) → rotateAround(R_X) → rotateAround(R_Z)
        //    rotateAround 内部做 T(p)*R*T(-p)，相邻的 T(±p) 相消后得到 T(p) * R_Z * R_X * R_Y * T(-p)
        //    故旋转部分是 R_Z * R_X * R_Y（JOML 右乘：I.rotateZ.rotateX.rotateY）
        val pitch = Mth.lerp(partialTicks, vehicle.xRotO + vehicle.fakePitchO, vehicle.xRot + vehicle.fakePitch)
        val roll = Mth.lerp(partialTicks, vehicle.prevRoll + vehicle.fakeRollO, vehicle.roll + vehicle.fakeRoll)
        val pivotY = vehicle.rotateOffsetHeight

        val worldRot = Matrix4f()
            .rotateZ(-roll * Mth.DEG_TO_RAD)
            .rotateX(-pitch * Mth.DEG_TO_RAD)
            .rotateY((-entityYaw + 180f) * Mth.DEG_TO_RAD)

        // 4. 旋转中心偏移：pivot + R * (modelPos - pivot)
        val relativeToPivot = Vector3f(
            modelPos.x.toFloat(),
            (modelPos.y - pivotY).toFloat(),
            modelPos.z.toFloat()
        )
        val rotatedRelative = Vector3f()
        worldRot.transformPosition(relativeToPivot, rotatedRelative)

        val worldPos = vehicle.position().add(
            rotatedRelative.x().toDouble(),
            rotatedRelative.y().toDouble() + pivotY,
            rotatedRelative.z().toDouble()
        )

        // 5. 方向旋转到世界空间
        val worldDirVec = Vector3f()
        worldRot.transformDirection(
            Vector3f(modelDir.x.toFloat(), modelDir.y.toFloat(), modelDir.z.toFloat()),
            worldDirVec
        )
        val worldDir = Vec3(
            worldDirVec.x().toDouble(),
            worldDirVec.y().toDouble(),
            worldDirVec.z().toDouble()
        ).normalize()

        return Pair(worldPos, worldDir)
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
