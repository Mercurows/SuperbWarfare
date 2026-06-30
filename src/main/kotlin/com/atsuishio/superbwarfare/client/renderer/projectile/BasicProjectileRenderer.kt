package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.BasicGeoProjectileEntity
import com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.maydaymemory.mae.basic.ArrayPoseBuilder
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory
import com.maydaymemory.mae.blend.EulerAdditiveBlender
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

open class BasicProjectileRenderer<T>(manager: EntityRendererProvider.Context) :
    EntityRenderer<T>(manager) where T : Entity, T : BasicGeoProjectileEntity {
    override fun getTextureLocation(entity: T): ResourceLocation {
        val (_, namespace, id) = entity.type.descriptionId.split(".")
        return ResourceLocation(namespace, "textures/bedrock/projectile/$id.png")
    }

    fun getModelLocation(entity: T): ResourceLocation {
        val (_,  namespace, id) = entity.type.descriptionId.split(".")
        return ResourceLocation(namespace, "models/bedrock/projectile/$id.geo.json")
    }

    override fun shouldShowName(pEntity: T): Boolean {
        return false
    }

    override fun shouldRender(
        pLivingEntity: T,
        pCamera: Frustum,
        pCamX: Double,
        pCamY: Double,
        pCamZ: Double
    ): Boolean {
        return true
    }

    override fun render(
        entity: T,
        yaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        if (entity is FastThrowableProjectile) {
            if (entity.syncedTick <= entity.getHiddenTicks()) return
        } else {
            if (entity.tickCount <= entity.getHiddenTicks()) return
        }

        val model = ProjectileModelReloadListener.getModel(getModelLocation(entity)) ?: return

        poseStack.pushPose()

        poseStack.translate(0f, entity.bbHeight / 2, 0f)

        // 十分鬼畜而神秘的写法，直接用yaw的话会导致弹体在+-180°偏航时抽搐，遂采用这种脱裤子放屁的写法
        poseStack.mulPose(Axis.YP.rotationDegrees(VehicleVecUtils.getYRotFromVector(entity.lookAngle).toFloat()))
        poseStack.mulPose(Axis.XP.rotationDegrees(-VehicleVecUtils.getXRotFromVector(entity.lookAngle).toFloat() + 180f))
        poseStack.mulPose(Axis.ZP.rotationDegrees(180f))

        if (entity.getAnimationInstance() != null) {
            val ani = entity.getAnimationInstance()!!
            ani.context.partialTick = partialTick
            ani.tick()
            model.applyPose(BLENDER.blend(model.bindPose, ani.getPose()))
        }

        val flare = model.getBone("flare")
        val flag = flare != null
        if (flag) {
            flare.visible = false
        }

        model.renderToBuffer(
            poseStack,
            buffer,
            RenderType.entityCutout(getTextureLocation(entity)),
            BedrockModelRenderTypes.polyMeshCutout(getTextureLocation(entity)),
            packedLight,
            OverlayTexture.NO_OVERLAY
        )

        val texture = entity.getEmissiveTexture()
        if (texture != null) {
            model.renderToBuffer(
                poseStack,
                buffer,
                RenderType.entityCutout(getTextureLocation(entity)),
                BedrockModelRenderTypes.polyMeshCutout(getTextureLocation(entity)),
                packedLight,
                OverlayTexture.NO_OVERLAY
            )
        }

        val flag2 = if (entity is FastThrowableProjectile) {
            entity.syncedTick > entity.getFlareHiddenTicks()
        } else {
            entity.tickCount > entity.getFlareHiddenTicks()
        }

        if (flag && flag2) {
            flare.visible = true
            flare.rotation.rotationZ(2.5f * (Math.random().toFloat() - 0.5f))
            flare.xScale = ((2 * Math.random() - 1) * 0.4f + 1.6).toFloat()
            flare.yScale = ((2 * Math.random() - 1) * 0.4f + 1.6).toFloat()
            flare.zScale = ((2 * Math.random() - 1) * 0.4f + 1.6).toFloat()
            flare.render(
                poseStack,
                buffer.getBuffer(RenderType.eyes(FLARE_TEXTURE)),
                packedLight,
                OverlayTexture.NO_OVERLAY
            )
        }

        poseStack.popPose()
    }

    companion object {
        val BLENDER: EulerAdditiveBlender = SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }
        val FLARE_TEXTURE = loc("textures/bedrock/projectile/flare.png")
    }
}