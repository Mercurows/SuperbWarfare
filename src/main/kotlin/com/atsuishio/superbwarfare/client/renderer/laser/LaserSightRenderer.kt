package com.atsuishio.superbwarfare.client.renderer.laser

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.client.renderer.laser.LaserSightRenderer.projectionScale
import com.atsuishio.superbwarfare.compat.oculus.OculusCompat
import com.atsuishio.superbwarfare.data.attachment.LaserInfo
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.tools.BedrockBoneCoordinateTool
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.systems.RenderSystem
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
import kotlin.math.tan

/**
 * 在枪械自己的绘制调用里直绘激光束（视图空间）。
 *
 * 坐标全部来自 [LaserSightCapture]：出光口与方向是画那件配件几何时用同一份矩阵算出来的，
 * 落点射线也在**这一帧**里做完，所以光束与枪体、配件同源同帧 —— 后坐、跑动摇摆、开镜压缩、
 * 换手全都不用再单独处理。
 *
 * ## 两条空间/投影上的坑（都是"画在手部 pass 里"带来的）
 *
 * 1. **模型矩阵**：视图空间的坐标**不能**再用枪自己的 `poseStack` 去画 —— 那份矩阵是"模型 → 视图空间"，
 *    再乘一遍等于把枪的定位变换算两遍（`idle_view` 的逆变换本身就是一米多的平移）。所以这里压一份
 *    **单位**模型矩阵，把坐标原样送进投影。
 * 2. **投影矩阵**：手部 pass 的投影是原版固定基准 70°，本模组的开镜倍率**只除在世界 pass 上**
 *    （见 [projectionScale]）—— 激光必须落在世界的画面上，所以远端坐标要按两份投影的比例换算，
 *    近端保持原样（贴着画出来的那把枪）。
 */
@OnlyIn(Dist.CLIENT)
object LaserSightRenderer {

    /** 内芯向白色提亮的比例 */
    private const val CORE_WHITENESS = 0.7f

    private const val GLOW_ALPHA = 90
    private const val CORE_ALPHA = 230

    /** 光斑基础半径（米） */
    private const val DOT_RADIUS = 0.035f

    private const val DOT_GROW_START_DISTANCE = 8.0
    private const val DOT_MAX_GROWTH = 4.0f

    /** 射程兜底值（米）：数据里写成 0 / 负数时用 */
    private const val DEFAULT_RANGE = 64.0

    /** 投影换算比例的夹值，防数据异常时把几何拉飞 */
    private const val MIN_PROJECTION_SCALE = 0.05f
    private const val MAX_PROJECTION_SCALE = 20f

    /** 全亮的光照贴图坐标 */
    private const val FULL_BRIGHT = 0xF000F0

    /** 纯白 16x16，颜色全靠顶点色 */
    private val TEXTURE = loc("textures/entity/white.png")

    /**
     * 画掉本帧采集到的全部激光束。
     *
     * 排在 `GeoGunRenderer.renderModel` 的**最后**：枪身几何（含瞄具模板那一路）都已落盘、
     * 模板测试也已关闭，激光不会被 `GL_EQUAL 0` 从镜筒里裁掉。
     */
    fun render(poseStack: PoseStack, bufferSource: MultiBufferSource) {
        val beams = LaserSightCapture.beams()
        if (beams.isEmpty()) return

        castBeams(beams)

        // 需要能自己控制落盘时机：激光要画在枪身之后、且要在自己指定的深度状态下画
        if (bufferSource !is MultiBufferSource.BufferSource) return

        val projectionScale = projectionScale()

        poseStack.pushPose()
        poseStack.last().pose().identity()
        poseStack.last().normal().identity()
        try {
            // 枪身那一批先落盘。换渲染类型时 BufferSource 本来也会把上一批冲掉，
            // 这里显式冲一次，让"激光最后画"这条顺序与下面的深度测试开关严格对齐。
            if (!OculusCompat.endBatch(bufferSource)) bufferSource.endBatch()

            // 关掉深度测试：出光口就在配件表面上，不关的话枪管 / 瞄具 / 手臂会把最近的那一小截
            // 光束切掉，看上去就不像"从激光口射出去"的。代价是光束不再被自己与枪之间的障碍物挡住
            // ——但它本来就画到命中点为止，被墙挡住的那一段不存在。
            // ⚠ LASER_SIGHT 这个 RenderType 没有 depth 状态，它的 clearState 不会把深度测试打开，
            // 必须自己恢复（否则后面画的枪、手、GUI 全都穿模）。
            RenderSystem.disableDepthTest()
            try {
                val renderType = ModRenderTypes.laserSight(TEXTURE)
                val consumer = bufferSource.getBuffer(renderType)
                val pose = poseStack.last().pose()
                for (beam in beams) {
                    emitBeam(consumer, pose, beam, projectionScale)
                }
                if (!OculusCompat.endBatch(bufferSource)) bufferSource.endBatch(renderType)
            } finally {
                RenderSystem.enableDepthTest()
            }
        } finally {
            poseStack.popPose()
        }
    }

    /**
     * 手部 pass 与世界 pass 的"放大比例"，也就是世界投影 ÷ 手部投影在 x/y 上的倍率。
     *
     * 这曾经是"三条激光各偏一个方向"的根因：原版 `GameRenderer.renderItemInHand` 在画手之前用
     * `getFov(camera, partialTick, false)` 重设过一次投影（基准固定 70°，不看玩家的 FOV 设置），
     * 而本模组的开镜倍率**只除在世界 pass 上**（[ClientEventHandler.onFovUpdate] 在手部 pass 直接
     * return），枪是靠 `zoomLengthScale` 沿 Z 压缩来"假装"开镜的 —— 枪有补偿，激光没有。于是激光按
     * 70° 出图、世界按 `设置 ÷ 倍率` 出图：开 4 倍镜时差 4 倍，FOV 设置不是 70 时差 `设置/70`；
     * 屏幕方向被按"离屏幕中心多远"径向压缩，起点不同的三条束就各偏一个方向。
     *
     * 透视投影里"换一个 FOV"与"在视图空间缩放像平面"是等价的（两份投影矩阵只差 `m00`/`m11` 这一项，
     * 近远比、宽高比、探照镜的 `zoom` 位移都相同），而 `m00 = 1 / tan(fov / 2)`，所以比例就是
     * `tan(手部/2) / tan(世界/2)`。**不去动投影矩阵**是刻意的：换矩阵会牵动 Iris / 加速渲染的
     * 状态机，而缩放顶点只是几何上的等价变换。
     */
    private fun projectionScale(): Float {
        val hand = ClientEventHandler.handFov
        val world = ClientEventHandler.fov
        if (!hand.isFinite() || !world.isFinite() || hand <= 0.0 || world <= 0.0) return 1f

        val tanHand = tan(Math.toRadians(hand / 2.0))
        val tanWorld = tan(Math.toRadians(world / 2.0))
        if (tanWorld < 1e-6) return 1f

        val scale = (tanHand / tanWorld).toFloat()
        if (!scale.isFinite() || scale <= 0f) return 1f
        return scale.coerceIn(MIN_PROJECTION_SCALE, MAX_PROJECTION_SCALE)
    }

    /**
     * 每帧把视图空间的束送到世界里做一次方块射线，再把落点搬回视图空间。
     *
     * 视图 ↔ 世界只差一个相机旋转，用的就是第一人称枪口粒子那一套
     * （[BedrockBoneCoordinateTool.cameraRotationInverse]，含相机横滚），所以这里不自己拼
     * `left/up/look` 基向量，也不用链式 `rotateX/Y/Z` 去凑。
     *
     * ⚠ 射线必须和采集出光口在**同一帧**里做：出光口是视图空间的量，而视图空间随相机每帧变化，
     * 隔一个 tick 再拿它换算世界坐标，等于用旧相机去解释新的枪姿态，快速转头时光束会被拧过去。
     */
    private fun castBeams(beams: List<LaserSightCapture.Beam>) {
        val level = mc.level ?: return
        val player = mc.player ?: return
        val camera = mc.gameRenderer.mainCamera

        val viewToWorld = BedrockBoneCoordinateTool.cameraRotationInverse(camera)
        val worldToView = Matrix4f(viewToWorld).invert()

        for (beam in beams) {
            // 相机在视图空间的原点，所以视图空间的点就是"相对相机的世界偏移"
            val startWorld = camera.position.add(rotate(viewToWorld, beam.viewOrigin))
            val directionWorld = rotate(viewToWorld, beam.viewDirection).normalize()
            val range = resolveRange(beam.info)

            val hit = level.clip(
                ClipContext(
                    startWorld,
                    startWorld.add(directionWorld.scale(range)),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player,
                )
            )

            val endWorld =
                if (hit.type == HitResult.Type.MISS) startWorld.add(directionWorld.scale(range))
                else hit.location

            beam.viewEnd = rotate(worldToView, endWorld.subtract(camera.position))
            beam.hasHit = hit.type == HitResult.Type.BLOCK
        }
    }

    /** 射程，非正数回退默认值 */
    private fun resolveRange(info: LaserInfo): Double {
        val raw = info.range
        if (!raw.isFinite() || raw <= 0f) return DEFAULT_RANGE
        return raw.coerceAtMost(LaserInfo.MAX_RANGE).toDouble()
    }

    /**
     * 把一个"相机相对偏移"在视图空间与世界轴之间换基。
     *
     * 这里传进来的两份矩阵（`cameraRotationInverse` 与它的逆）都**只有旋转**，所以用
     * [Matrix4f.transformPosition] 对"位置"和"方向"是同一件事，而且它不会像 `transformDirection`
     * 那样需要调用方自己再归一化（见 `CharmRuntime` 里那句"必须归一化"的注释）—— 长度是这个函数的
     * 语义的一部分（出光口的半米、落点的二十米）。
     */
    private fun rotate(matrix: Matrix4f, v: Vec3): Vec3 {
        val r = matrix.transformPosition(Vector3f(v.x.toFloat(), v.y.toFloat(), v.z.toFloat()), Vector3f())
        return Vec3(r.x.toDouble(), r.y.toDouble(), r.z.toDouble())
    }

    /**
     * 一束 = 外辉光 + 内芯（+ 命中时的落点光斑）。
     *
     * ⚠ 两端的换算方式**故意不同**，这是让光束"既咬住枪、又打在世界画面上"的关键：
     *
     * - **近端（出光口）按原样画**：枪本体的开镜是 `zoomLengthScale` 压缩假装的，它的屏幕位置与
     *   手部投影自洽，光束起点跟着它才落在画出来的激光口上；
     * - **远端（落点）乘 [projectionScale]**：落点是世界里的一个点，必须落在世界画出来的那一像素上，
     *   否则贴墙光斑会随开镜倍率整体偏移。
     *
     * 两端各自落到正确的像素上，中间按直线连起来，就是这条光束在这个世界里该有的样子。
     * 宽度（[LaserInfo.glowWidth] / [LaserInfo.coreWidth] / 光斑半径）是世界尺度，一起按比例放大。
     */
    private fun emitBeam(
        consumer: VertexConsumer,
        pose: Matrix4f,
        beam: LaserSightCapture.Beam,
        projectionScale: Float,
    ) {
        val start = beam.viewOrigin
        val end = scaleXY(beam.viewEnd, projectionScale)
        if (end.subtract(start).lengthSqr() < 1e-8) return

        val r = (beam.colorRgb shr 16) and 0xFF
        val g = (beam.colorRgb shr 8) and 0xFF
        val b = beam.colorRgb and 0xFF

        val glowHalf = beam.info.glowWidth.coerceIn(0f, LaserInfo.MAX_HALF_WIDTH).toDouble() * projectionScale
        if (glowHalf > 0.0) {
            emitRibbon(consumer, pose, start, end, glowHalf, r, g, b, GLOW_ALPHA)
        }

        val coreHalf = beam.info.coreWidth.coerceIn(0f, LaserInfo.MAX_HALF_WIDTH).toDouble() * projectionScale
        if (coreHalf > 0.0) {
            emitRibbon(
                consumer, pose, start, end, coreHalf,
                mixToWhite(r), mixToWhite(g), mixToWhite(b), CORE_ALPHA,
            )
        }

        if (beam.hasHit) {
            emitDot(consumer, pose, start, end, r, g, b, projectionScale)
        }
    }

    /** 只缩放世界投影会缩放的那两个轴（z 是深度，两份投影的深度映射完全相同） */
    private fun scaleXY(v: Vec3, scale: Float): Vec3 =
        Vec3(v.x * scale, v.y * scale, v.z)

    /**
     * 两条互相垂直的细带。
     *
     * 截面方向由"束轴 × 视线"决定 —— 视图空间里视线恒为 −Z，所以这里完全没有
     * 世界轴参考向量（早先用世界轴叉乘，枪一滚转光柱就在原地打转）。
     */
    private fun emitRibbon(
        consumer: VertexConsumer,
        pose: Matrix4f,
        start: Vec3,
        end: Vec3,
        halfWidth: Double,
        r: Int,
        g: Int,
        b: Int,
        alpha: Int,
    ) {
        val axisVec = end.subtract(start)
        val length = axisVec.length()
        if (length < 1e-4) return
        val axis = axisVec.scale(1.0 / length)

        // 视图空间里"指向相机"就是 +Z（相机在原点、朝 −Z 看）
        var side = axis.cross(Vec3(0.0, 0.0, 1.0))
        if (side.lengthSqr() < 1e-8) side = axis.cross(Vec3(0.0, 1.0, 0.0))
        if (side.lengthSqr() < 1e-8) return
        side = side.normalize()
        val other = axis.cross(side).normalize()

        emitQuad(consumer, pose, start, end, side, halfWidth, r, g, b, alpha)
        emitQuad(consumer, pose, start, end, other, halfWidth, r, g, b, alpha)
    }

    private fun emitQuad(
        consumer: VertexConsumer,
        pose: Matrix4f,
        start: Vec3,
        end: Vec3,
        side: Vec3,
        halfWidth: Double,
        r: Int,
        g: Int,
        b: Int,
        alpha: Int,
    ) {
        val offset = side.scale(halfWidth)
        val sa = start.subtract(offset)
        val ea = end.subtract(offset)
        val eb = end.add(offset)
        val sb = start.add(offset)

        // 法线朝相机即可（自发光，法线只影响混合着色）
        val normal = Vec3(0.0, 0.0, 1.0).let { if (it.dot(side) < 0) it else it.scale(-1.0) }

        vertex(consumer, pose, sa, r, g, b, alpha, 0f, 0f, normal)
        vertex(consumer, pose, ea, r, g, b, alpha, 1f, 0f, normal)
        vertex(consumer, pose, eb, r, g, b, alpha, 1f, 1f, normal)
        vertex(consumer, pose, sb, r, g, b, alpha, 0f, 1f, normal)
    }

    /** 落点光斑：视图空间里正对相机的一小块方片（落在世界画出来的那个像素上） */
    private fun emitDot(
        consumer: VertexConsumer,
        pose: Matrix4f,
        start: Vec3,
        end: Vec3,
        r: Int,
        g: Int,
        b: Int,
        projectionScale: Float,
    ) {
        val axis = end.subtract(start)
        val length = axis.length()
        if (length < 1e-4) return

        val growth = max(1.0, length / DOT_GROW_START_DISTANCE).coerceAtMost(DOT_MAX_GROWTH.toDouble())
        val half = DOT_RADIUS * growth * projectionScale
        val normal = Vec3(0.0, 0.0, 1.0)

        // 微朝相机偏移一点，避免与命中面 z-fighting
        val center = end.add(normal.scale(0.01))
        val p0 = center.add(-half, -half, 0.0)
        val p1 = center.add(half, -half, 0.0)
        val p2 = center.add(half, half, 0.0)
        val p3 = center.add(-half, half, 0.0)

        vertex(consumer, pose, p0, r, g, b, 255, 0f, 0f, normal)
        vertex(consumer, pose, p1, r, g, b, 255, 1f, 0f, normal)
        vertex(consumer, pose, p2, r, g, b, 255, 1f, 1f, normal)
        vertex(consumer, pose, p3, r, g, b, 255, 0f, 1f, normal)
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

    /** 把通道向白色插值 */
    private fun mixToWhite(channel: Int): Int = (channel + (255 - channel) * CORE_WHITENESS).toInt()
}
