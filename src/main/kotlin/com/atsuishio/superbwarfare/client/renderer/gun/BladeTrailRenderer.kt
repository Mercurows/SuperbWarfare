package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.client.animation.gun.GeoGunAnimationInstance
import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.compat.shader.ShaderCompat
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.resource.gun.pojo.BladeTrailInfo
import com.atsuishio.superbwarfare.tools.localPlayer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import org.lwjgl.system.MemoryStack
import kotlin.math.max
import kotlin.math.min

object BladeTrailRenderer {
    private const val LIFETIME_NS = 120_000_000L
    private const val MAX_SAMPLES = 32
    private val defaultRgb = floatArrayOf(0.90f, 0.95f, 1f)

    private data class Sample(
        val swing: Int,
        val seconds: Float,
        val time: Long,
        val base: Vector3f,
        val tip: Vector3f,
        val rgb: FloatArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Sample

            if (swing != other.swing) return false
            if (seconds != other.seconds) return false
            if (time != other.time) return false
            if (base != other.base) return false
            if (tip != other.tip) return false
            if (!rgb.contentEquals(other.rgb)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = swing
            result = 31 * result + seconds.hashCode()
            result = 31 * result + time.hashCode()
            result = 31 * result + base.hashCode()
            result = 31 * result + tip.hashCode()
            result = 31 * result + rgb.contentHashCode()
            return result
        }
    }

    private var activeAnimation: GeoGunAnimationInstance? = null
    private var activeConfig: BladeTrailInfo? = null
    private var previous: Sample? = null
    private val samples = ArrayDeque<Sample>()
    private data class HandView(
        val projection: Matrix4f, val modelView: Matrix4f, val depthNear: Double, val depthFar: Double, val time: Long,
    )

    private val postCompositeBuffers = MultiBufferSource.immediate(BufferBuilder(2048))
    private var pendingView: HandView? = null
    private var sampledThisFrame = false

    @JvmStatic
    fun beginFrame() {
        pendingView = null
        sampledThisFrame = false
    }

    fun render(
        config: BladeTrailInfo?,
        animation: GeoGunAnimationInstance?,
        stack: ItemStack,
        model: GeoGunModel,
        poseStack: PoseStack,
        buffers: MultiBufferSource,
    ) {
        if (activeAnimation !== animation || activeConfig !== config) {
            previous = null
            samples.clear()
            activeAnimation = animation.takeIf { config != null }
            activeConfig = config
        }
        if (config == null || animation == null) return

        val held = localPlayer?.mainHandItem ?: return
        val data = GunData.from(stack)
        if (held.item != stack.item || (held !== stack && GunData.from(held).uuid != data.uuid)) {
            previous = null
            samples.clear()
            return
        }

        val defer = ShaderCompat.isShaderPackActive()
        if (defer && sampledThisFrame) return
        sampledThisFrame = defer
        val now = System.nanoTime()
        while (samples.firstOrNull()?.let { now - it.time >= LIFETIME_NS } == true) {
            samples.removeFirst()
        }

        val playback = animation.meleePlayback()
        val window = playback?.let { config.windows[it.clip] }
        val bone = if (window != null) model.getGlobalTransform(config.bone) else null
        if (playback != null && window != null && bone != null) {
            // Store the points in hand view space so old samples don't follow the current blade pose.
            val transform = Matrix4f(poseStack.last().pose()).mul(bone)
            val rgb = data.perk.get(Perk.Type.AMMO)?.ammoRgb ?: defaultRgb
            record(
                Sample(
                    playback.swing,
                    playback.seconds,
                    now,
                    point(transform, config.base),
                    point(transform, config.tip),
                    rgb
                ), window
            )
        } else {
            previous = null
        }

        if (samples.size < 2) return
        if (defer) {
            // Oculus compresses hand depth through its projection. Replay the same hand view.
            MemoryStack.stackPush().use { memory ->
                val depthRange = memory.mallocDouble(2)
                GL11.glGetDoublev(GL11.GL_DEPTH_RANGE, depthRange)
                pendingView = HandView(
                    Matrix4f(RenderSystem.getProjectionMatrix()), Matrix4f(RenderSystem.getModelViewMatrix()),
                    depthRange[0], depthRange[1], now,
                )
            }
        } else {
            drawTrail(buffers, now)
        }
    }

    @JvmStatic
    fun renderAfterComposite() {
        val view = pendingView ?: return
        pendingView = null
        val projection = RenderSystem.getProjectionMatrix()
        val sorting = RenderSystem.getVertexSorting()
        val shader = RenderSystem.getShader()
        val modelView = RenderSystem.getModelViewStack()
        MemoryStack.stackPush().use { memory ->
            val depthRange = memory.mallocDouble(2)
            GL11.glGetDoublev(GL11.GL_DEPTH_RANGE, depthRange)
            modelView.pushPose()
            try {
                modelView.last().pose().set(view.modelView)
                RenderSystem.applyModelViewMatrix()
                RenderSystem.setProjectionMatrix(view.projection, sorting)
                GL11.glDepthRange(view.depthNear, view.depthFar)
                drawTrail(postCompositeBuffers, view.time)
                postCompositeBuffers.endBatch(ModRenderTypes.BLADE_TRAIL)
            } finally {
                GL11.glDepthRange(depthRange[0], depthRange[1])
                modelView.popPose()
                RenderSystem.applyModelViewMatrix()
                RenderSystem.setProjectionMatrix(projection, sorting)
                RenderSystem.setShader { shader }
            }
        }
    }

    private fun drawTrail(buffers: MultiBufferSource, now: Long) {
        val vertices = buffers.getBuffer(ModRenderTypes.BLADE_TRAIL)
        var older = samples.first()
        for (index in 1 until samples.size) {
            val newer = samples[index]
            if (older.swing == newer.swing) {
                val oldAlpha = opacity(older, now)
                val newAlpha = opacity(newer, now)
                vertex(vertices, older.base, older.rgb, oldAlpha * 0.16f)
                vertex(vertices, older.tip, older.rgb, oldAlpha)
                vertex(vertices, newer.tip, newer.rgb, newAlpha)
                vertex(vertices, newer.base, newer.rgb, newAlpha * 0.16f)
            }
            older = newer
        }
    }

    private fun record(current: Sample, window: BladeTrailInfo.Window) {
        val older = previous
        previous = current
        if (older == null || older.swing != current.swing || current.time - older.time >= LIFETIME_NS) {
            if (current.seconds in window.start..window.end) add(current)
            return
        }

        val start = max(older.seconds, window.start)
        val end = min(current.seconds, window.end)
        if (start >= end) return
        // A frame can cross a window boundary, particularly on fast swings or at low frame rates.
        if (older.seconds < start || samples.lastOrNull()?.swing != current.swing) {
            add(interpolate(older, current, start))
        }
        add(interpolate(older, current, end))
    }

    private fun interpolate(older: Sample, newer: Sample, seconds: Float): Sample {
        if (seconds == newer.seconds) return newer
        val progress = (seconds - older.seconds) / (newer.seconds - older.seconds)
        return Sample(
            newer.swing, seconds, older.time + ((newer.time - older.time) * progress).toLong(),
            older.base.lerp(newer.base, progress, Vector3f()),
            older.tip.lerp(newer.tip, progress, Vector3f()),
            newer.rgb,
        )
    }

    private fun add(sample: Sample) {
        if (samples.size == MAX_SAMPLES) samples.removeFirst()
        samples.addLast(sample)
    }

    private fun point(transform: Matrix4f, offset: Vector3f): Vector3f =
        transform.transformPosition(-offset.x / 16f, offset.y / 16f, offset.z / 16f, Vector3f())

    private fun opacity(sample: Sample, now: Long): Float =
        (1f - (now - sample.time).toFloat() / LIFETIME_NS) * 0.78f

    private fun vertex(buffer: VertexConsumer, position: Vector3f, rgb: FloatArray, alpha: Float) {
        buffer.vertex(position.x.toDouble(), position.y.toDouble(), position.z.toDouble())
            .color(rgb[0].coerceIn(0f, 1f), rgb[1].coerceIn(0f, 1f), rgb[2].coerceIn(0f, 1f), alpha)
            .endVertex()
    }
}
