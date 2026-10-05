package com.atsuishio.superbwarfare.client.renderer.laser

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.client.renderer.laser.LaserSightRenderer.CORE_WIDTH_RATIO
import com.atsuishio.superbwarfare.client.renderer.laser.LaserSightRenderer.TEXTURE
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
 * 在枪械自己的绘制调用里直绘激光束。
 *
 * 几何与观感照 TACZ（`BeamRenderer`）：**一根沿出光口本地 −Z 拉伸的空心方形管**（4 个侧面），
 * 附加混合、顶点 alpha 沿长度从 255 渐隐到 0。TACZ 的 `textures/entity/beam.png` 实测是
 * 纯白 8×8，观感全部来自几何与顶点 alpha，所以这里直接复用 [TEXTURE]（white.png），不新增贴图。
 * 本模组在彩色外层里再套了一根更细的**纯白内芯**（[CORE_WIDTH_RATIO]），凑出"白芯 + 本色辉光"的
 * 激光观感；两层几何完全一样，只是半宽与颜色不同。
 *
 * **绘制方式也照 TACZ**：用 [ModRenderTypes.LASER_BEAM]（带 `ITEM_ENTITY_TARGET` 输出状态，
 * 光影才认得这是"物品 / 实体"那一路，不会把光束按错误的深度关系丢给云和地形去挡）。光束画在
 * 瞄具模板窗口**内部**（`GeoGunRenderer` 在窗口收尾之前调它），所以**会被瞄准镜剪裁**：高倍镜
 * （`scope`）里枪身与配件被 `GL_EQUAL 0` 从镜内剔掉时，光束在镜内那一段也一起被剔掉，不会出现
 * "一根亮管悬在镜片里、看不出从哪来"。镜外则照常被枪身与镜筒按深度遮挡。
 *
 * ## 全部在出光口的本地空间里画
 *
 * 每一束都用 [LaserSightCapture.Beam.emitterMatrix]（"出光口 → 本次渲染空间"）覆盖 pose，然后在
 * **本地坐标**里沿 −Z 拉管。于是光束与枪身**刚性绑定**：姿态怎么转（疾跑摇摆、后坐、开镜的
 * `zoomLengthScale` 压缩、换弹动画），光束就怎么转，中间没有任何独立算出来的量。第一人称与
 * 第三人称只差**用哪一组长度 / 宽度**，几何、混合、渐隐、光斑四者完全共用。
 *
 * ## 方块截断为什么不会再把光束掰弯
 *
 * 每帧仍然对世界做一次方块射线（只有第一人称做），但射线结果只被当成一个**标量长度**：
 * 命中就画短一点，没命中就画满 [LaserInfo.length]。管的两端始终是本地 `(0,0,0)` 与 `(0,0,−d)`，
 * 方向永远是本地 −Z。截断还有一个下限（[LaserInfo.minLength]，默认 3 米）：贴脸对着墙时方管不会
 * 缩成看不见的一小截，光斑跟着方管末端一起走。
 *
 * ⚠ 这里曾经把远端当成"世界里的一个点"来画：端点按 `projectionScale` 折算（手部 pass 投影固定
 * 70°，开镜倍率只除在世界 pass 上）。那条路有个很隐蔽的方向偏差 —— 只缩放端点 x/y 而 z 不动是
 * **非保角**变换，比例 k ≠ 1 时（疾跑会改世界 FOV，开镜更是差好几倍）光束的**方向**会被掰弯，
 * 看上去就是"光束不跟着枪转、疾跑时和枪身差一个角度"。**把世界信息限死在"长度"这一个自由度上**，
 * 偏差就从结构上消失了：长度怎么变都还是在同一条射线上。
 */
@OnlyIn(Dist.CLIENT)
object LaserSightRenderer {

    /** 光斑基础半径（米），再按距离放大（见 [DOT_GROW_START_DISTANCE]） */
    private const val DOT_RADIUS = 0.035f

    private const val DOT_GROW_START_DISTANCE = 8.0
    private const val DOT_MAX_GROWTH = 4.0f

    /** 光斑沿光束朝出光口方向退这么远，免得和命中面 z-fighting */
    private const val DOT_SURFACE_OFFSET = 0.01

    /**
     * 白芯的半宽 / 外层的半宽。
     *
     * 白芯是**同一根管、更细的一层**，颜色纯白、贴在外层彩色辉光里面：附加混合下"彩色 + 白"就是
     * 中间一条亮到发白的芯、外面一圈本色辉光，和真实激光的观感一致（TACZ 只有单层，这一层是本模组
     * 额外加的）。取 0.45 而不是更小，是因为白芯太细时在远处会先于外层被像素网格切没，只剩一条纯色
     * 管子；再大又会吃掉辉光的宽度。
     */
    private const val CORE_WIDTH_RATIO = 0.45

    /** 全亮的光照贴图坐标 */
    private const val FULL_BRIGHT = 0xF000F0

    /** 纯白 16x16，颜色全靠顶点色 */
    private val TEXTURE = loc("textures/entity/white.png")

    /**
     * 画掉本帧采集到的全部激光束（整根，从出光口到命中点 / 配置长度）。
     *
     * 排在 `GeoGunRenderer.renderModel` 的**模板窗口收尾之前**：`GL_EQUAL 0` 还在生效，所以高倍镜
     * （`scope`）里光束会和枪身一样被从镜内剔掉，镜片里不会留下一截悬空的管子。第三人称右手排在
     * 同一处，但只画自己的短管、不射线（那边根本没有镜筒窗口，不受影响）。
     */
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

    /**
     * 每帧把第一人称的束送到世界里做一次方块射线，**只把命中距离记下来**。
     *
     * 视图 ↔ 世界只差一个相机旋转，用的就是第一人称枪口粒子那一套
     * （[BedrockBoneCoordinateTool.cameraRotationInverse]，含相机横滚），所以这里不自己拼
     * `left/up/look` 基向量，也不用链式 `rotateX/Y/Z` 去凑。
     *
     * ⚠ 射线必须和采集出光口在**同一帧**里做：出光口是渲染空间的量，而渲染空间随相机每帧变化，
     * 隔一个 tick 再拿它换算世界坐标，等于用旧相机去解释新的枪姿态。
     *
     * 第三人称的短光束不射线（同 TACZ：它本来就只有两格长，参考意义不大）。
     */
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

    /**
     * 一束：本地空间里 p0 = 原点、p1 = (0, 0, −长度) 的**两层**空心方管（白芯 + 本色外层），
     * 末端 alpha 渐隐；两头的端面都不封口（TACZ 也是）—— 没被截断的远端渐隐到 0 之后本来就看不见。
     *
     * 命中方块时只把**长度**换成长度更小的命中距离（截断），方向不动；命中处再补一个落在该平面上的
     * 光斑。光斑不分层：它只有一片、位置就在截断面上，套白芯反而会在近处的墙上糊出一圈同心方框。
     *
     * 截断有下限（[LaserInfo.minLength]，默认 3 米）：方管最短也画这么长，**光斑跟着方管末端走**，
     * 两者不会一个在墙外、一个留在墙上。
     */
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

        // 实际绘制长度：不短于 [LaserInfo.minLength]。贴脸对着墙时命中距离趋近 0，方管会被压成
        // 看不见的一小截 —— 激光存在的意义就是那条线，所以给它一个下限。下限本身再被本次的配置长度
        // 压住（超不过自己声明的长度），所以第三人称那条约两格的短光束不受影响
        //（何况它根本不射线，[truncated] 恒等于 configured）。
        val drawn = truncated.coerceAtLeast(resolveMinLength(info)).coerceAtMost(configured)
        if (drawn <= 0.0) return

        val r = (beam.colorRgb shr 16) and 0xFF
        val g = (beam.colorRgb shr 8) and 0xFF
        val b = beam.colorRgb and 0xFF

        // 亮度按"每米衰减率"均匀：截断到 d 时保留头段该有的亮度（1 − d/L）。
        // 若末端一律取 0，近距离的墙会让光束在墙前就淡没、只剩一个亮光斑，扫过墙角时还会整根跳暗。
        // ⚠ 这里用的是**方管**的长度（[drawn]，可能因下限而长于命中距离）：alpha 表达的是
        // "光走到这个位置还剩多少亮度"，所以必须跟着方管自己的末端走，否则下限会让末端亮度突变。
        val endAlpha = (255.0 * (1.0 - drawn / configured)).roundToInt().coerceIn(0, 255)

        // Blockbench 约定模型正前方是 −Z，截面就在本地 XY 平面上
        val p0 = Vec3(0.0, 0.0, 0.0)
        val p1 = Vec3(0.0, 0.0, -drawn)
        val ex = Vec3(1.0, 0.0, 0.0)
        val ey = Vec3(0.0, 1.0, 0.0)

        // ⚠ 白芯必须**先画**。两层都写深度（[ModRenderTypes.LASER_BEAM] 的 `COLOR_DEPTH_WRITE`），
        // 而且 `NO_CULL` 下四壁不看朝向、谁先写深度谁就赢过更远的那面：一根射线穿过管子，只留下
        // 最近的那个面。白芯比外层细，同一像素上"芯的近壁"总在"外层的近壁"后面 —— 先画芯，芯的近壁
        // 落盘；再画外层时外层的近壁更近、`LEQUAL` 通过，两者在附加混合下叠加成"白芯 + 本色辉光"。
        // 反过来先画外层，外层的近壁会把芯整个挡掉，白芯一点都看不见。
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

        // 光斑画在**方管末端**（[drawn]，含下限），不画在真实命中点上：两者必须一致，
        // 否则近处会出现"方管穿出墙外、光斑单独留在墙上"的断层
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
