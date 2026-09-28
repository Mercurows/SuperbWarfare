package com.atsuishio.superbwarfare.client.charm

import com.atsuishio.superbwarfare.client.charm.CharmRuntime.apply
import com.atsuishio.superbwarfare.client.charm.CharmRuntime.revert
import com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel
import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.attachment.CharmInfo
import com.atsuishio.superbwarfare.event.ClientEventHandler
import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 吊坠摆动运行时。
 *
 * 每帧在**第一人称**渲染 `Charm` 槽位配件时被调用一次，负责：
 *
 * 1. 把悬挂点从**视图空间**换算到「相机原点 + 世界轴」参考系（见 [CharmSolver] 的说明）；
 * 2. 按**固定步长**推进 [CharmSolver]；
 * 3. 在两次解算之间插值，得到本帧真正要用的摆角；
 * 4. 把绳方向换算回**附件模型局部空间**，算出绕摆点的旋转并写进 `string` / `charm` 两根骨骼。
 *
 * ## 为什么是固定步长 + 插值
 *
 * 物理本身**不需要**跟着渲染帧率跑：吊坠的自然频率只有一赫兹出头，30Hz 的步长已经足够精确，
 * 而高帧率下每帧算一次纯属浪费。固定步长还带来两个好处：
 *
 * - **帧率无关**：144fps 和 30fps 下摆动的快慢、幅度完全一致（变步长 Verlet 做不到这一点）；
 * - **更稳更顺**：步长恒定，积分误差不随帧率漂移；渲染侧对两次解算结果做球面插值，
 *   看上去是连续运动而不是"每帧跳一下"。
 *
 * 代价是**一个步长的延迟**（1/30 秒），对一个挂件来说无关紧要。
 *
 * ## 为什么状态挂在这里而不是模型上
 *
 * 附件模型实例是**全局共享**的（`AttachmentModelReloadListener` 按路径只存一份），
 * 所以摆动状态只能挂在"这一帧是谁在渲染"上 —— 也就是 `(手, 配件, 挂点骨骼)` 三元组。
 * 同一把枪两只手同时拿（`renderModel` 一帧跑两遍）时它们各有一份状态，不会互相踩。
 *
 * ## 只在第一人称
 *
 * 调用点只在 `transformType.firstPerson()` 且非阴影 pass 时进入；第三人称/掉落物/展示框/GUI
 * 一律**不推进**物理，吊坠保持绑定姿态垂下 —— 既符合"仅第一人称生效"的设定，
 * 也顺带避免了"别人手里的枪跟着我晃"。
 */
object CharmRuntime {

    /** 一个 (手, 配件, 挂点) 的摆动状态 */
    private class CharmState {
        val solver = CharmSolver()
        val params = CharmParams()

        /** 数据侧给定的响应系数（开镜时会被压小，所以要留一份原始值） */
        var baseResponse = 0.15f
        var aimResponse = 0.08f

        /** 上一次配置用的是哪份数据 / 哪个模型；变了就重算参数 */
        var configuredInfo: CharmInfo? = null
        var configuredLength = -1f

        var lastNanos = 0L

        /** 固定步长累加器（秒） */
        var accumulator = 0f

        /** 上一帧的相机世界坐标；子步之间在它和本帧之间线性插值 */
        var camX = 0.0
        var camY = 0.0
        var camZ = 0.0
        var hasCamera = false

        /** 上一次**解算**时用的悬挂点；子步之间同样线性插值到本帧采样值 */
        val anchorStep = Vector3f()
        val anchorInterpolated = Vector3f()
        var hasAnchor = false

        /**
         * 低通之后的悬挂点。
         *
         * ⚠ 走路/飞行时枪的步行摇晃、第一人称动画姿态里都混着 tick（20Hz）量化出来的台阶，
         * 而绳长只有一两厘米 —— 逐帧直接拿这些台阶当输入，摆角会以 20Hz 抖几十度。
         * 悬挂点先过一道一阶低通（时间常数见 [CharmInfo.smoothing]），台阶就没了，
         * 走路的摇晃本身（一点几赫兹）几乎不受影响。
         */
        val anchorFiltered = Vector3f()

        /** 最近两次解算出的摆角，渲染时在它们之间插值 */
        val previousSwing = Quaternionf()
        val currentSwing = Quaternionf()
        val outputSwing = Quaternionf()
        var hasSwing = false

        val anchorView = Vector3f()
        val anchorWorld = Vector3f()
        val direction = Vector3f()
        val swing = Quaternionf()
        val snapshot = CharmSnapshot()
    }

    private data class CharmKey(
        val hand: InteractionHand,
        val attachment: String,
        val mountBone: String,
    )

    private val states = HashMap<CharmKey, CharmState>()

    /**
     * 解算并把摆动姿态写进模型。
     *
     * @param model 附件模型（由它提供 `string` / `charm` 分组与摆点摆长）
     * @param definition 配件定义（提供 [CharmInfo] 手感参数）
     * @param mountBone 实际使用的挂点骨骼名，只用来区分状态
     * @param modelToView `poseStack.last().pose()`，**已经乘过挂点变换**，即"附件模型局部 → 视图空间"
     * @param worldFromView `GeoGunRenderer.cameraRotationInverse()`，即"视图空间 → 世界轴"（纯旋转）
     * @return 需要交给 [revert] 还原的快照；这个模型不是吊坠（没有那两个分组）时返回 `null`
     */
    @JvmStatic
    fun apply(
        model: BedrockAttachmentModel,
        definition: AttachmentDefinition,
        mountBone: String,
        modelToView: Matrix4f,
        worldFromView: Matrix4f,
        hand: InteractionHand,
    ): CharmSnapshot? {
        val rig = model.charmRig() ?: return null

        val key = CharmKey(hand, definition.getId(), mountBone)
        val state = states.getOrPut(key) { CharmState() }

        val info = definition.charmInfo()
        if (state.configuredInfo !== info || state.configuredLength != rig.length) {
            configure(state, info, rig.length)
        }

        val mc = Minecraft.getInstance()

        // 距上次推进太久（切 F5、开 GUI、暂停、换手）→ 重新归位，别让质点带着旧速度回来
        val now = System.nanoTime()
        if (now - state.lastNanos > RESET_GAP_NANOS) {
            state.solver.invalidate()
            state.accumulator = 0f
            state.hasCamera = false
            state.hasAnchor = false
        }
        state.lastNanos = now

        // 悬挂点：模型局部（摆点）→ 视图空间 → 世界轴。每帧只采一次、子步共用：
        // 一帧之内枪的姿态不会变，重复采样只会白算几遍矩阵。
        modelToView.transformPosition(rig.pivot, state.anchorView)
        worldFromView.transformPosition(state.anchorView, state.anchorWorld)

        val frameDt = (mc.deltaFrameTime / 20f).coerceIn(MIN_FRAME_DT, MAX_FRAME_DT)
        if (!state.hasAnchor) {
            state.anchorFiltered.set(state.anchorWorld)
            state.anchorStep.set(state.anchorWorld)
            state.hasAnchor = true
        } else {
            // 一阶低通：滤掉 tick 量化出来的台阶（走路/飞行时的高频抖动来源）
            val tau = state.params.smoothing
            val k = if (tau > 0f) 1f - exp(-frameDt / tau) else 1f
            state.anchorFiltered.lerp(state.anchorWorld, k)
        }

        // 开镜时把惯性响应压下去：瞄准时吊坠还在乱晃会干扰视线
        val zoom = ClientEventHandler.zoomTime.coerceIn(0.0, 1.0).toFloat()
        state.params.response = state.baseResponse * (1f - zoom) + state.aimResponse * zoom

        val camera = mc.gameRenderer.mainCamera
        val cameraPos = camera.position

        state.accumulator = (state.accumulator + frameDt).coerceAtMost(MAX_ACCUMULATOR)
        if (!state.hasCamera) {
            state.camX = cameraPos.x
            state.camY = cameraPos.y
            state.camZ = cameraPos.z
            state.hasCamera = true
        }

        val steps = (state.accumulator / SIM_STEP).toInt().coerceAtMost(MAX_STEPS)
        for (i in 1..steps) {
            // ⚠ 悬挂点与相机位置都要在"上一次解算 → 本帧采样"之间**线性铺开**分给各个子步。
            // 直接把本帧的值原样交给每一步是不行的：一帧跑几步时后面几步的位移会算成 0，
            // 而"一帧跑几步"取决于渲染帧率 —— 那会让摆幅跟着帧率变（高帧率下每步摊到的位移更小）。
            val t = i.toFloat() / steps
            state.anchorInterpolated.set(state.anchorStep).lerp(state.anchorFiltered, t)
            val ct = i.toDouble() / steps

            state.solver.update(
                state.anchorInterpolated,
                Mth.lerp(ct, state.camX, cameraPos.x),
                Mth.lerp(ct, state.camY, cameraPos.y),
                Mth.lerp(ct, state.camZ, cameraPos.z),
                state.params,
                SIM_STEP,
            )
            state.accumulator -= SIM_STEP

            // 解算结果 → 模型局部空间的旋转；两次解算之间由渲染侧插值
            state.previousSwing.set(state.currentSwing)
            if (solveSwing(state, rig, modelToView, worldFromView)) {
                state.currentSwing.set(state.swing)
                state.hasSwing = true
            }
        }

        // ⚠ 只有**真的推进过**才把基准挪到本帧：否则高帧率下连着好几帧不推进，
        // 基准却被逐帧推着走，下一次推进就只能看到最后一帧的位移 —— 摆幅又会跟着帧率变。
        if (steps > 0) {
            state.camX = cameraPos.x
            state.camY = cameraPos.y
            state.camZ = cameraPos.z
            state.anchorStep.set(state.anchorFiltered)
        }

        // 还没跑过任何一步（刚装上 / 刚切回第一人称）：这一帧先按绑定姿态画
        if (!state.hasSwing) return null

        val alpha = (state.accumulator / SIM_STEP).coerceIn(0f, 1f)
        state.outputSwing.set(state.previousSwing).slerp(state.currentSwing, alpha)
        rig.apply(model.instance, state.outputSwing, state.snapshot)
        return state.snapshot
    }

    /** 还原 [apply] 写进骨骼的摆动姿态（**必须**在画完之后调用） */
    @JvmStatic
    fun revert(model: BedrockAttachmentModel, snapshot: CharmSnapshot?) {
        val rig = model.charmRig() ?: return
        if (snapshot == null) return
        rig.restore(model.instance, snapshot)
    }

    /**
     * 解算出来的绳方向 → 模型局部空间的旋转，结果放在 `state.swing`。
     *
     * 方向要连过两次换基：世界轴 → 视图空间 → 附件模型局部（摆点就是模型局部空间的原点）。
     * 换基用的是**当前这一帧**的渲染矩阵，所以插值出来的角度会被画在正确的挂点姿态上。
     *
     * @return 方向退化（理论上不会发生）时返回 `false`，调用方保持上一次的结果
     */
    private fun solveSwing(
        state: CharmState,
        rig: CharmRig,
        modelToView: Matrix4f,
        worldFromView: Matrix4f,
    ): Boolean {
        state.direction.set(state.solver.rope)
        VIEW_FROM_WORLD.set(worldFromView).invert().transformDirection(state.direction)
        MODEL_FROM_VIEW.set(modelToView).invert().transformDirection(state.direction)
        if (state.direction.lengthSquared() < 1.0e-8f) return false
        state.direction.normalize()

        clampLateral(state.direction, state.params.lateralLimitSin)

        // 从"绑定垂下方向"转到"解算方向"的最短弧旋转，就是两根分组骨骼要绕摆点转的角度
        state.swing.rotationTo(rig.restDir, state.direction)
        return true
    }

    /**
     * 侧向限位：把绳方向在模型局部 X 上的分量钳到 [limitSin]，Y/Z 等比缩放保持单位长度。
     *
     * 只卡一个方向（[limitSin] 的符号决定是哪一边），因为吊坠只挂在枪身**一侧**：
     * 朝枪身那一边摆会扫进模型里，朝外侧摆是自由的（上限由 `MaxAngle` 的锥形管）。
     *
     * 钳的是**方向**而不是物理状态：解算在重力参考系里跑，模型局部的约束在那里表达不出来。
     * 表现上就是"贴住一面斜墙滑动"——顶住时角度停住、不抖，推力消失后自然弹回来。
     */
    private fun clampLateral(direction: Vector3f, limitSin: Float) {
        if (limitSin == 0f) return
        val x = direction.x
        if (limitSin > 0f) {
            if (x <= limitSin) return
        } else {
            if (x >= limitSin) return
        }

        val rest = sqrt((1f - limitSin * limitSin).coerceAtLeast(0f))
        val yz = sqrt(direction.y * direction.y + direction.z * direction.z)
        if (yz > 1.0e-6f) {
            val k = rest / yz
            direction.y *= k
            direction.z *= k
        } else {
            // 退化：本来几乎就是正上/正下方，直接给个正下方
            direction.y = -rest
            direction.z = 0f
        }
        direction.x = limitSin
    }

    /** 数据或模型换了：把 [CharmInfo] 摊平成解算用的标量 */
    private fun configure(state: CharmState, info: CharmInfo, length: Float) {
        state.configuredInfo = info
        state.configuredLength = length

        val params = state.params
        params.length = length
        val gravity = info.gravity
        if (gravity != null) {
            // 显式指定了等效重力就用它（想要"真实重力"的写法）
            params.gravity = gravity.toFloat()
        } else {
            params.applyFrequency(info.frequency)
        }
        params.damping = info.damping.coerceAtLeast(0.0).toFloat()
        params.setMaxAngleDegrees(info.maxAngle)
        params.idleSway = Math.toRadians(info.idleSway).toFloat()
        params.smoothing = info.smoothing.coerceAtLeast(0.0).toFloat()

        val lateral = info.lateralAngle.coerceIn(-89.0, 89.0)
        params.lateralLimitSin = if (lateral == 0.0) 0f else sin(Math.toRadians(lateral)).toFloat()

        state.baseResponse = info.response.coerceIn(0.0, 1.0).toFloat()
        state.aimResponse = info.aimResponseScale.coerceIn(0.0, 1.0).toFloat()
        state.params.response = state.baseResponse

        // 参数变了（尤其是摆长）就重新归位，免得旧速度配上新摆长直接甩飞
        state.solver.invalidate()
        state.accumulator = 0f
        state.hasAnchor = false
    }

    /** 视图空间 → 世界轴 的临时矩阵，复用它避免每帧分配 */
    private val VIEW_FROM_WORLD = Matrix4f()

    /** 模型局部 → 视图空间 的逆矩阵，同上 */
    private val MODEL_FROM_VIEW = Matrix4f()

    /** 固定物理步长（秒）。30Hz 对一两赫兹的摆锤绰绰有余 */
    private const val SIM_STEP = 1f / 30f

    /** 单帧时长下限/上限（秒）：卡顿或调试停帧时不让一步跨太大 */
    private const val MIN_FRAME_DT = 1f / 240f
    private const val MAX_FRAME_DT = 0.1f

    /** 累加器上限（秒）：一次卡顿最多补三步，不做死亡螺旋 */
    private const val MAX_ACCUMULATOR = 0.1f

    /** 单帧最多跑几步 */
    private const val MAX_STEPS = 3

    /** 两帧间隔超过它就重新归位（纳秒） */
    private const val RESET_GAP_NANOS = 250_000_000L
}
