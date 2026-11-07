package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.ClientRenderHandler
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.client.renderer.special.LightningArcRenderer.lightning
import com.atsuishio.superbwarfare.entity.projectile.ReforgingLightningBallEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ReforgingLightningBallRenderer(context: EntityRendererProvider.Context) :
    EntityRenderer<ReforgingLightningBallEntity>(context) {

    private val arcRandom = Random()

    override fun render(
        entity: ReforgingLightningBallEntity,
        entityYaw: Float,
        partialTicks: Float,
        pose: PoseStack,
        buffers: MultiBufferSource,
        packedLight: Int,
    ) {
        pose.pushPose()

        // 与普通子弹一致：弹体从眼位生成，开火头几 tick 挪到枪口再淡出
        // （见 [ClientRenderHandler.bulletRenderOffset]）。电弧的端点都是相对实体位置算的，
        // 跟着同一份 pose 走才不会和球体分家。
        ClientRenderHandler.transformVirtualRenderPosition(pose, entity, partialTicks)

        val phase = entity.level().gameTime / 2
        val seed = entity.id.toLong() * 31 + phase * 17
        val model = entity.getModelInstance()
        val animation = entity.getAnimationInstance()
        if (model != null && animation != null) {
            animation.context.partialTick = partialTicks
            animation.tick()
            model.applyPose(BasicProjectileRenderer.BLENDER.blend(model.bindPose, animation.getPose()))
            model.getBone("middle_frame")?.visible = false
            val outerFrame = model.getBone("outer_frame")
            val outerVisible = outerFrame?.visible ?: false
            val renderType = RenderType.eyes(TEXTURE)
            try {
                outerFrame?.visible = false
                model.renderToBuffer(
                    pose, buffers.getBuffer(renderType),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
                )
                if (outerVisible) {
                    outerFrame.visible = true
                    // Tint only the outer cage; keep the inner frame and bright core unchanged.
                    model.renderSingleBone(
                        pose, model.getIndex("outer_frame"), buffers, renderType, renderType,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                        OUTER_FRAME_TINT.x.toFloat(), OUTER_FRAME_TINT.y.toFloat(), OUTER_FRAME_TINT.z.toFloat(), 1f, true
                    )
                }
            } finally {
                outerFrame?.visible = outerVisible
            }
            // The thin inner cage and toothed outer cage rotate independently.
            for ((boneName, halfSize) in ELECTRIC_CAGES) {
                val bone = model.getIndex(boneName)
                if (bone < 0) continue
                val transform = Matrix4f(pose.last().pose()).mul(model.getGlobalTransform(bone))
                electrifyCage(buffers.getBuffer(ModRenderTypes.REFORGING_GLOW), transform, halfSize, seed + bone * 101L,
                    phase, if (boneName == "outer_frame") OUTER_FRAME_TINT else WHITE_TINT)
            }
        }

        val buffer = buffers.getBuffer(ModRenderTypes.REFORGING_GLOW)
        ambientElectricity(buffer, pose.last().pose(), entity, partialTicks)

        for (arc in entity.lightningArcs) {
            val end = arc.end.subtract(entity.getPosition(partialTicks))
            lightning(buffer, pose.last().pose(), Vec3.ZERO, end, arc.seed, (arc.ticks - partialTicks) / 4f)
        }
        pose.popPose()
        super.render(entity, entityYaw, partialTicks, pose, buffers, packedLight)
    }

    private fun ambientElectricity(
        buffer: VertexConsumer, pose: Matrix4f, entity: ReforgingLightningBallEntity, partialTicks: Float,
    ) {
        val time = entity.level().gameTime + partialTicks.toDouble()
        // Hold each random shape for a few ticks; never choose new geometry on every rendered frame.
        repeat(7) { index ->
            val clock = (time + index * 1.73) / 4.0
            val generation = kotlin.math.floor(clock).toLong()
            val random = arcRandom
            random.setSeed(entity.id * 7919L + generation * 104729L + index * 31L)
            if (random.nextFloat() < 0.22f) return@repeat
            val direction = randomDirection(random)
            val start = direction.scale(0.5 + random.nextDouble() * 0.35)
            val end = direction.scale(1.0 + random.nextDouble() * 0.65)
                .add(randomDirection(random).scale(0.3))
            val arcSeed = random.nextLong()
            val fade = sin(PI * (clock - generation)).toFloat()
            lightning(buffer, pose, start, end, arcSeed, fade * 0.85f, 9, 0.55, 8)
            if (index % 2 == 0) {
                val branch = start.lerp(end, 0.55)
                lightning(buffer, pose, branch, branch.add(direction.scale(0.25)).add(0.15, 0.2, -0.15),
                    arcSeed + 1, fade * 0.5f, 5, 0.3, 4)
            }
        }
        repeat(24) { index ->
            val clock = (time + index * 3.17) / (12.0 + index % 7)
            val generation = kotlin.math.floor(clock).toLong()
            arcRandom.setSeed(entity.id * 3571L + index * 101L + generation * 65537L)
            val direction = randomDirection(arcRandom)
            val point = direction.scale(0.55 + arcRandom.nextDouble() * 0.95)
                .add(direction.scale((clock - generation) * 0.12))
            val radius = 0.008 + arcRandom.nextDouble() * 0.015
            val fade = sin(PI * (clock - generation)).toFloat()
            sphere(buffer, pose, point, radius * 2.1, 0.25f, 0.65f, 1f, fade * 0.12f)
            sphere(buffer, pose, point, radius, 0.75f, 0.95f, 1f, fade * 0.85f)
        }
    }

    private fun randomDirection(random: Random): Vec3 {
        val y = random.nextDouble() * 2.0 - 1.0
        val angle = random.nextDouble() * PI * 2.0
        val radius = sqrt(1.0 - y * y)
        return Vec3(cos(angle) * radius, y, sin(angle) * radius)
    }

    private fun electrifyCage(
        buffer: VertexConsumer, pose: Matrix4f, halfSize: Double, seed: Long, phase: Long, tint: Vec3,
    ) {
        val corners = CAGE_CORNERS.map { it.scale(halfSize) }
        for (index in CAGE_EDGES.indices) {
            val edge = CAGE_EDGES[index]
            lightning(buffer, pose, corners[edge.first], corners[edge.second], seed + index * 37L, 0.45f, 3, 0.25, tint = tint)
        }
        repeat(3) { faceIndex ->
            val face = Math.floorMod(phase + faceIndex * 2, 6)
            val axis = face / 2
            val start = if (face % 2 == 0) 0 else 1 shl axis
            val end = start xor (7 xor (1 shl axis))
            lightning(buffer, pose, corners[start], corners[end], seed + 1000 + faceIndex, 0.5f, 5, 0.25, tint = tint)
        }
        // A fourth arc bridges the charged shell and its core.
        lightning(buffer, pose, Vec3.ZERO, corners[(phase.toInt() and 7)], seed + 2000, 0.5f, 4, 0.35, tint = tint)
    }

    // Cache unit sphere vertices instead of rebuilding the trigonometry every frame.
    private val sphere = buildList {
        fun point(latitude: Int, longitude: Int): Vec3 {
            val pitch = PI * latitude / 6
            val yaw = PI * longitude / 6
            return Vec3(sin(pitch) * cos(yaw), cos(pitch), sin(pitch) * sin(yaw))
        }
        for (latitude in 0 until 6) for (longitude in 0 until 12) {
            add(point(latitude, longitude))
            add(point(latitude, longitude + 1))
            add(point(latitude + 1, longitude + 1))
            add(point(latitude + 1, longitude))
        }
    }

    private fun sphere(
        buffer: VertexConsumer, pose: Matrix4f, center: Vec3, radius: Double,
        red: Float, green: Float, blue: Float, alpha: Float,
    ) {
        if (radius <= 0.0001 || alpha <= 0f) return
        for (point in sphere) vertex(buffer, pose, center.add(point.scale(radius)), red, green, blue, alpha)
    }

    private fun vertex(
        buffer: VertexConsumer, pose: Matrix4f, point: Vec3,
        red: Float, green: Float, blue: Float, alpha: Float,
    ) {
        buffer.vertex(pose, point.x.toFloat(), point.y.toFloat(), point.z.toFloat())
            .color(red, green, blue, alpha).endVertex()
    }

    override fun getTextureLocation(entity: ReforgingLightningBallEntity) = TEXTURE

    companion object {
        private val TEXTURE = loc("textures/bedrock/projectile/reforging_lightning_ball.png")
        private val WHITE_TINT = Vec3(1.0, 1.0, 1.0)
        private val OUTER_FRAME_TINT = Vec3(0.4, 0.68, 0.95)
        private val ELECTRIC_CAGES = arrayOf("inner_frame" to 0.35, "outer_frame" to 0.5)
        private val CAGE_CORNERS = Array(8) { corner ->
            Vec3(
                if (corner and 1 == 0) -1.0 else 1.0,
                if (corner and 2 == 0) -1.0 else 1.0,
                if (corner and 4 == 0) -1.0 else 1.0
            )
        }
        private val CAGE_EDGES = buildList {
            for (axis in 0..2) {
                val bit = 1 shl axis
                for (corner in 0..7) if (corner and bit == 0) add(corner to (corner xor bit))
            }
        }
    }
}
