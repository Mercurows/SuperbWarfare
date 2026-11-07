package com.atsuishio.superbwarfare.client.renderer.special

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.compat.shader.ShaderCompat
import com.atsuishio.superbwarfare.tools.clientLevel
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.joml.Matrix4f
import java.util.Random
import kotlin.collections.ArrayDeque
import kotlin.collections.indices
import kotlin.collections.removeAll
import kotlin.math.abs

/** Shared seeded lightning geometry and short-lived, world-space discharge effects. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID, value = [Dist.CLIENT])
object LightningArcRenderer {
    private val WHITE_TINT = Vec3(1.0, 1.0, 1.0)
    private val arcRandom = Random()
    private data class Arc(val start: Vec3, val end: Vec3, val seed: Long, val expires: Long)
    private var world: ClientLevel? = null
    private val arcs = ArrayDeque<Arc>()

    @SubscribeEvent
    fun logout(event: ClientPlayerNetworkEvent.LoggingOut) {
        arcs.clear()
        world = null
    }

    fun add(start: Vec3, end: Vec3, seed: Long) {
        val level = clientLevel ?: return
        if (world !== level) {
            arcs.clear()
            world = level
        }
        if (arcs.size >= 256) arcs.removeFirst()
        arcs.addLast(Arc(start, end, seed, level.gameTime + 4))
    }

    @SubscribeEvent
    fun render(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return
        if (ShaderCompat.isRenderingShadowPass()) return
        val level = clientLevel ?: return
        if (world !== level) {
            arcs.clear()
            world = level
        }
        arcs.removeAll { it.expires <= level.gameTime }
        if (arcs.isEmpty()) return
        val pose = event.poseStack
        val camera = event.camera.position
        val buffers = mc.renderBuffers().bufferSource()
        val buffer = buffers.getBuffer(ModRenderTypes.REFORGING_GLOW)
        for (arc in arcs) {
            // Subtract in double precision before vertices enter the float pose matrix.
            // Translating absolute world coordinates in the matrix loses arc position/width far from spawn.
            lightning(buffer, pose.last().pose(), arc.start.subtract(camera), arc.end.subtract(camera), arc.seed,
                ((arc.expires - level.gameTime - event.partialTick) / 4f).coerceIn(0f, 1f))
        }
        // Oculus ignores endBatch(RenderType), even with shader packs disabled. Flush its
        // real batch here, before LevelRenderer changes the model-view matrix for clouds/weather.
        if (!ShaderCompat.endBatch(buffers)) buffers.endBatch(ModRenderTypes.REFORGING_GLOW)
    }

    fun lightning(
        buffer: VertexConsumer, pose: Matrix4f, start: Vec3, end: Vec3, seed: Long, alpha: Float,
        maxSegments: Int = 32, widthScale: Double = 1.0, minSegments: Int = 3, tint: Vec3 = WHITE_TINT,
    ) {
        val offset = end.subtract(start)
        val length = offset.length()
        if (length < 0.01 || alpha <= 0f) return
        val random = arcRandom
        random.setSeed(seed)
        val segments = (length * 8).toInt().coerceIn(minSegments, maxSegments)
        val jitter = (length * 0.06).coerceIn(0.08, 0.35)
        var previous = start
        for (i in 1..segments) {
            val point = if (i == segments) end else start.add(offset.scale(i.toDouble() / segments)).add(
                (random.nextDouble() - 0.5) * jitter * 2,
                (random.nextDouble() - 0.5) * jitter * 2,
                (random.nextDouble() - 0.5) * jitter * 2,
            )
            beam(buffer, pose, previous, point, 0.035 * widthScale,
                (0.2 * tint.x).toFloat(), (0.4 * tint.y).toFloat(), tint.z.toFloat(), alpha * 0.2f)
            beam(buffer, pose, previous, point, 0.009 * widthScale,
                (0.65 * tint.x).toFloat(), (0.85 * tint.y).toFloat(), tint.z.toFloat(), alpha)
            previous = point
        }
    }

    private fun beam(
        buffer: VertexConsumer, pose: Matrix4f, start: Vec3, end: Vec3,
        width: Double, red: Float, green: Float, blue: Float, alpha: Float,
    ) {
        if (alpha <= 0f || width <= 0.0 || start.distanceToSqr(end) < 1.0E-10) return
        val direction = end.subtract(start).normalize()
        val reference = if (abs(direction.y) > 0.9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)
        val side = direction.cross(reference).normalize().scale(width)
        val up = direction.cross(side).normalize().scale(width)
        val offsets = arrayOf(side.add(up), side.subtract(up), side.scale(-1.0).subtract(up), up.subtract(side))
        for (i in offsets.indices) {
            val next = (i + 1) % offsets.size
            vertex(buffer, pose, start.add(offsets[i]), red, green, blue, alpha)
            vertex(buffer, pose, end.add(offsets[i]), red, green, blue, alpha)
            vertex(buffer, pose, end.add(offsets[next]), red, green, blue, alpha)
            vertex(buffer, pose, start.add(offsets[next]), red, green, blue, alpha)
        }
    }

    private fun vertex(
        buffer: VertexConsumer, pose: Matrix4f, point: Vec3,
        red: Float, green: Float, blue: Float, alpha: Float,
    ) {
        buffer.vertex(pose, point.x.toFloat(), point.y.toFloat(), point.z.toFloat())
            .color(red, green, blue, alpha).endVertex()
    }

}
