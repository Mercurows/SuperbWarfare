package com.atsuishio.superbwarfare.client.charm

import com.atsuishio.superbwarfare.client.charm.CharmRuntime.apply
import com.atsuishio.superbwarfare.client.charm.CharmRuntime.revert
import com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel
import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.attachment.CharmInfo
import com.atsuishio.superbwarfare.event.ClientEventHandler
import net.minecraft.client.Minecraft
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
 * 1. 采集悬挂点在**视图空间**的位置，并算出本帧的相机运动量（[CharmFrame]）；
 * 2. 按**固定步长**推进 [CharmSolver]；
 * 3. 在两次解算之间插值，得到本帧真正要用的摆角；
 * 4. 把绳方向换算回**附件模型局部空间**，算出绕摆点的旋转并写进 `string` / `charm` 两根骨骼。
 *
 * 解算整个跑在视图空间（相机原点 + 相机轴）里，理由见 [CharmSolver] 的说明：悬挂点在那个
 * 参考系里相对相机是**静止**的，于是"转头"与"枪自己动"能被彻底拆开，各自用正确的方式驱动。
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
 * ⚠ 相机运动量（[CharmFrame]）反过来**必须按帧算**：它靠"这一帧和上一帧的差分"得到，
 * 一帧之内根本没有变化，按子步算只会得到一串零和一帧一次的尖峰。
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

        /** 本帧的相机运动量；一帧算一次、各个子步共用 */
        val frame = CharmFrame()

        /** 数据侧给定的响应系数（开镜时会被压小，所以要留一份原始值） */
        var baseResponse = 0.15f
        var baseTurnResponse = 0.3f
        var aimResponse = 0.08f

        /** 上一次配置用的是哪份数据 / 哪个模型；变了就重算参数 */
        var configuredInfo: CharmInfo? = null
        var configuredLength = -1f

        var lastNanos = 0L

        /** 固定步长累加器（秒） */
        var accumulator = 0f

        /** 上一**帧**的相机世界坐标；相机线加速度是它的帧间差分 */
        var camX = 0.0
        var camY = 0.0
        var camZ = 0.0

        /**
         * 相机世界速度（方块/秒）的一阶低通与上一帧的值。
         *
         * ⚠ 为什么非低通不可：玩家坐标是**按 tick 插值**的折线，它的导数是阶梯状的，
         * 二阶差分因此在每个 tick 边界上打一个脉冲。直接拿它当惯性伪力，就是一串 20Hz 的踢 ——
         * 这正是走路/飞行时吊坠高频抖动的来源。低通之后再差分，剩下的才是"真在加速"那部分。
         */
        val camVelocity = DoubleArray(3)
        val camVelocityPrev = DoubleArray(3)
        var hasCamera = false

        /**
         * 「视图 ← 世界」旋转（[apply] 收到的那个矩阵的逆）。
         *
         * 重力方向、相机线加速度、角速度全都要在这套轴向里表达，所以每帧备一份。
         */
        val viewFromWorld = Matrix4f()

        /** 上一帧的 [viewFromWorld]，用来差分角速度 */
        val rotationPrev = Matrix4f()
        var hasRotation = false

        /** 低通之后的角速度（视图空间）与上一帧的值，后者用来差分出角加速度 */
        val omega = Vector3f()
        val omegaPrev = Vector3f()

        /** 上一次**解算**时用的悬挂点；子步之间线性插值到本帧采样值 */
        val anchorStep = Vector3f()
        val anchorInterpolated = Vector3f()
        var hasAnchor = false

        /**
         * 低通之后的悬挂点（**视图空间**）。
         *
         * ⚠ 走路/飞行时枪的步行摇晃、第一人称动画姿态里都混着 tick（20Hz）量化出来的台阶，
         * 而绳长只有一两厘米 —— 逐帧直接拿这些台阶当输入，摆角会以 20Hz 抖几十度。
         * 悬挂点先过一道一阶低通（时间常数见 [CharmInfo.smoothing]），台阶就没了，
         * 走路的摇晃本身（一点几赫兹）几乎不受影响。
         *
         * 顺带一提：在视图空间里做这道低通是**安全**的 —— 悬挂点不随转头移动，
         * 低通不会像旧版（世界轴参考系）那样把"转头的圆弧"也一起拖出滞后。
         */
        val anchorFiltered = Vector3f()

        /** 最近两次解算出的摆角，渲染时在它们之间插值 */
        val previousSwing = Quaternionf()
        val currentSwing = Quaternionf()
        val outputSwing = Quaternionf()
        var hasSwing = false

        val anchorView = Vector3f()
        val direction = Vector3f()

        /** 模型局部 +X → 视图空间 的草稿（软限位的侧别参考轴） */
        val sideAxisLocal = Vector3f()

        val swing = Quaternionf()
        val rawOmega = Vector3f()
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
     * @param worldFromView `GeoGunRenderer.cameraRotationInverse()`，即"视图空间 → 世界轴"（纯旋转）；
     *   这里取它的逆用（解算在视图空间里跑，见 [CharmSolver]）
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
            state.hasRotation = false
            state.hasAnchor = false
            // ⚠ 速度基线也要一起清掉：留着一整段"没渲染的时间"之前的速度去差分，
            // 下一帧会算出一个巨大的加速度（走了两步、参考点却停在原地）。
            state.camVelocity.fill(0.0)
            state.camVelocityPrev.fill(0.0)
            state.omega.set(0f, 0f, 0f)
            state.omegaPrev.set(0f, 0f, 0f)
        }
        state.lastNanos = now

        // 悬挂点：模型局部（摆点）→ 视图空间。每帧只采一次、子步共用：
        // 一帧之内枪的姿态不会变，重复采样只会白算几遍矩阵。
        // 「视图 ← 世界」也在这里备好，重力/角速度/相机加速度都要用它换基。
        state.viewFromWorld.set(worldFromView).invert()
        modelToView.transformPosition(rig.pivot, state.anchorView)

        // 模型局部 +X（= 用 [CharmInfo.lateralAngle] 声明"枪身侧"时用的那条尺子）转到视图空间，
        // 交给解算器区分"枪身侧/无遮挡侧"（见 `CharmSolver.swingLimit`）。
        // ⚠ 必须归一化：挂点变换里带着缩放（手臂锚点就是这么带 scale 的）。
        // ⚠ 和 [clampLateral] 用的是**同一个** `modelToView`，两边对"左右"的定义天然一致。
        modelToView.transformDirection(1f, 0f, 0f, state.sideAxisLocal)
        if (state.sideAxisLocal.lengthSquared() < 1.0e-8f) {
            state.frame.sideAxis.set(1f, 0f, 0f)
        } else {
            state.frame.sideAxis.set(state.sideAxisLocal).normalize()
        }

        val frameDt = (mc.deltaFrameTime / 20f).coerceIn(MIN_FRAME_DT, MAX_FRAME_DT)
        if (!state.hasAnchor) {
            state.anchorFiltered.set(state.anchorView)
            state.anchorStep.set(state.anchorView)
            state.hasAnchor = true
        } else {
            // 一阶低通：滤掉 tick 量化出来的台阶（走路/飞行时的高频抖动来源）
            val tau = state.params.smoothing
            val k = if (tau > 0f) 1f - exp(-frameDt / tau) else 1f
            state.anchorFiltered.lerp(state.anchorView, k)
        }

        // 开镜时把响应压下去：瞄准时吊坠还在乱晃会干扰视线
        val zoom = ClientEventHandler.zoomTime.coerceIn(0.0, 1.0).toFloat()
        state.params.response = state.baseResponse * (1f - zoom) + state.aimResponse * zoom
        state.params.turnResponse = state.baseTurnResponse * (1f - zoom) + state.aimResponse * zoom

        val cameraPos = mc.gameRenderer.mainCamera.position
        // ⚠ 相机位置基线必须在 [updateFrame] **之前**立好：它是"帧间位移"的零点，
        // 缺了它第一次差分就会拿 (0,0,0) 当上一帧位置，得到一个上万方块/秒的速度。
        if (!state.hasCamera) {
            state.camX = cameraPos.x
            state.camY = cameraPos.y
            state.camZ = cameraPos.z
            state.hasCamera = true
        }
        updateFrame(state, frameDt, cameraPos.x, cameraPos.y, cameraPos.z)

        state.accumulator = (state.accumulator + frameDt).coerceAtMost(MAX_ACCUMULATOR)
        val steps = (state.accumulator / SIM_STEP).toInt().coerceAtMost(MAX_STEPS)
        for (i in 1..steps) {
            // ⚠ 悬挂点要在"上一次解算 → 本帧采样"之间**线性铺开**分给各个子步。
            // 直接把本帧的值原样交给每一步是不行的：一帧跑几步时后面几步的位移会算成 0，
            // 而"一帧跑几步"取决于渲染帧率 —— 那会让摆幅跟着帧率变（高帧率下每步摊到的位移更小）。
            val t = i.toFloat() / steps
            state.anchorInterpolated.set(state.anchorStep).lerp(state.anchorFiltered, t)

            state.solver.update(state.anchorInterpolated, state.frame, state.params, SIM_STEP)
            state.accumulator -= SIM_STEP

            // 解算结果 → 模型局部空间的旋转；两次解算之间由渲染侧插值
            state.previousSwing.set(state.currentSwing)
            if (solveSwing(state, rig, modelToView)) {
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

    /**
     * 采集本帧的相机运动量（[CharmFrame]）。每帧**一次**，各个子步共用。
     *
     * 三个量都在视图空间里表达：
     *
     * - **重力**：世界的"向下"被相机转过来。相机抬头时它跟着偏，于是吊坠吊的仍然是世界的下；
     * - **线加速度**：相机世界坐标先低通成速度、再差分；这是"起步往后甩"的全部来源；
     * - **角速度**：由「视图 ← 世界」的帧间差得到（推导见 [CharmFrameMath.angularVelocity]），
     *   角加速度再差分一次。**匀速转身时角加速度为 0、离心力恒定**，这正是"匀速转身往前飘"。
     */
    private fun updateFrame(state: CharmState, frameDt: Float, camX: Double, camY: Double, camZ: Double) {
        val frame = state.frame
        val p = state.params
        val tau = p.smoothing
        val k = if (tau > 0f) 1f - exp(-frameDt / tau) else 1f

        // 重力（视图空间）
        frame.gravity.set(0f, -p.gravity, 0f)
        state.viewFromWorld.transformDirection(frame.gravity)

        // 相机线加速度（世界轴）→ 视图空间
        if (state.hasCamera) {
            state.camVelocity[0] += ((camX - state.camX) / frameDt - state.camVelocity[0]) * k
            state.camVelocity[1] += ((camY - state.camY) / frameDt - state.camVelocity[1]) * k
            state.camVelocity[2] += ((camZ - state.camZ) / frameDt - state.camVelocity[2]) * k
            frame.camAccel.set(
                ((state.camVelocity[0] - state.camVelocityPrev[0]) / frameDt).toFloat(),
                ((state.camVelocity[1] - state.camVelocityPrev[1]) / frameDt).toFloat(),
                ((state.camVelocity[2] - state.camVelocityPrev[2]) / frameDt).toFloat(),
            )
            state.camVelocityPrev[0] = state.camVelocity[0]
            state.camVelocityPrev[1] = state.camVelocity[1]
            state.camVelocityPrev[2] = state.camVelocity[2]
        } else {
            // 归位后的第一步：不知道上一刻的速度，就把当前速度当基线、伪力记 0 ——
            // 否则"走动中被重置"会凭空吃到一次和走路速度等量的踢。（[apply] 会先立好位置基线，
            // 这里只是兜底。）
            state.camVelocity[0] = (camX - state.camX) / frameDt
            state.camVelocity[1] = (camY - state.camY) / frameDt
            state.camVelocity[2] = (camZ - state.camZ) / frameDt
            state.camVelocityPrev[0] = state.camVelocity[0]
            state.camVelocityPrev[1] = state.camVelocity[1]
            state.camVelocityPrev[2] = state.camVelocity[2]
            frame.camAccel.set(0f, 0f, 0f)
            state.hasCamera = true
        }
        // ⚠ 传送/维度切换/调试停帧会让"一帧位移"大到离谱，差分出来的加速度能上百倍重力。
        // 夹一下上限（远高于正常游玩：走路起步约 20、载具急加速约 50），
        // 免得偶尔一次跳变把吊坠甩到限位上去。
        if (frame.camAccel.lengthSquared() > MAX_CAM_ACCEL * MAX_CAM_ACCEL) {
            frame.camAccel.normalize(MAX_CAM_ACCEL)
        }
        state.viewFromWorld.transformDirection(frame.camAccel)
        state.camX = camX
        state.camY = camY
        state.camZ = camZ

        // 相机角速度（视图空间）→ 低通 → 角加速度
        if (state.hasRotation) {
            CharmFrameMath.angularVelocity(state.rotationPrev, state.viewFromWorld, frameDt, state.rawOmega)
            // 低通的作用和相机速度那道一样：把鼠标/window 事件带来的逐帧抖动抹平。
            // 它保住的是**冲量**（∫α dt = Δω），只是把一个尖峰摊成 0.06 秒的斜坡 —— 反而更好积分。
            state.omega.lerp(state.rawOmega, k)
            frame.alpha.set(state.omega).sub(state.omegaPrev).mul(1f / frameDt)
            state.omegaPrev.set(state.omega)
        } else {
            state.omega.set(0f, 0f, 0f)
            state.omegaPrev.set(0f, 0f, 0f)
            frame.alpha.set(0f, 0f, 0f)
            state.hasRotation = true
        }
        frame.omega.set(state.omega)
        state.rotationPrev.set(state.viewFromWorld)
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
     * 解算已经跑在视图空间里，所以只需要**一次**换基：视图空间 → 附件模型局部
     * （摆点就是模型局部空间的原点）。换基用的是**当前这一帧**的渲染矩阵，
     * 所以插值出来的角度会被画在正确的挂点姿态上。
     *
     * @return 方向退化（理论上不会发生）时返回 `false`，调用方保持上一次的结果
     */
    private fun solveSwing(state: CharmState, rig: CharmRig, modelToView: Matrix4f): Boolean {
        state.direction.set(state.solver.rope)
        MODEL_FROM_VIEW.set(modelToView).invert().transformDirection(state.direction)
        if (state.direction.lengthSquared() < 1.0e-8f) return false
        state.direction.normalize()

        clampLateral(state.direction, state.params.lateralLimitSin)

        // ⚠ 最短弧旋转在 `direction ≈ −restDir` 处退化（叉积为零，"最短弧"没有定义）：
        // 方向在它附近来回抖就会逐帧翻面。开侧上限放宽到 90° 之后，硬夹逼**不再是**充分的保护 ——
        // 夹逼锥的轴在模型局部是"挂点变换求逆后的世界下方向"，而挂点会被收枪/冲刺/开镜动画转走，
        // 余量（`180° − β − 上限`）会缩水。这里干脆放弃这一帧的更新、保持上一次的结果，
        // 比翻面好看得多，而且方向一旦离开就会自己接上。
        if (state.direction.dot(rig.restDir) < -0.999f) return false

        // 从"绑定垂下方向"转到"解算方向"的最短弧旋转，就是两根分组骨骼要绕摆点转的角度
        state.swing.rotationTo(rig.restDir, state.direction)
        return true
    }

    /**
     * 侧向限位：把绳方向在模型局部 X 上的分量钳到 [limitSin]，Y/Z 等比缩放保持单位长度。
     *
     * 只卡一个方向（[limitSin] 的符号决定是哪一边），因为吊坠只挂在枪身**一侧**：
     * 朝枪身那一边摆会扫进模型里，朝外侧摆是自由的（上限交给 `MaxAngleFree` 与软限位）。
     *
     * 钳的是**方向**而不是物理状态：解算在视图空间里跑，模型局部的约束在那里表达不出来。
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
        // ⚠ 顺序有意义：开侧上限要钳到"不小于枪身侧"，所以先设枪身侧
        params.setMaxAngleDegrees(info.maxAngle)
        params.setMaxAngleFreeDegrees(info.maxAngleFree)
        params.limitStiffness = info.limitStiffness.coerceIn(0.0, MAX_LIMIT_STIFFNESS).toFloat()
        params.idleSway = Math.toRadians(info.idleSway).toFloat()
        params.smoothing = info.smoothing.coerceAtLeast(0.0).toFloat()

        val lateral = info.lateralAngle.coerceIn(-89.0, 89.0)
        params.lateralLimitSin = if (lateral == 0.0) 0f else sin(Math.toRadians(lateral)).toFloat()
        // 侧别跟着 [lateralLimitSin] 的符号走：0 = 没声明，开侧上限不生效（见 `CharmSolver.swingLimit`）
        params.blockedAxisSign = when {
            params.lateralLimitSin > 0f -> 1f
            params.lateralLimitSin < 0f -> -1f
            else -> 0f
        }

        state.baseResponse = info.response.coerceIn(0.0, 1.0).toFloat()
        state.baseTurnResponse = info.turnResponse.coerceIn(0.0, 1.0).toFloat()
        state.aimResponse = info.aimResponseScale.coerceIn(0.0, 1.0).toFloat()
        state.params.response = state.baseResponse
        state.params.turnResponse = state.baseTurnResponse

        // 参数变了（尤其是摆长）就重新归位，免得旧速度配上新摆长直接甩飞
        state.solver.invalidate()
        state.accumulator = 0f
        state.hasAnchor = false
    }

    /** 模型局部 → 视图空间 的逆矩阵，复用它避免每帧分配 */
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

    /** 相机线加速度上限（方块/秒²，约 5g）：只用来挡住传送这类异常的帧间跳变 */
    private const val MAX_CAM_ACCEL = 50f

    /**
     * 软限位强度上限。再大，壁面附近的等效频率 `ω_eff ≈ sqrt(g/L · 2k/上限)` 就逼近
     * Verlet 的稳定边界（`ω·dt < 2`），30Hz 下摆动会开始发颤。
     */
    private const val MAX_LIMIT_STIFFNESS = 8.0

    /** 两帧间隔超过它就重新归位（纳秒） */
    private const val RESET_GAP_NANOS = 250_000_000L
}
