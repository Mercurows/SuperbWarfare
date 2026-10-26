package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.living.CreepingSenpaiEntity
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.maydaymemory.mae.basic.ArrayPoseBuilder
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory
import com.maydaymemory.mae.blend.EulerAdditiveBlender
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import org.joml.Matrix3f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

class CreepingSenpaiRenderer(renderManager: EntityRendererProvider.Context) :
    EntityRenderer<CreepingSenpaiEntity>(renderManager) {
    private val surfacePoses = WeakHashMap<CreepingSenpaiEntity, SurfacePose>()

    private class SurfacePose(val rotation: Quaternionf, val offset: Vector3f, var time: Float)

    init {
        this.shadowRadius = 0.5f
    }

    override fun getTextureLocation(pEntity: CreepingSenpaiEntity): ResourceLocation {
        return TEXTURE
    }

    override fun render(
        pEntity: CreepingSenpaiEntity,
        pEntityYaw: Float,
        pPartialTick: Float,
        pPoseStack: PoseStack,
        pBuffer: MultiBufferSource,
        pPackedLight: Int
    ) {
        val ani = pEntity.animationInstance ?: return
        val instance = pEntity.modelInstance ?: return

        val normal = pEntity.attachedFace
        val heading = Math.toRadians(pEntity.surfaceHeading.toDouble()).toFloat()
        val up = Vector3f(normal.stepX.toFloat(), normal.stepY.toFloat(), normal.stepZ.toFloat())
        val forward = if (normal.axis.isHorizontal) Vector3f(0f, 1f, 0f) else Vector3f(0f, 0f, 1f)
        val right = Vector3f(forward).cross(up)
        forward.mul(cos(heading)).add(Vector3f(right).mul(sin(heading)))
        right.set(forward).cross(up)
        val rotation = Quaternionf().setFromNormalized(Matrix3f(
            right.x, right.y, right.z, up.x, up.y, up.z, -forward.x, -forward.y, -forward.z
        ))
        val extent = if (normal.axis.isHorizontal) pEntity.bbWidth / 2f else pEntity.bbHeight / 2f
        val offset = Vector3f(up).mul(-extent).add(0f, pEntity.bbHeight / 2f, 0f)
        val now = pEntity.tickCount + pPartialTick
        val pose = surfacePoses.getOrPut(pEntity) { SurfacePose(Quaternionf(rotation), Vector3f(offset), now) }
        val blend = 1f - exp(-(now - pose.time).coerceIn(0f, 10f) / 2f)
        pose.rotation.slerp(rotation, blend)
        pose.offset.lerp(offset, blend)
        pose.time = now

        pPoseStack.pushPose()
        pPoseStack.translate(pose.offset.x.toDouble(), pose.offset.y.toDouble(), pose.offset.z.toDouble())
        pPoseStack.mulPose(pose.rotation)

        ani.context.partialTick = pPartialTick
        ani.tick()
        instance.applyPose(BLENDER.blend(instance.bindPose, ani.getPose()))

        instance.renderToBuffer(
            pPoseStack,
            pBuffer,
            RenderType.entityCutout(getTextureLocation(pEntity)),
            BedrockModelRenderTypes.polyMeshCutout(getTextureLocation(pEntity)),
            pPackedLight,
            OverlayTexture.pack(0f, pEntity.hurtTime > 0 || pEntity.deathTime > 0)
        )
        pPoseStack.popPose()
    }

    companion object {
        var TEXTURE = loc("textures/bedrock/entity/creeping_senpai.png")
        val BLENDER: EulerAdditiveBlender = SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }
    }
}
