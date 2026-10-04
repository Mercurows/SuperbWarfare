package com.atsuishio.superbwarfare.client.renderer.laser

import com.atsuishio.superbwarfare.client.renderer.laser.LaserSightCapture.beginFrame
import com.atsuishio.superbwarfare.client.renderer.laser.LaserSightCapture.capture
import com.atsuishio.superbwarfare.data.attachment.LaserInfo
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f

/**
 * 第一人称全部激光束的**本帧**捕获槽。
 *
 * 一束激光的生命周期就是一次 `GeoGunRenderer.renderModel`（本地玩家第一人称）：
 * [beginFrame] 清空 → 画每件激光配件时 [capture] 采一束 → [LaserSightRenderer] 在**同一次调用**
 * 的末尾整批画掉。
 *
 * ⚠ 这里**不能**有任何跨帧状态。上一版把束按 `(手, 槽位)` 存进一张表、靠"谁采集谁续期"淘汰，
 * 但续期那一句写在 `getOrPut` 的默认值里（只在新建时跑一次），于是束在创建后 3 tick 就被当成
 * 过期淘汰、下一帧又用零值重建 —— 表现就是激光每 4 tick（约 5 Hz）闪一下，中间还夹着几帧
 * "从枪口射向自己眼睛"的乱束。生命周期收到一次绘制里之后，这类问题从结构上不存在。
 *
 * 坐标一律是**视图空间**：SBM 第一人称的 `poseStack` 就是"模型 → 视图空间"，
 * 相机在原点、−Z 是视线方向（见 `BedrockBoneCoordinateTool` 的说明）。
 * 世界坐标只在做射线的那一刻现算，不缓存。
 */
object LaserSightCapture {

    private val beams = ArrayList<Beam>(4)

    /** 一次 `renderModel` 开始：上一帧的束全部作废，本次调用重新采集 */
    @JvmStatic
    fun beginFrame() {
        beams.clear()
    }

    /**
     * 采一束激光的出光口与方向。
     *
     * [poseMatrix] 必须是**画该配件几何用的那一份** `poseStack.last().pose()`
     * （已经乘过挂点骨骼与配件自己的旋转），[locatorTransform] 是配件模型里出光口 locator 的
     * 全局变换；两者相乘就是"出光口 → 视图空间"，与画面同源，所以这里不再另拼一遍
     * `poseStack × mount × rotation`（变换次序一旦与渲染路径分叉，出光口就会离骨骼一截）。
     *
     * 方向取出光口的局部 **−Z**：Blockbench 约定模型正前方是 −Z，而四条导轨的挂点骨骼都是绕 Z 转
     * 的（`upper_rail_pos` 是 `[0,0,-180]`、左右导轨是 `±90`，个别枪的下导轨只有 1° 俯仰修正），
     * Z 轴不受影响，所以这个方向自动等于枪械正前方，不需要按槽位特判。
     *
     * @return 采集到的束；配件模型里没有那个 locator、或变换退化时返回 `null`（什么都不画）
     */
    @JvmStatic
    fun capture(
        info: LaserInfo,
        colorRgb: Int,
        poseMatrix: Matrix4f,
        locatorTransform: Matrix4f?,
    ): Beam? {
        if (locatorTransform == null) return null

        val matrix = Matrix4f(poseMatrix).mul(locatorTransform)
        val origin = matrix.getTranslation(Vector3f())
        // 方向 = 矩阵作用在局部 **−Z** 上的像。
        //
        // ⚠ 这里必须走"方向变换"这条 API，**不能**手写 `-m02/-m12/-m22`：JOML 的字段名是
        // `m<列><行>`（`m30/m31/m32` 是平移，`RenderDistanceHelper` 就是按这个用的；
        // `SyncedEntityWorldRenderer` 也从 `m22`/`m32` 反推透视投影的 near/far），所以
        // **第三列**（局部 +Z 的像）是 `(m20, m21, m22)`，而 `(m02, m12, m22)` 是第三**行** ——
        // 那是把"模型空间 → 视图空间"的矩阵转置着用，等于把模型空间的分量当成视图空间的方向。
        // 后果正是：只有挂点没带旋转的下导轨看着是对的（θ=0 时那一项退化掉了），
        // 上/左/右导轨像被"跟着挂点转了 90°*n"；而枪一换弹 / 进改装（姿态一动）连方向都不对了。
        val direction = matrix.transformDirection(0f, 0f, -1f, Vector3f())
        if (direction.lengthSquared() < 1e-12f) return null
        direction.normalize()

        return Beam(
            info = info,
            colorRgb = colorRgb,
            viewOrigin = Vec3(origin.x.toDouble(), origin.y.toDouble(), origin.z.toDouble()),
            viewDirection = Vec3(direction.x.toDouble(), direction.y.toDouble(), direction.z.toDouble()),
        ).also { beams += it }
    }

    /** 本帧要画的全部束（一把枪上四根导轨各装一件时就是四束） */
    @JvmStatic
    fun beams(): List<Beam> = beams

    /**
     * 一束激光。
     *
     * 出光口 / 方向 / 颜色在 [capture] 里一次定死，**对象每帧新建、不做跨帧复用**：
     * 复用对象时只要漏写一个字段，上一帧的值就会留在画面上，那是比每帧几个小对象昂贵得多的 bug。
     * [viewEnd] 与 [hasHit] 由 [LaserSightRenderer] 在同一帧里做完射线后写回。
     */
    class Beam(
        /** 这件配件的激光配置（射程、粗细都在里面） */
        val info: LaserInfo,

        /** 最终颜色（配件 tag 覆盖已由调用方合成） */
        val colorRgb: Int,

        /** 出光口（视图空间） */
        val viewOrigin: Vec3,

        /** 出光方向（视图空间，单位向量） */
        val viewDirection: Vec3,
    ) {
        /** 落点（视图空间）：命中方块时是命中点，没命中时是射程末端 */
        var viewEnd: Vec3 = viewOrigin

        /** 是否命中方块（没命中就不画落点光斑） */
        var hasHit: Boolean = false
    }
}
