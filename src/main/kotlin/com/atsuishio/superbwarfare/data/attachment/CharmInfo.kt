package com.atsuishio.superbwarfare.data.attachment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 吊坠（`AttachmentDefinition.Charm`）。
 *
 * 吊坠模型按 `fixed` / `string` / `charm` 三组约定制作（组名见
 * [AttachmentSlots.Bones.CHARM_FIXED] 等），后两组会随玩家运动与视角晃动摆动。
 * 这里只放**手感参数**，几何量（摆点、摆长、静止方向）一律从模型自身推导：
 *
 * | 量 | 来源 |
 * |---|---|
 * | 摆点（旋转中心） | `string` 分组绑定包围盒的**顶部中心**（绳子挂在枪上的那一点） |
 * | 摆长 `L` | `string` 分组绑定包围盒的高度（绳长：挂点到挂件的距离） |
 * | 静止方向 | 世界重力方向（`(0, -1, 0)`），**不是**模型的绑定朝向 |
 *
 * 也就是说：美术只要把绳子竖直向下、本体吊在绳子末端，代码就不需要任何额外参数。
 * 想让摆点落在别处就改模型（绳子从哪儿垂下来，包围盒顶部就在哪儿），这里没有手写覆盖。
 *
 * ```jsonc
 * // sbw/attachments/charm_fukamizu_fish.json
 * {
 *   "Slot": "Charm",
 *   "Charm": { "Response": 0.1 },   // 只写想改的那几项
 *   "Model": "...", "Texture": "..."
 * }
 * ```
 *
 * ## 摆幅是怎么定下来的
 *
 * 摆角是**加速度**的函数（`tan(θ) ≈ a / g`，`g` 是等效重力），而吊坠的绳长往往只有一两厘米
 * （当前这条鱼是 0.019 方块），所以游戏里那点加速度都比等效重力大得多，两个系数就是用来
 * 把它们按比例压回好看的量级的：
 *
 * | 运动 | 走哪个系数 | 观感 |
 * |---|---|---|
 * | 转身（离心力 + 角加速度） | [turnResponse] | 匀速转身往前飘，起转/停转时甩向切向 |
 * | 平移（起步、急停、坐载具） | [response] | 起步往后甩、停下荡回来 |
 * | 枪身动画（走路摇晃、收枪、开镜、后坐） | [response] 的"携带"项 | 枪自己晃，吊坠跟着荡 |
 *
 * 摆角上限只有**朝枪身侧**那一个方位是紧的（[maxAngle]），其余方位 —— 无遮挡侧、正前、正后 ——
 * 一律按 [maxAngleFree] 放到最宽；[limitStiffness] 让回正力随偏离增长 —— 越接近上限越难推；
 * [frequency]/[gravity] 管的是"摆得快不快、回弹利不利索"。
 *
 * @param frequency 自然摆动频率（Hz），默认 1.5（周期约 0.67 秒）。
 *   **与摆长无关**：等效重力由 `g = L·(2πf)²` 反推，所以换一个更大/更小的吊坠模型观感不变。
 *   这是刻意的 —— 摆长来自模型，用它直接套真实重力时小吊坠会以三四赫兹高频抖，像在发颤而不是在晃。
 *   它同时决定了**回弹的快慢**：摆得越慢，一次甩动的余摆拖得越长。
 * @param gravity 显式指定等效重力（方块/秒²）；`null`（默认）= 用 [frequency] 反推。
 *   想要"真实重力"就写 `9.8`（代价见 [frequency] 的说明），想更飘就写小一点。
 * @param damping 速度阻尼（1/秒），默认 4.0。相对速度按 `exp(-damping · dt)` 衰减。
 *   阻尼比 `ζ = damping / (2·2πf)`，小于 1 才会**振荡**（默认值配上 1.5Hz 约 0.21，
 *   每摆一次衰减到 26%）。不想让它荡就往上写，20 以上基本"黏在枪上"；
 *   **想要更明显的余摆往下写**：6 是两三下就停，4 能荡三四下，3 以下会像钟摆一样晃很久。
 *   ⚠ 旧版这里写 6 其实看不出区别 —— 那时的阻尼是乘在"相对速度"上的
 *   （每步再乘一次 response），摆动的动量每步只剩一成，根本进不了振荡区。
 * @param response 惯性响应系数（0..1），默认 **0.15**。
 *   **1 = 完全物理**（吊坠把枪的运动完全甩在身后），**0 = 完全跟枪**（挂在枪上不动，
 *   只在重力下垂直下垂 —— 它也是"携带权重"，`0` 时枪的每个位移都会原样带着吊坠走）。
 *   这一路管的是**平移**（起步/急停/载具）与**枪身动画**的幅度：初版用 0.6，玩家稍快走两步
 *   就会被甩满，现在压到四分之一左右。
 *   ⚠ 转身那一路**不归它管**，见 [turnResponse]；想让吊坠彻底不动就两个都写 0。
 * @param turnResponse 转身响应系数（0..1），默认 **0.30**：缩放"转身"那一路的伪力
 *   （离心力 + 欧拉力 + 科里奥利力）。
 *
 *   和 [response] 分开，是因为两者要的量级不一样：平移那一路 0.15 已经够，
 *   而转身的离心力要 0.3 上下才能在正常转速下看到明显的"往前飘"。
 *
 *   估算摆角：匀速转身 `tan(θ) ≈ turnResponse · ω² · R / g`，
 *   其中 `R` 是挂件到相机的水平距离（约 0.5 方块）、`g` 是等效重力（默认约 1.67）。
 *   ω = 2 rad/s（约 115°/秒）时：0.15 → 10°、0.3 → 20°、0.5 → 31°、0.7 → 40°（顶到 [maxAngle]）。
 *   想更夸张就往上写，但超过约 0.6 之后正常转身也会被顶在最大摆角上。
 * @param maxAngle 最大摆角（度），默认 35。它只限制**朝枪身侧**（[lateralAngle] 指向的那一侧）
 *   的偏离，以**世界下方**为 0°，防止吊坠甩进枪身/手臂里。
 *
 *   ⚠ 它已经不是"整圈的圆锥"了：过渡是**单边**的 —— 偏离方向带枪身侧分量时才从 [maxAngleFree]
 *   往这个值收，落在正前、正后或另一侧时一概保持 [maxAngleFree]。只在没写 [lateralAngle]
 *   （等于没声明哪一侧被挡住）时，它才是整圈的对称上限。
 * @param maxAngleFree **非枪身侧**的最大摆角（度），默认 90：无遮挡侧、正前、正后都用它。
 *
 *   吊坠挂在枪身一侧，只有朝枪身摆才会扫进模型里，朝别的方向都是空的 —— 那些方向的上限
 *   可以放到 90°。哪一侧是"枪身侧"由 [lateralAngle] 的符号决定；
 *   [lateralAngle] 为 0（默认）时这一项**不生效**，圆锥保持对称。
 *
 *   ⚠ 它是**动态过冲的天花板，不是平衡点**：越接近它，[limitStiffness] 给出的额外回正越强，
 *   而驱动力（离心力那一路）随着方向趋于水平而变小，所以稳态永远到不了 90° ——
 *   只有起转那一下的欧拉冲量能把它甩得更近。想让转向时甩得更远就调大它。
 * @param limitStiffness **软限位强度** `k`，默认 3.0；`0` = 关闭（行为回到只有硬夹逼的旧版）。
 *
 *   额外回正 = `k·(偏离角/该侧上限)²` 倍的重力回正，方向是切向、`偏离角→0` 时为零。
 *   于是偏离越远回正越强、越接近上限越难再偏转 —— 表现上是"靠近上限时明显减速"，
 *   而不是撞上一堵硬墙再"啪"地贴住。值越大越硬越早：`1` 只在接近上限时有点感觉，
 *   `3`（默认）中段就开始变紧，`6` 以上会明显缩短余摆并让摆动变"利索"。
 *
 *   ⚠ 别写太大：壁面附近的等效频率 `ω_eff ≈ sqrt(g/L · 2k/上限)`，`k` 越大越接近
 *   30Hz 固定步长的稳定边界（代码里钳到 8）。
 * @param aimResponseScale 开镜时 [response] 与 [turnResponse] 的缩放（默认 0.08）。
 *   瞄准时吊坠还在晃会干扰视线，这里按 `zoomTime` 在 1 与它之间插值。
 * @param idleSway 站立不动时的余摆幅度（度），默认 0.4。
 *   纯程序化的极慢正弦扰动，避免"完全静止"带来的死板感；设 0 关闭。
 * @param lateralAngle **侧向限位**：朝模型局部 **+X**（= 玩家视角的**右侧**，也就是吊坠挂在枪身另一侧时
 *   枪身所在的那一侧）最多偏转多少度；**负值**改为限制 −X 侧；0（默认）= 不额外限制。
 *
 *   吊坠通常挂在枪身一侧，本体比绳子长得多，只要往枪身那边摆一点就会扫进模型里。
 *   这个限位是**按方向而不是按摆角**卡的：把绳方向在局部 X 上的分量钳到 `sin(lateralAngle)`，
 *   Y/Z 按比例缩放保持单位长度，于是"贴住"时是顺着一个斜面滑动，不会抖也不会跳。
 *   它的**符号**还决定了哪一侧算"枪身侧"，也就是 [maxAngle] / [maxAngleFree] 的过渡朝哪边收。
 *   ⚠ 模型烘焙时 **X 轴取负**（Bedrock 与渲染空间的约定），所以这里说的 +X 对应
 *   Blockbench 里的 −X —— 拿不准就先填 15 进游戏看一眼摆哪边。
 * @param smoothing 输入平滑时间常数（秒），默认 0.06；0 = 关闭。
 *
 *   **专治走路/飞行时的高频抖动**：玩家位置是按 tick（20Hz）插值的折线，枪的步行摇晃相位也是
 *   每 tick 一个速度值，于是"悬挂点位移""相机速度""视角角速度"里都混着量化噪声。
 *   逐帧对它做差分就是一根脉冲串，而吊坠的绳长只有一两厘米，一丁点位移就能放大成几十度。
 *   把这个时间常数调大更平滑、更迟钝；调到 0.15 以上连起步甩动都会变糊。
 */
@Serializable
data class CharmInfo(
    @SerialName("Frequency")
    val frequency: Double = 1.5,

    @SerialName("Gravity")
    val gravity: Double? = null,

    @SerialName("Damping")
    val damping: Double = 4.0,

    @SerialName("Response")
    val response: Double = 0.15,

    @SerialName("TurnResponse")
    val turnResponse: Double = 0.02,

    @SerialName("MaxAngle")
    val maxAngle: Double = 35.0,

    @SerialName("MaxAngleFree")
    val maxAngleFree: Double = 90.0,

    @SerialName("LimitStiffness")
    val limitStiffness: Double = 6.0,

    @SerialName("AimResponseScale")
    val aimResponseScale: Double = 0.08,

    @SerialName("IdleSway")
    val idleSway: Double = 0.4,

    @SerialName("LateralAngle")
    val lateralAngle: Double = 0.0,

    @SerialName("Smoothing")
    val smoothing: Double = 0.06,
) {
    companion object {
        /**
         * 缺省参数的一份实例：配件数据里没写 `Charm` 块时用它。
         *
         * 渲染路径每帧都会问一次参数，用它省掉一次分配。
         */
        @JvmField
        val DEFAULT: CharmInfo = CharmInfo()
    }
}
