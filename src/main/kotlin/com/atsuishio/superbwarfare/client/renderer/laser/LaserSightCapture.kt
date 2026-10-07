package com.atsuishio.superbwarfare.client.renderer.laser

import com.atsuishio.superbwarfare.data.attachment.LaserInfo
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f

object LaserSightCapture {

    private val beams = ArrayList<Beam>(4)

    /** 一次 `renderModel` 开始：上一帧的束全部作废，本次调用重新采集 */
    @JvmStatic
    fun beginFrame() {
        beams.clear()
    }

    /**
     * 采一束激光的出光口
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

    class Beam(
        /** 这件配件的激光配置（长度、粗细、颜色都在里面） */
        val info: LaserInfo,

        /** 最终颜色（配件 tag 覆盖已由调用方合成） */
        val colorRgb: Int,

        /** 是否第一人称：选长度 / 宽度的一组，并且只有第一人称做方块射线 */
        val firstPerson: Boolean,

        /** "出光口 → 本次渲染空间"的合成矩阵（`poseMatrix × locatorTransform`）*/
        val emitterMatrix: Matrix4f,

        /** 出光口（渲染空间）。**只给射线用** */
        val viewOrigin: Vec3,

        /** 出光方向（渲染空间，单位向量）。**只给射线用** */
        val viewDirection: Vec3,

        /** 矩阵沿出光轴的缩放：本地长度 × 它 = 世界米。正常骨骼是 1 */
        val axisScale: Float,
    ) {
        /**
         * 命中点**投影到光束轴**之后、走出光口量出去的长度，本地单位（世界米 ÷ [axisScale]）。
         *
         * 射线是`LaserSightRenderer.castFirstPersonBeams` 从**玩家眼睛**打出去的，和这条光束平行、
         * 错开半米，所以不能拿眼睛到命中点的距离直接用，见那边的注释。
         */
        var hitDistance: Double = 0.0

        /** 是否命中了东西（方块或实体，弹射物不算；没命中就不截断、也不画落点光斑） */
        var hasHit: Boolean = false
    }
}
