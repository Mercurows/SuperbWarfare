package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.compat.shader.ShaderCompat
import com.atsuishio.superbwarfare.entity.projectile.SwordWaveEntity
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
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

        val matrix = poseStack.last().pose()
        val buffer = buffers.getBuffer(ModRenderTypes.SWORD_WAVE.apply(getTextureLocation(entity)))
        val color = entity.rgb
        fun vertex(x: Double, y: Double, u: Float, v: Float) {
            val position = textureRight.scale(x).add(rolledUp.scale(y))
            buffer.vertex(matrix, position.x.toFloat(), position.y.toFloat(), position.z.toFloat())
                .color((color ushr 16) and 255, (color ushr 8) and 255, color and 255, 225)
                .uv(u, v).endVertex()
        }

        vertex(-HALF_SIZE, -HALF_SIZE, 0f, 1f)
        vertex(HALF_SIZE, -HALF_SIZE, 1f, 1f)
        vertex(HALF_SIZE, HALF_SIZE, 1f, 0f)
        vertex(-HALF_SIZE, HALF_SIZE, 0f, 0f)
        renderLaunchEdge(entity, partialTick, poseStack, buffers, forward, rolledUp, normal)
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight)
    }

    private fun renderLaunchEdge(
        entity: SwordWaveEntity,
        partialTick: Float,
        poseStack: PoseStack,
        buffers: MultiBufferSource,
        forward: Vec3,
        span: Vec3,
        normal: Vec3,
    ) {
        val viewer = mc.player ?: return
        if (!mc.options.cameraType.isFirstPerson || mc.cameraEntity !== viewer || entity.launchOwnerId != viewer.id ||
            ShaderCompat.isRenderingShadowPass()
        ) return

        val t = partialTick.toDouble()
        val toCamera = mc.gameRenderer.mainCamera.position.subtract(
            Mth.lerp(t, entity.xOld, entity.x),
            Mth.lerp(t, entity.yOld, entity.y),
            Mth.lerp(t, entity.zOld, entity.z),
        )
        val distance = toCamera.length()
        if (distance <= 0.55) return
        val view = toCamera.scale(1.0 / distance)
        val nearFade = ((distance - 0.55) / 0.8).coerceAtMost(1.0)
        val behindFade = ((-forward.dot(view) - 0.75) / 0.25).coerceIn(0.0, 1.0)
        val edgeFade = (1.0 - abs(normal.dot(view)) / 0.28).coerceAtLeast(0.0)
        val alpha = (0.85 * nearFade * behindFade * edgeFade).toFloat()
        if (alpha <= 0.01f) return

        // The extra ribbon lies in the plane across the flight axis and shares the crescent's roll.
        val matrix = poseStack.last().pose()
        val buffer = buffers.getBuffer(ModRenderTypes.BLADE_TRAIL)
        val color = entity.rgb
        val red = ((color ushr 16) and 255) / 255f
        val green = ((color ushr 8) and 255) / 255f
        val blue = (color and 255) / 255f
        val edgeCenter = forward.scale(HALF_SIZE * (1.0 - 2.0 * LAUNCH_EDGE_U))
        fun vertex(slice: EdgeSlice, side: Int) {
            val position = span.scale(slice.height).add(normal.scale(slice.halfWidth * side))
                .scale(SIZE_SCALE).add(edgeCenter)
            val opacity = if (side == 0) alpha * slice.opacity else 0f
            buffer.vertex(matrix, position.x.toFloat(), position.y.toFloat(), position.z.toFloat())
                .color(red, green, blue, opacity).endVertex()
        }

        for (index in 1 until EDGE_SLICES.size) {
            val bottom = EDGE_SLICES[index - 1]
            val top = EDGE_SLICES[index]
            vertex(bottom, -1)
            vertex(bottom, 0)
            vertex(top, 0)
            vertex(top, -1)
            vertex(bottom, 0)
            vertex(bottom, 1)
            vertex(top, 1)
            vertex(top, 0)
        }
    }

    override fun getTextureLocation(entity: SwordWaveEntity) = TEXTURE

    private data class EdgeSlice(val height: Double, val halfWidth: Double, val opacity: Float)

    companion object {
        private const val SIZE_SCALE = 1.4
        private const val HALF_SIZE = 0.64 * SIZE_SCALE
        private const val LAUNCH_EDGE_U = 0.08 // Bright portion of the crescent near its leading edge.
        private val TEXTURE = loc("textures/entity/sword_wave.png")
        private val EDGE_SLICES = arrayOf(
            EdgeSlice(-0.55, 0.02, 0f),
            EdgeSlice(-0.30, 0.075, 1f),
            EdgeSlice(0.30, 0.075, 1f),
            EdgeSlice(0.55, 0.02, 0f),
        )
    }
}
