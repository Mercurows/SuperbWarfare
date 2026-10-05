package com.atsuishio.superbwarfare.client.renderer.laser

import com.atsuishio.superbwarfare.client.renderer.laser.LaserSightCapture.beginFrame
import com.atsuishio.superbwarfare.client.renderer.laser.LaserSightCapture.capture
import com.atsuishio.superbwarfare.data.attachment.LaserInfo
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f

/**
 * 本帧全部激光束的**捕获槽**。
 *
 * 一束激光的生命周期就是一次 `GeoGunRenderer.renderModel`：
 * [beginFrame] 清空 → 画每件激光配件时 [capture] 采一束 → [LaserSightRenderer] 在**同一次调用**
 * 的稍后整批画掉。
 *
 * ⚠ 这里**不能**有任何跨帧状态。上一版把束按 `(手, 槽位)` 存进一张表、靠"谁采集谁续期"淘汰，
 * 但续期那一句写在 `getOrPut` 的默认值里（只在新建时跑一次），于是束在创建后 3 tick 就被当成
 * 过期淘汰、下一帧又用零值重建 —— 表现就是激光每 4 tick（约 5 Hz）闪一下，中间还夹着几帧
 * "从枪口射向自己眼睛"的乱束。生命周期收到一次绘制里之后，这类问题从结构上不存在。
 *
 * 采集出来的是**一份矩阵**：几何完全在这份矩阵的本地空间里画（沿 −Z 拉管）。矩阵里另外取出的
 * 出光口位置 / 方向**只服务于方块射线**（算"光束该在多长的地方被墙截断"），
 * **绝不参与摆放几何** —— 一旦拿世界里的点去定光束的远端，方向就会被投影换算掰弯，
 * 见 [LaserSightRenderer] 的说明。
 */
object LaserSightCapture {

    private val beams = ArrayList<Beam>(4)

    /** 一次 `renderModel` 开始：上一帧的束全部作废，本次调用重新采集 */
    @JvmStatic
    fun beginFrame() {
        beams.clear()
    }

    /**
     * 采一束激光的出光口。
     *
     * [poseMatrix] 必须是**画该配件几何用的那一份** `poseStack.last().pose()`
     * （已经乘过挂点骨骼与配件自己的旋转），[locatorTransform] 是配件模型里出光口 locator 的
     * 全局变换；两者相乘就是"出光口 → 本次渲染空间"，与画面同源，所以这里不再另拼一遍
     * `poseStack × mount × rotation`（变换次序一旦与渲染路径分叉，出光口就会离骨骼一截。
     * 疾跑摇摆、后坐这些每帧都在动的姿态也因此在结构上不可能与枪身不同步）。
     *
     * 光束方向就是这份矩阵里的本地 **−Z**（Blockbench 约定模型正前方是 −Z）：四条导轨的挂点骨骼都是
     * 绕 Z 转的（`upper_rail_pos` 是 `[0,0,-180]`、左右导轨是 `±90`，个别枪的下导轨只有 1° 俯仰修正），
     * Z 轴不受影响，所以这个方向自动等于枪械正前方，不需要按槽位特判。
     *
     * @param firstPerson 是否走第一人称：决定用哪一组长度 / 宽度（[LaserInfo.length] 还是
     *   [LaserInfo.thirdPersonLength]），以及要不要做方块射线（只有第一人称做）。
     * @return 采集到的束；配件模型里没有那个 locator、或变换退化时返回 `null`（什么都不画）
     */
    @JvmStatic
    fun capture(
        info: LaserInfo,
        colorRgb: Int,
        firstPerson: Boolean,
        poseMatrix: Matrix4f,
        locatorTransform: Matrix4f?,
    ): Beam? {
        if (locatorTransform == null) return null

        val matrix = Matrix4f(poseMatrix).mul(locatorTransform)
        val origin = matrix.getTranslation(Vector3f())

        // 方向 = 矩阵作用在局部 **−Z** 上的像。它**只给射线用**，几何永远按本地 −Z 画。
        //
        // ⚠ 必须走"方向变换"这条 API，**不能**手写 `-m02/-m12/-m22`：JOML 的字段名是
        // `m<列><行>`（`m30/m31/m32` 是平移），所以**第三列**（局部 +Z 的像）是 `(m20, m21, m22)`，
        // 而 `(m02, m12, m22)` 是第三**行** —— 那是把"模型空间 → 渲染空间"的矩阵转置着用，
        // 只有挂点没带旋转的下导轨（θ=0 时那一项退化）看着是对的，其余导轨会像被挂点多转 90°*n。
        //
        // 这个向量的**长度**是矩阵沿该轴的缩放（正常骨骼是 1，挂点骨骼带了 scale 时不是），
        // 射线距离要拿它把"世界米"换回"本地单位"。
        val axis = matrix.transformDirection(0f, 0f, -1f, Vector3f())
        val axisScale = axis.length()
        if (axisScale < 1e-6f) return null
        axis.div(axisScale)

        return Beam(
            info = info,
            colorRgb = colorRgb,
            firstPerson = firstPerson,
            emitterMatrix = matrix,
            viewOrigin = Vec3(origin.x.toDouble(), origin.y.toDouble(), origin.z.toDouble()),
            viewDirection = Vec3(axis.x.toDouble(), axis.y.toDouble(), axis.z.toDouble()),
            axisScale = axisScale,
        ).also { beams += it }
    }

    /** 本帧要画的全部束（一把枪上四根导轨各装一件时就是四束） */
    @JvmStatic
    fun beams(): List<Beam> = beams

    /**
     * 一束激光。
     *
     * 出光口 / 颜色在 [capture] 里一次定死，**对象每帧新建、不做跨帧复用**：
     * 复用对象时只要漏写一个字段，上一帧的值就会留在画面上，那是比每帧几个小对象昂贵得多的 bug。
     * [hitDistance] 与 [hasHit] 由 [LaserSightRenderer] 在同一帧里做完射线后写回。
     */
    class Beam(
        /** 这件配件的激光配置（长度、粗细、颜色都在里面） */
        val info: LaserInfo,

        /** 最终颜色（配件 tag 覆盖已由调用方合成） */
        val colorRgb: Int,

        /** 是否第一人称：选长度 / 宽度的一组，并且只有第一人称做方块射线 */
        val firstPerson: Boolean,

        /**
         * "出光口 → 本次渲染空间"的合成矩阵（`poseMatrix × locatorTransform`）。
         *
         * 直接覆盖到 pose 上，就能在出光口的本地空间里沿 −Z 画管 —— 这是光束与枪身刚性绑定的
         * 全部机制，几何的位置与方向都只由它决定。
         */
        val emitterMatrix: Matrix4f,

        /** 出光口（渲染空间）。**只给射线用** */
        val viewOrigin: Vec3,

        /** 出光方向（渲染空间，单位向量）。**只给射线用** */
        val viewDirection: Vec3,

        /** 矩阵沿出光轴的缩放：本地长度 × 它 = 世界米。正常骨骼是 1 */
        val axisScale: Float,
    ) {
        /**
         * 命中点距出光口的距离，**本地单位**（与 [LaserInfo.length] 同一把尺子）。
         *
         * 写回之后只被当成"这根管画多长"的**长度**用，绝不会被当成端点的位置 —— 光束的方向
         * 自始至终是本地 −Z，所以截断不会引起任何角度偏差。
         */
        var hitDistance: Double = 0.0

        /** 是否命中了方块（没命中就不截断、也不画落点光斑） */
        var hasHit: Boolean = false
    }
}
