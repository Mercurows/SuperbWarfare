package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.SwordWaveEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

open class SwordWaveRenderer(context: EntityRendererProvider.Context) : EntityRenderer<SwordWaveEntity>(context) {
    override fun render(
        entity: SwordWaveEntity,
        yaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        buffers: MultiBufferSource,
        packedLight: Int,
    ) {
        val forward = if (entity.deltaMovement.lengthSqr() > 1e-6) entity.deltaMovement.normalize()
        else Vec3.directionFromRotation(entity.xRot, entity.yRot)
        val horizontal = forward.cross(Vec3(0.0, 1.0, 0.0))
        val right = if (horizontal.lengthSqr() > 1e-5) {
            horizontal.normalize()
        } else {
            val launchRight = Vec3.directionFromRotation(0f, entity.yRot + 90f)
            launchRight.subtract(forward.scale(launchRight.dot(forward))).normalize()
        }
        val up = right.cross(forward).normalize()
        val radians = Math.toRadians(entity.roll.toDouble())
        val rolledUp = right.scale(sin(radians)).add(up.scale(cos(radians)))
        // The crescent bulges toward low U, so the texture's positive X axis points backward.
        val textureRight = forward.scale(-1.0)
        val normal = textureRight.cross(rolledUp)

        val matrix = poseStack.last()
        val buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE))
        val color = entity.rgb
        fun vertex(x: Double, y: Double, u: Float, v: Float) {
            val position = textureRight.scale(x).add(rolledUp.scale(y))
            buffer.vertex(matrix.pose(), position.x.toFloat(), position.y.toFloat(), position.z.toFloat())
                .color((color ushr 16) and 255, (color ushr 8) and 255, color and 255, 225)
                .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(matrix.normal(), normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat()).endVertex()
        }

        vertex(-0.64, -0.64, 0f, 1f)
        vertex(0.64, -0.64, 1f, 1f)
        vertex(0.64, 0.64, 1f, 0f)
        vertex(-0.64, 0.64, 0f, 0f)
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight)
    }

    override fun getTextureLocation(entity: SwordWaveEntity) = TEXTURE

    companion object {
        private val TEXTURE = loc("textures/entity/projectile/sword_wave.png")
    }
}
