package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.C4Entity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Quaternionf

class C4Renderer(renderManager: EntityRendererProvider.Context) : EntityRenderer<C4Entity>(renderManager) {
    init {
        this.shadowRadius = 0f
    }

    override fun render(
        entityIn: C4Entity,
        entityYaw: Float,
        partialTicks: Float,
        poseStack: PoseStack,
        bufferIn: MultiBufferSource,
        packedLightIn: Int
    ) {
        val model = ProjectileModelReloadListener.getModel(MODEL) ?: return

        poseStack.pushPose()
        val q = Quaternionf(entityIn.getQuaternion(partialTicks))
        poseStack.rotateAround(q, 0f, 0f, 0f)

        val renderType = RenderType.entityTranslucent(getTextureLocation(entityIn))
        val vertexConsumer = bufferIn.getBuffer(renderType)

        model.renderToBuffer(
            poseStack,
            vertexConsumer,
            packedLightIn,
            OverlayTexture.NO_OVERLAY
        )

        poseStack.popPose()

        if (this.entityRenderDispatcher.shouldRenderHitBoxes()
            && !entityIn.isInvisible
            && !mc.showOnlyReducedInfo()
        ) {
            val matrix4f = poseStack.last().pose()
            val matrix3f = poseStack.last().normal()
            val buffer = bufferIn.getBuffer(RenderType.lines())

            val frontVec = VehicleVecUtils.getFrontVec(q)
            renderAxis(matrix3f, matrix4f, frontVec, buffer, 0, 0, 255)

            val upVec = VehicleVecUtils.getUpVec(q)
            renderAxis(matrix3f, matrix4f, upVec, buffer, 0, 255, 0)

            val rightVec = VehicleVecUtils.getRightVec(q)
            renderAxis(matrix3f, matrix4f, rightVec, buffer, 255, 0, 0)
        }
    }

    override fun getTextureLocation(entity: C4Entity): ResourceLocation {
        val uuid = entity.getUUID()
        return if (uuid.leastSignificantBits % 114 == 0L) {
            TEXTURE_ALTER
        } else {
            TEXTURE
        }
    }

    private fun renderAxis(
        matrix3f: Matrix3f,
        matrix4f: Matrix4f,
        vec3: Vec3,
        buffer: VertexConsumer,
        r: Int,
        g: Int,
        b: Int
    ) {
        buffer.vertex(matrix4f, 0.0f, 0.125f, 0.0f)
            .color(r, g, b, 255)
            .normal(matrix3f, vec3.x.toFloat(), vec3.y.toFloat(), vec3.z.toFloat())
            .endVertex()
        buffer.vertex(
            matrix4f,
            (vec3.x * 0.5).toFloat(),
            (0.125 + vec3.y * 0.5).toFloat(),
            (vec3.z * 0.5).toFloat()
        ).color(r, g, b, 255)
            .normal(matrix3f, vec3.x.toFloat(), vec3.y.toFloat(), vec3.z.toFloat())
            .endVertex()
    }

    companion object {
        val TEXTURE = loc("textures/bedrock/projectile/c4.png")
        val TEXTURE_ALTER = loc("textures/bedrock/projectile/c4_alter.png")
        val MODEL = loc("models/bedrock/projectile/c4.geo.json")
    }
}
