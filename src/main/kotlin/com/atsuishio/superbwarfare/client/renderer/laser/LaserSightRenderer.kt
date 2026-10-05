package com.atsuishio.superbwarfare.client.renderer.laser

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.client.renderer.laser.LaserSightRenderer.DOT_GROW_START_DISTANCE
import com.atsuishio.superbwarfare.data.attachment.LaserInfo
import com.atsuishio.superbwarfare.tools.BedrockBoneCoordinateTool
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4f
import org.joml.Vector3f
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 在枪械自己的绘制调用里直绘激光束
 */
@OnlyIn(Dist.CLIENT)
object LaserSightRenderer {

    /** 光斑基础半径（米），再按距离放大（见 [DOT_GROW_START_DISTANCE]） */
    private const val DOT_RADIUS = 0.035f

    private const val DOT_GROW_START_DISTANCE = 8.0
    private const val DOT_MAX_GROWTH = 4.0f

    /** 光斑沿光束朝出光口方向退这么远，免得和命中面 z-fighting */
    private const val DOT_SURFACE_OFFSET = 0.01

    /** 白芯的半宽 / 外层的半宽 */
    private const val CORE_WIDTH_RATIO = 0.45

    /** 全亮的光照贴图坐标 */
    private const val FULL_BRIGHT = 0xF000F0

    /** 纯白 16x16，颜色全靠顶点色 */
    private val TEXTURE = loc("textures/entity/white.png")

    /** 渲染本帧采集到的全部激光束 */
    fun render(poseStack: PoseStack, bufferSource: MultiBufferSource) {
        val beams = LaserSightCapture.beams()
        if (beams.isEmpty()) return

        castFirstPersonBeams(beams)

        poseStack.pushPose()
        try {
            val consumer = bufferSource.getBuffer(ModRenderTypes.laserBeam(TEXTURE))
            val pose = poseStack.last().pose()
            for (beam in beams) {
                // 出光口矩阵已经是绝对姿态，直接覆盖；顶点用本地坐标发
                pose.set(beam.emitterMatrix)
                emitBeam(consumer, pose, beam)
            }
        } finally {
            poseStack.popPose()
        }
    }

    private fun castFirstPersonBeams(beams: List<LaserSightCapture.Beam>) {
        val firstPerson = beams.filter { it.firstPerson }
        if (firstPerson.isEmpty()) return

        val level = mc.level ?: return
        val player = mc.player ?: return
        val camera = mc.gameRenderer.mainCamera

        val viewToWorld = BedrockBoneCoordinateTool.cameraRotationInverse(camera)

        for (beam in firstPerson) {
            // 相机在渲染空间的原点，所以渲染空间的点就是"相对相机的世界偏移"
            val startWorld = camera.position.add(rotate(viewToWorld, beam.viewOrigin))
            val directionWorld = rotate(viewToWorld, beam.viewDirection).normalize()
            // 本地单位 → 世界米：骨骼带 scale 时本地一单位不是一格
            val rangeWorld = resolveLength(beam.info, true) * beam.axisScale

            val hit = level.clip(
                ClipContext(
                    startWorld,
                    startWorld.add(directionWorld.scale(rangeWorld)),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player,
                )
            )
            if (hit.type != HitResult.Type.BLOCK) continue

            beam.hitDistance = hit.location.distanceTo(startWorld) / beam.axisScale
            beam.hasHit = true
        }
    }

    private fun emitBeam(
        consumer: VertexConsumer,
        pose: Matrix4f,
        beam: LaserSightCapture.Beam,
    ) {
        val info = beam.info
        val configured = resolveLength(info, beam.firstPerson)
        if (configured <= 0.0) return

        val half = resolveHalfWidth(info, beam.firstPerson)
        if (half <= 0.0) return

        // 截断长度：命中距离（没命中就是配置长度）
        val truncated = if (beam.hasHit) beam.hitDistance.coerceIn(0.0, configured) else configured

        val drawn = truncated.coerceAtLeast(resolveMinLength(info)).coerceAtMost(configured)
        if (drawn <= 0.0) return

        val r = (beam.colorRgb shr 16) and 0xFF
        val g = (beam.colorRgb shr 8) and 0xFF
        val b = beam.colorRgb and 0xFF

        val endAlpha = (255.0 * (1.0 - drawn / configured)).roundToInt().coerceIn(0, 255)

        // Blockbench 约定模型正前方是 −Z，截面就在本地 XY 平面上
        val p0 = Vec3(0.0, 0.0, 0.0)
        val p1 = Vec3(0.0, 0.0, -drawn)
        val ex = Vec3(1.0, 0.0, 0.0)
        val ey = Vec3(0.0, 1.0, 0.0)

        emitTube(
            consumer, pose, p0, p1,
            half * CORE_WIDTH_RATIO, ex, ey,
            255, 255, 255, 255, endAlpha,
        )

        emitTube(
            consumer, pose, p0, p1,
            half, ex, ey,
            r, g, b, 255, endAlpha,
        )

        if (beam.hasHit) {
            emitDot(consumer, pose, drawn, beam.axisScale, r, g, b)
        }
    }

    /** 第一人称光束长度（米），非正数 / 非有限值回退该路的默认值 */
    private fun resolveLength(info: LaserInfo, firstPerson: Boolean): Double {
        val raw = if (firstPerson) info.length else info.thirdPersonLength
        if (!raw.isFinite() || raw <= 0f) {
            return (if (firstPerson) LaserInfo.DEFAULT_LENGTH else LaserInfo.DEFAULT_THIRD_PERSON_LENGTH).toDouble()
        }
        val max = if (firstPerson) LaserInfo.MAX_LENGTH else LaserInfo.MAX_THIRD_PERSON_LENGTH
        return raw.coerceAtMost(max).toDouble()
    }

    /** 截断后的绘制长度下限（米）：非正数 / 非有限值视为 0（不设下限） */
    private fun resolveMinLength(info: LaserInfo): Double {
        val raw = info.minLength
        if (!raw.isFinite() || raw <= 0f) return 0.0
        return raw.coerceAtMost(LaserInfo.MAX_LENGTH).toDouble()
    }

    /** 半宽（米）：全宽的一半，数据异常时回退到 0（这一束就不画了） */
    private fun resolveHalfWidth(info: LaserInfo, firstPerson: Boolean): Double {
        val raw = if (firstPerson) info.width else info.thirdPersonWidth
        if (!raw.isFinite() || raw <= 0f) return 0.0
        return raw.coerceAtMost(LaserInfo.MAX_WIDTH) / 2.0
    }

    /**
     * 把一个"相机相对偏移"在渲染空间与世界轴之间换基。
     *
     * 这里传进来的两份矩阵（`cameraRotationInverse` 与它的逆）都**只有旋转**，所以用
     * [Matrix4f.transformPosition] 对"位置"和"方向"是同一件事，而且它不会像 `transformDirection`
     * 那样需要调用方自己再归一化（见 `CharmRuntime` 里那句"必须归一化"的注释）—— 长度是这个函数的
     * 语义的一部分（出光口的半米、射线的几十米）。
     */
    private fun rotate(matrix: Matrix4f, v: Vec3): Vec3 {
        val r = matrix.transformPosition(Vector3f(v.x.toFloat(), v.y.toFloat(), v.z.toFloat()), Vector3f())
        return Vec3(r.x.toDouble(), r.y.toDouble(), r.z.toDouble())
    }

    /**
     * 落点光斑：**本地 XY 平面**上一小块方片，落在管被截断的那个平面上、沿光束朝出光口退一点点。
     *
     * 面法线是本地 +Z ——也就是沿着光束回看出光口的方向，正对着光束打上去的那面墙，
     * 所以瞄准时看到的是一个正圆/正方，斜掠时会自然摊成椭圆（真实激光的落点就是这样）。
     */
    private fun emitDot(
        consumer: VertexConsumer,
        pose: Matrix4f,
        distance: Double,
        axisScale: Float,
        r: Int,
        g: Int,
        b: Int,
    ) {
        val worldDistance = distance * axisScale
        val growth = max(1.0, worldDistance / DOT_GROW_START_DISTANCE).coerceAtMost(DOT_MAX_GROWTH.toDouble())
        val half = DOT_RADIUS * growth
        val z = -max(0.0, distance - DOT_SURFACE_OFFSET)

        val normal = Vec3(0.0, 0.0, 1.0)
        val p0 = Vec3(-half, -half, z)
        val p1 = Vec3(half, -half, z)
        val p2 = Vec3(half, half, z)
        val p3 = Vec3(-half, half, z)

        vertex(consumer, pose, p0, r, g, b, 255, 0f, 0f, normal)
        vertex(consumer, pose, p1, r, g, b, 255, 1f, 0f, normal)
        vertex(consumer, pose, p2, r, g, b, 255, 1f, 1f, normal)
        vertex(consumer, pose, p3, r, g, b, 255, 0f, 1f, normal)
    }

    /**
     * 一根空心方形管：沿 `p0 → p1` 拉出 4 个侧面，截面在以 `ux`/`uy` 为轴、边长 `2 * halfWidth` 的正方形上。
     *
     * 顶点的 UV：`u` 沿长度 0→1、`v` 每个面 0/1；顶点 alpha：起点 `startAlpha`、终点 `endAlpha`
     * （附加混合下 alpha 直接决定远端是否渐隐）。
     */
    private fun emitTube(
        consumer: VertexConsumer,
        pose: Matrix4f,
        p0: Vec3,
        p1: Vec3,
        halfWidth: Double,
        ux: Vec3,
        uy: Vec3,
        r: Int,
        g: Int,
        b: Int,
        startAlpha: Int,
        endAlpha: Int,
    ) {
        val uxh = ux.scale(halfWidth)
        val uyh = uy.scale(halfWidth)

        // +ux 面
        emitQuad(
            consumer, pose,
            p0.add(uxh).subtract(uyh), p0.add(uxh).add(uyh),
            p1.add(uxh).add(uyh), p1.add(uxh).subtract(uyh),
            ux, r, g, b, startAlpha, endAlpha,
        )
        // +uy 面
        emitQuad(
            consumer, pose,
            p0.subtract(uxh).add(uyh), p0.add(uxh).add(uyh),
            p1.add(uxh).add(uyh), p1.subtract(uxh).add(uyh),
            uy, r, g, b, startAlpha, endAlpha,
        )
        // -ux 面
        emitQuad(
            consumer, pose,
            p0.subtract(uxh).add(uyh), p0.subtract(uxh).subtract(uyh),
            p1.subtract(uxh).subtract(uyh), p1.subtract(uxh).add(uyh),
            ux.scale(-1.0), r, g, b, startAlpha, endAlpha,
        )
        // -uy 面
        emitQuad(
            consumer, pose,
            p0.subtract(uxh).subtract(uyh), p0.add(uxh).subtract(uyh),
            p1.add(uxh).subtract(uyh), p1.subtract(uxh).subtract(uyh),
            uy.scale(-1.0), r, g, b, startAlpha, endAlpha,
        )
    }

    /** 一个侧面：`a`/`b` 在 `p0` 端（alpha = startAlpha），`c`/`d` 在 `p1` 端（alpha = endAlpha） */
    private fun emitQuad(
        consumer: VertexConsumer,
        pose: Matrix4f,
        a: Vec3,
        b: Vec3,
        c: Vec3,
        d: Vec3,
        normal: Vec3,
        r: Int,
        g: Int,
        b2: Int,
        startAlpha: Int,
        endAlpha: Int,
    ) {
        vertex(consumer, pose, a, r, g, b2, startAlpha, 0f, 0f, normal)
        vertex(consumer, pose, b, r, g, b2, startAlpha, 0f, 1f, normal)
        vertex(consumer, pose, c, r, g, b2, endAlpha, 1f, 1f, normal)
        vertex(consumer, pose, d, r, g, b2, endAlpha, 1f, 0f, normal)
    }

    /** 一个 NEW_ENTITY 格式顶点；形状属性缺一个都会在 endVertex 抛异常 */
    private fun vertex(
        consumer: VertexConsumer,
        pose: Matrix4f,
        p: Vec3,
        r: Int,
        g: Int,
        b: Int,
        a: Int,
        u: Float,
        v: Float,
        normal: Vec3,
    ) {
        consumer.vertex(pose, p.x.toFloat(), p.y.toFloat(), p.z.toFloat())
            .color(r, g, b, a)
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(FULL_BRIGHT)
            .normal(normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat())
            .endVertex()
    }
}
