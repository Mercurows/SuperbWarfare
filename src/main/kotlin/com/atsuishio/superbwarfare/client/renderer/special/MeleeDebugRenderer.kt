package com.atsuishio.superbwarfare.client.renderer.special

import com.atsuishio.superbwarfare.client.gun.GunActionLock
import com.atsuishio.superbwarfare.client.renderer.special.MeleeDebugRenderer.CAP_SEGMENTS
import com.atsuishio.superbwarfare.client.renderer.special.OBBRenderer.renderOBB
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.tools.MeleeQuery
import com.atsuishio.superbwarfare.tools.clientLevel
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import kotlin.math.cos
import kotlin.math.sin

/**
 * 近战判定体可视化。
 *
 * **触发方式（两种，满足其一即可）**：
 * 1. **原版 `F3 + B`**（实体 hitbox 显示）打开 —— 推荐，和原版"显示判定体"是同一个开关；
 * 2. `DisplayConfig.MELEE_HITBOX_RENDER` 打开（`run/config/superbwarfare-client.toml`，
 *    键名 `melee_hitbox_render`）—— 不按 F3 也常显。
 *
 * 前提：**主手是一把能近战的枪**（`hasMeleeAttack()`）。
 *
 * 画的是**真实形状**（见 `MeleeQuery.debugShapes`）：
 * - `Cone` → 顶点在眼睛、末端圆环半径 `reach × sin(半角)`、母线连到顶点；
 *   `Pitch >= 180` 时垂直不受限，只画球面天线罩
 * - `Box` → 带 yaw/pitch 姿态的盒体
 * - `Capsule` → 沿视线的线段（两端加端盖圆）
 *
 * 颜色：**红 = 当前正在挥的那一段**，其余为 **橙**。
 *
 * > **别再把它画成"近似盒"了**（§11.2-㉓）：`reach` 通常是 6~7 格（`Range + getEntityReach()`），
 * > 而一个"宽 `reach×sin(半角)`、长 `reach/2`"的盒子又粗又短，
 * > 看的人会得出"盒子里的怪打不到、盒子外的怪反而挨打"的错误结论 —— 判定其实一直是对的。
 */
@EventBusSubscriber(value = [Dist.CLIENT])
object MeleeDebugRenderer {

    private const val COLOR_ACTIVE_R = 1f
    private const val COLOR_ACTIVE_G = 0.25f
    private const val COLOR_ACTIVE_B = 0.2f
    private const val COLOR_IDLE_R = 1f
    private const val COLOR_IDLE_G = 0.6f
    private const val COLOR_IDLE_B = 0.15f

    /** 圆锥末端圆环的采样点数 */
    private const val RING_SEGMENTS = 48

    /** 胶囊端盖的采样点数 */
    private const val CAP_SEGMENTS = 16

    @SubscribeEvent
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return

        // 任一开关打开即可：F3+B 是原版既有的"显示判定体"，配置项用于长开
        val hitBoxes = mc.entityRenderDispatcher.shouldRenderHitBoxes()
        if (!hitBoxes && !DisplayConfig.MELEE_HITBOX_RENDER.get()) return

        val player = localPlayer ?: return
        if (clientLevel == null) return

        val stack = player.mainHandItem
        if (stack.item !is GunItem || !GunItem.isHeldWeapon(stack)) return

        val data = GunData.from(stack)
        if (!data.hasMeleeAttack()) return

        val state = GunActionLock.of(data)
        val context = MeleeQuery.contextOf(player, data.resolveMeleeAction(state.meleeActionIndex))

        val poseStack = event.poseStack
        val camera = event.camera
        val bufferSource = mc.renderBuffers().bufferSource()
        val buffer = bufferSource.getBuffer(RenderType.lines())

        poseStack.pushPose()

        // 相机相对变换：判定体坐标是世界坐标
        poseStack.translate(-camera.position.x, -camera.position.y, -camera.position.z)

        val active = state.meleeTicks > 0
        for (actionIndex in data.meleeActions().indices) {
            val isActive = active && actionIndex == state.meleeActionIndex
            val action = data.resolveMeleeAction(actionIndex)

            val r: Float
            val g: Float
            val b: Float
            if (isActive) {
                r = COLOR_ACTIVE_R; g = COLOR_ACTIVE_G; b = COLOR_ACTIVE_B
            } else {
                r = COLOR_IDLE_R; g = COLOR_IDLE_G; b = COLOR_IDLE_B
            }

            for (shape in MeleeQuery.debugShapes(context, action)) {
                when (shape) {
                    is MeleeQuery.DebugShape.Cone -> renderCone(poseStack, buffer, shape, r, g, b)
                    is MeleeQuery.DebugShape.Box -> renderOBB(
                        poseStack, buffer,
                        shape.center.x, shape.center.y, shape.center.z,
                        // 与判定共用同一个旋转，否则"看到的盒子"≠"判定的盒子"
                        MeleeQuery.yawPitchQuaternion(shape.yaw, shape.pitch),
                        shape.radiusX, shape.radiusY, shape.radiusZ,
                        r, g, b, 1f,
                    )

                    is MeleeQuery.DebugShape.Segment -> renderSegment(poseStack, buffer, shape, r, g, b)
                }
            }
        }

        poseStack.popPose()
        bufferSource.endBatch()
    }

    /**
     * 圆锥线框：顶点 + 末端圆环 + 若干母线。
     *
     * 用一套与[MeleeQuery.yawPitchQuaternion]一致的正交基（轴线 = 视线）把圆环画出来，
     * 这样俯仰时圆环跟着倾斜，看起来就是个真正的锥体。
     */
    private fun renderCone(
        poseStack: PoseStack,
        buffer: VertexConsumer,
        cone: MeleeQuery.DebugShape.Cone,
        r: Float,
        g: Float,
        b: Float,
    ) {
        if (cone.reach <= 0.0) return

        // 轴线正交基：axis / right / up
        val axis = cone.axis.normalize()
        val upHint = if (kotlin.math.abs(axis.y) > 0.99) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)
        val right = axis.cross(upHint).normalize()
        val up = right.cross(axis).normalize()

        val ringCenter = cone.apex.add(axis.scale(cone.reach))
        val radius = cone.radius

        var prev: Vec3? = null
        var first: Vec3? = null
        for (i in 0..RING_SEGMENTS) {
            val t = (i % RING_SEGMENTS) * (2.0 * Math.PI / RING_SEGMENTS)
            val point = ringCenter
                .add(right.scale(radius * cos(t)))
                .add(up.scale(radius * sin(t)))

            if (first == null) first = point
            prev?.let { line(poseStack, buffer, it, point, r, g, b) }
            prev = point
        }

        // 母线：4 条，从顶点连到圆环
        val spokes = if (cone.verticalCapped) 8 else 4
        for (i in 0 until spokes) {
            val t = i * (2.0 * Math.PI / spokes)
            val point = ringCenter
                .add(right.scale(radius * cos(t)))
                .add(up.scale(radius * sin(t)))
            line(poseStack, buffer, cone.apex, point, r, g, b)
        }
    }

    /** 胶囊线框：轴线 + 两端圆环 */
    private fun renderSegment(
        poseStack: PoseStack,
        buffer: VertexConsumer,
        segment: MeleeQuery.DebugShape.Segment,
        r: Float,
        g: Float,
        b: Float,
    ) {
        line(poseStack, buffer, segment.start, segment.end, r, g, b)
        if (segment.radius <= 0.0) return

        val axis = segment.end.subtract(segment.start)
        if (axis.lengthSqr() < 1e-9) return
        val dir = axis.normalize()
        val upHint = if (kotlin.math.abs(dir.y) > 0.99) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)
        val right = dir.cross(upHint).normalize()
        val up = right.cross(dir).normalize()

        for (end in listOf(segment.start, segment.end)) {
            var prev: Vec3? = null
            for (i in 0..CAP_SEGMENTS) {
                val t = (i % CAP_SEGMENTS) * (2.0 * Math.PI / CAP_SEGMENTS)
                val point = end
                    .add(right.scale(segment.radius * cos(t)))
                    .add(up.scale(segment.radius * sin(t)))
                prev?.let { line(poseStack, buffer, it, point, r, g, b) }
                prev = point
            }
        }
    }

    private fun line(
        poseStack: PoseStack,
        buffer: VertexConsumer,
        from: Vec3,
        to: Vec3,
        r: Float,
        g: Float,
        b: Float,
    ) {
        val pose = poseStack.last()
        val dx = (to.x - from.x).toFloat()
        val dy = (to.y - from.y).toFloat()
        val dz = (to.z - from.z).toFloat()
        val len = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
        if (len < 1e-6f) return
        val nx = dx / len
        val ny = dy / len
        val nz = dz / len

        buffer.addVertex(pose.pose(), from.x.toFloat(), from.y.toFloat(), from.z.toFloat())
            .setColor(r, g, b, 1f)
            .setNormal(pose, nx, ny, nz)
        buffer.addVertex(pose.pose(), to.x.toFloat(), to.y.toFloat(), to.z.toFloat())
            .setColor(r, g, b, 1f)
            .setNormal(pose, nx, ny, nz)
    }

    /**
     * 把圆柱面的一部分用线段连起来：分 [CAP_SEGMENTS] 段画一圈。
     */
    private fun circle(
        poseStack: PoseStack,
        buffer: VertexConsumer,
        center: Vec3,
        right: Vec3,
        up: Vec3,
        radius: Double,
        r: Float,
        g: Float,
        b: Float,
    ) {
        var prev: Vec3? = null
        for (i in 0..CAP_SEGMENTS) {
            val t = (i % CAP_SEGMENTS) * (2.0 * Math.PI / CAP_SEGMENTS)
            val point = center.add(right.scale(radius * cos(t))).add(up.scale(radius * sin(t)))
            prev?.let { line(poseStack, buffer, it, point, r, g, b) }
            prev = point
        }
    }
}
