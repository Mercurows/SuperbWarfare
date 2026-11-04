package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.ClientRenderHandler
import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.client.renderer.gun.GunRenderContext
import com.atsuishio.superbwarfare.compat.shader.ShaderCompat
import com.atsuishio.superbwarfare.entity.projectile.LaserBeamEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.tools.BedrockBoneCoordinateTool
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

open class LaserBeamRenderer<T : LaserBeamEntity>(context: EntityRendererProvider.Context) : EntityRenderer<T>(context) {
    private class VisualRay(val start: Vec3, val end: Vec3)
    private val rays = WeakHashMap<T, VisualRay>()

    override fun getTextureLocation(entity: T) = loc("textures/entity/white.png")

    // The origin may be outside the camera frustum while the far end of the beam is visible.
    override fun shouldRender(entity: T, camera: Frustum, x: Double, y: Double, z: Double) = true

    override fun render(
        entity: T, entityYaw: Float, partialTicks: Float,
        pose: PoseStack, buffers: MultiBufferSource, packedLight: Int,
    ) {
        if (ShaderCompat.isRenderingShadowPass()) return
        val age = (entity.level().gameTime - entity.startTime + partialTicks.toDouble()).coerceAtLeast(0.0)
        val progress = (age / entity.getLife()).coerceIn(0.0, 1.0)
        if (progress >= 1.0) return
        val shooter = (entity.owner ?: entity.level().getEntity(entity.ownerId)) as? LivingEntity
        val sampled = shooter?.let { muzzles[it] }?.takeIf {
            it.firstPerson == (shooter === mc.player && mc.options.cameraType.isFirstPerson)
                    && System.nanoTime() - it.time < 250_000_000L
        }
        val cached = rays[entity]
        // 采不到枪口样本时（开枪者这一帧没跑持枪渲染），起点退回**眼位 + 枪口偏移**：
        // `bulletRenderOffset` 在这条路径上是当**固定的初始位置**用的，不做淡出
        // （见 [ClientRenderHandler.muzzleOffsetFor]）—— 整根光束只有十来 tick 寿命，
        // 中途把起点挪回眼位就是让线横移、落点跟着爬，比起点略偏还难看。
        val muzzleOffset = ClientRenderHandler.muzzleOffsetFor(entity)
        val origin = cached?.start ?: sampled?.position ?: muzzleOffset?.let { entity.position().add(it) } ?: run {
            // Allow the gun render pass to provide its animated locator before choosing a fallback.
            if (entity.tickCount < 2) return
            entity.position()
        }
        val end = cached?.end ?: entity.level().clip(
            ClipContext(origin, entity.beamEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)
        ).location
        // 起点一确定就钉死：样本是枪口，偏移也按枪口算，两者都够格当"这一发的初始位置"。
        // 只有"什么都没有、退回眼位"那条不缓存，好让后面采到样本时还能纠回来。
        if (cached == null && (sampled != null || muzzleOffset != null)) rays[entity] = VisualRay(origin, end)
        val vector = end.subtract(origin)
        val length = vector.length()
        if (length < 0.001) return
        val direction = vector.scale(1.0 / length)
        val reference = if (kotlin.math.abs(direction.y) > 0.95) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)
        val right = direction.cross(reference).normalize()
        val up = right.cross(direction).normalize()
        val fade = ((1.0 - progress) * 1.8).coerceAtMost(1.0).toFloat()
        val radius = entity.beamRadius * (1.0 - progress * 0.3)
        pose.pushPose()
        val offset = origin.subtract(entity.getPosition(partialTicks))
        pose.translate(offset.x, offset.y, offset.z)
        val matrix = pose.last().pose()
        // Frames must remain visible from behind; overlapping glow must not occlude itself.
        val buffer = buffers.getBuffer(ModRenderTypes.REFORGING_GLOW)

        // Constant vertex count, regardless of range; no per-block particles or persistent emitters.
        val rgb = entity.getRGB()
        val color = Vec3(rgb[0].toDouble(), rgb[1].toDouble(), rgb[2].toDouble())
        val r = rgb[0]
        val g = rgb[1]
        val b = rgb[2]
        tube(buffer, matrix, direction, right, up, length, radius * 2.5, r, g, b, fade * 0.1f)
        tube(buffer, matrix, direction, right, up, length, radius, r, g, b, fade * 0.65f)
        tube(buffer, matrix, direction, right, up, length, radius * 0.3,
            0.8f + r * 0.2f, 0.8f + g * 0.2f, 0.8f + b * 0.2f, fade)
        val cameraRotation = mc.gameRenderer.mainCamera.rotation()
        val cameraRight = Vector3f(1f, 0f, 0f).rotate(cameraRotation)
        val cameraUp = Vector3f(0f, 1f, 0f).rotate(cameraRotation)
        renderEffects(entity, buffer, BeamFrame(matrix, direction, right, up, length, radius, age, progress, fade, color,
            Vec3(cameraRight.x.toDouble(), cameraRight.y.toDouble(), cameraRight.z.toDouble()),
            Vec3(cameraUp.x.toDouble(), cameraUp.y.toDouble(), cameraUp.z.toDouble())))
        pose.popPose()
        super.render(entity, entityYaw, partialTicks, pose, buffers, packedLight)
    }

    protected class BeamFrame(
        val matrix: Matrix4f, val direction: Vec3, val right: Vec3, val up: Vec3,
        val length: Double, val radius: Double, val age: Double, val progress: Double, val fade: Float,
        val color: Vec3,
        val cameraRight: Vec3, val cameraUp: Vec3,
    )

    protected open fun renderEffects(entity: T, buffer: VertexConsumer, beam: BeamFrame) {}

    protected fun square(
        buffer: VertexConsumer, matrix: Matrix4f, center: Vec3, right: Vec3, up: Vec3,
        size: Double, angle: Double, width: Double, alpha: Float, color: Vec3,
    ) {
        var previous: Vec3? = null
        for (corner in 0..4) {
            val theta = angle + corner * PI / 2
            val point = center.add(right.scale(cos(theta) * size)).add(up.scale(sin(theta) * size))
            previous?.let {
                ribbon(buffer, matrix, it, point, width,
                    color.x.toFloat(), color.y.toFloat(), color.z.toFloat(), alpha)
            }
            previous = point
        }
    }

    private fun tube(
        buffer: VertexConsumer, matrix: Matrix4f, direction: Vec3, right: Vec3, up: Vec3,
        length: Double, radius: Double, r: Float, g: Float, b: Float, alpha: Float,
    ) {
        val end = direction.scale(length)
        for (side in 0 until 8) {
            val angle = side * PI / 4
            val next = (side + 1) * PI / 4
            val a = right.scale(cos(angle) * radius).add(up.scale(sin(angle) * radius))
            val c = right.scale(cos(next) * radius).add(up.scale(sin(next) * radius))
            vertex(buffer, matrix, a, r, g, b, alpha)
            vertex(buffer, matrix, a.add(end), r, g, b, alpha)
            vertex(buffer, matrix, c.add(end), r, g, b, alpha)
            vertex(buffer, matrix, c, r, g, b, alpha)
        }
    }

    protected fun ribbon(
        buffer: VertexConsumer, matrix: Matrix4f, start: Vec3, end: Vec3,
        width: Double, r: Float, g: Float, b: Float, alpha: Float,
    ) {
        val direction = end.subtract(start).normalize()
        val reference = if (kotlin.math.abs(direction.y) > 0.95) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)
        val side = direction.cross(reference).normalize().scale(width)
        val up = direction.cross(side).normalize().scale(width)
        for (offset in arrayOf(side, up)) {
            vertex(buffer, matrix, start.add(offset), r, g, b, alpha)
            vertex(buffer, matrix, end.add(offset), r, g, b, alpha)
            vertex(buffer, matrix, end.subtract(offset), r, g, b, alpha)
            vertex(buffer, matrix, start.subtract(offset), r, g, b, alpha)
        }
    }

    protected fun vertex(buffer: VertexConsumer, matrix: Matrix4f, point: Vec3, r: Float, g: Float, b: Float, alpha: Float) {
        buffer.vertex(matrix, point.x.toFloat(), point.y.toFloat(), point.z.toFloat()).color(r, g, b, alpha).endVertex()
    }

    companion object {
        private class Muzzle(val position: Vec3, val firstPerson: Boolean, val time: Long = System.nanoTime())
        private val muzzles = WeakHashMap<LivingEntity, Muzzle>()

        /**
         * Sample the actual animated muzzle_pos, including the holder's first/third-person item transforms.
         *
         * [tailBackBone] 不为空时只把尾端的 Z（朝相机方向的深度）退到那根骨骼上，X/Y 仍是枪口：
         * 开镜（Scope 类型）时枪身前半截被模板剔除，光束尾端本来会悬在镜内视野正中间，
         * 退到瞄具挂点后看上去就是「从镜筒里射出」。
         */
        fun captureMuzzle(pose: PoseStack, model: GeoGunModel, context: ItemDisplayContext, tailBackBone: String? = null) {
            if (ShaderCompat.isRenderingShadowPass()) return
            val firstPerson = context.firstPerson()
            val holder = if (firstPerson) mc.player else GunRenderContext.entity()
            if (holder == null || (!firstPerson && !GunRenderContext.isMainArm())) return
            if (!firstPerson && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) return
            val bone = model.getGlobalTransform("muzzle_pos") ?: return
            val matrix = pose.last().pose()
            val point = Matrix4f(matrix).mul(bone).getTranslation(Vector3f())
            if (tailBackBone != null) {
                model.getGlobalTransform(tailBackBone)?.let {
                    point.z = Matrix4f(matrix).mul(it).getTranslation(Vector3f()).z
                }
            }
            val view = Vec3(point.x.toDouble(), point.y.toDouble(), point.z.toDouble())
            val camera = mc.gameRenderer.mainCamera
            val position = if (firstPerson) BedrockBoneCoordinateTool.viewPointToWorld(
                view, camera, ClientEventHandler.handFov.toFloat(), ClientEventHandler.fov.toFloat()
            ) else BedrockBoneCoordinateTool.viewPointToWorld(view, camera)
            muzzles[holder] = Muzzle(position, firstPerson)
        }
    }
}
