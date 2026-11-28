package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.compat.shader.ShaderCompat
import com.atsuishio.superbwarfare.entity.projectile.SwordWaveEntity
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexSorting
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
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
        if (ShaderCompat.isRenderingShadowPass()) return

        if (ShaderCompat.isShaderPackActive()) {
            if (postCompositeWaves.isEmpty()) {
                worldProjection.set(RenderSystem.getProjectionMatrix())
                worldModelView.set(RenderSystem.getModelViewMatrix())
            }
            val pose = PoseStack().last()
            pose.pose().set(poseStack.last().pose())
            pose.normal().set(poseStack.last().normal())
            postCompositeWaves.add(WaveDraw(this, entity, partialTick, pose))
        } else {
            renderWave(entity, partialTick, poseStack.last(), buffers)
        }
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight)
    }

    fun renderWave(
        entity: SwordWaveEntity,
        partialTick: Float,
        pose: PoseStack.Pose,
        buffers: MultiBufferSource
    ) {
        val age = maxOf(entity.tickCount, entity.syncedTick) + partialTick
        val lifetimeOpacity = Mth.clampedLerp(1f, MIN_LIFETIME_OPACITY, age / entity.getLife().coerceAtLeast(1))
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

        val horizontalExtent = textureRight.scale(HALF_SIZE)
        val verticalExtent = rolledUp.scale(HALF_SIZE)
        val bottomLeft = horizontalExtent.add(verticalExtent).scale(-1.0)
        val bottomRight = horizontalExtent.subtract(verticalExtent)
        val topRight = bottomLeft.scale(-1.0)
        val topLeft = bottomRight.scale(-1.0)
        fun layer(type: RenderType, color: Int, alpha: Int) {
            val buffer = buffers.getBuffer(type)
            buffer.waveVertex(pose, bottomLeft, normal, color, alpha, 0f, 1f)
            buffer.waveVertex(pose, bottomRight, normal, color, alpha, 1f, 1f)
            buffer.waveVertex(pose, topRight, normal, color, alpha, 1f, 0f)
            buffer.waveVertex(pose, topLeft, normal, color, alpha, 0f, 0f)
        }

        layer(ModRenderTypes.SWORD_WAVE.apply(INNER_TEXTURE), entity.rgb, (225 * lifetimeOpacity).roundToInt())
        layer(ModRenderTypes.SWORD_WAVE_RIM.apply(getTextureLocation(entity)), 0xFFFFFF, (255 * lifetimeOpacity).roundToInt())
        renderLaunchEdge(entity, partialTick, pose, buffers, forward, rolledUp, normal, lifetimeOpacity)
    }

    private fun renderLaunchEdge(
        entity: SwordWaveEntity,
        partialTick: Float,
        pose: PoseStack.Pose,
        buffers: MultiBufferSource,
        forward: Vec3,
        span: Vec3,
        normal: Vec3,
        lifetimeOpacity: Float,
    ) {
        val viewer = mc.player ?: return
        if (!mc.options.cameraType.isFirstPerson || mc.cameraEntity !== viewer || entity.launchOwnerId != viewer.id) return

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
        val viewAlpha = (0.85 * nearFade * behindFade * edgeFade).toFloat()
        if (viewAlpha <= 0.01f) return
        val alpha = viewAlpha * lifetimeOpacity

        // The extra ribbon lies in the plane across the flight axis and shares the crescent's roll.
        val buffer = buffers.getBuffer(ModRenderTypes.SWORD_WAVE.apply(EDGE_TEXTURE))
        val color = entity.rgb
        val edgeCenter = forward.scale(HALF_SIZE * (1.0 - 2.0 * LAUNCH_EDGE_U))
        fun vertex(slice: EdgeSlice, side: Int) {
            val position = span.scale(slice.height).add(normal.scale(slice.halfWidth * side))
                .scale(SIZE_SCALE).add(edgeCenter)
            val opacity = if (side == 0) (255 * alpha * slice.opacity).roundToInt() else 0
            buffer.waveVertex(pose, position, forward, color, opacity, 0.5f, 0.5f)
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

    private fun VertexConsumer.waveVertex(
        pose: PoseStack.Pose,
        position: Vec3,
        normal: Vec3,
        color: Int,
        alpha: Int,
        u: Float,
        v: Float,
    ) {
        vertex(pose.pose(), position.x.toFloat(), position.y.toFloat(), position.z.toFloat())
            .color((color ushr 16) and 255, (color ushr 8) and 255, color and 255, alpha)
            .uv(u, v).uv2(LightTexture.FULL_BRIGHT)
            .normal(pose.normal(), normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat())
            .endVertex()
    }

    override fun getTextureLocation(entity: SwordWaveEntity) = OUTER_TEXTURE

    private data class EdgeSlice(val height: Double, val halfWidth: Double, val opacity: Float)

    private data class WaveDraw(
        val renderer: SwordWaveRenderer, val entity: SwordWaveEntity, val partialTick: Float, val pose: PoseStack.Pose,
    )

    companion object {
        private const val SIZE_SCALE = 1.4
        private const val HALF_SIZE = 0.64 * SIZE_SCALE
        private const val MIN_LIFETIME_OPACITY = 0.2f
        private const val LAUNCH_EDGE_U = 0.08 // Bright portion of the crescent near its leading edge.
        private val OUTER_TEXTURE = loc("textures/entity/sword_wave_outer.png")
        private val INNER_TEXTURE = loc("textures/entity/sword_wave_inner.png")
        private val EDGE_TEXTURE = loc("textures/entity/white.png")
        private val EDGE_SLICES = arrayOf(
            EdgeSlice(-0.55, 0.02, 0f),
            EdgeSlice(-0.30, 0.075, 1f),
            EdgeSlice(0.30, 0.075, 1f),
            EdgeSlice(0.55, 0.02, 0f),
        )

        private val postCompositeWaves = ArrayList<WaveDraw>()
        private val worldProjection = Matrix4f()
        private val worldModelView = Matrix4f()
        private val postCompositeBuffers = MultiBufferSource.immediate(BufferBuilder(256))

        @JvmStatic
        fun beginFrame() {
            postCompositeWaves.clear()
        }

        /** Iris has restored the main target with scene depth. Blend over its finished colour. */
        @JvmStatic
        fun renderAfterComposite() {
            if (postCompositeWaves.isEmpty()) return
            val previousProjection = RenderSystem.getProjectionMatrix()
            val previousSorting = RenderSystem.getVertexSorting()
            val previousShader = RenderSystem.getShader()
            val previousTexture = RenderSystem.getShaderTexture(0)
            val modelView = RenderSystem.getModelViewStack()
            val camera = mc.gameRenderer.mainCamera.position
            modelView.pushPose()
            try {
                modelView.last().pose().set(worldModelView)
                RenderSystem.applyModelViewMatrix()
                RenderSystem.setProjectionMatrix(worldProjection, VertexSorting.DISTANCE_TO_ORIGIN)
                postCompositeWaves.sortByDescending { it.entity.distanceToSqr(camera) }
                for ((renderer, entity, partialTick, pose) in postCompositeWaves) {
                    renderer.renderWave(entity, partialTick, pose, postCompositeBuffers)
                }
                postCompositeBuffers.endBatch()
            } finally {
                beginFrame()
                modelView.popPose()
                RenderSystem.applyModelViewMatrix()
                RenderSystem.setProjectionMatrix(previousProjection, previousSorting)
                RenderSystem.setShader { previousShader }
                RenderSystem.setShaderTexture(0, previousTexture)
            }
        }
    }
}
