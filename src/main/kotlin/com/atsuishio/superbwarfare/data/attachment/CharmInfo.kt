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
 * [maxAngle] 只是兜底的上限，[frequency]/[gravity] 管的是"摆得快不快、回弹利不利索"。
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
 * @param maxAngle 最大摆角（度），默认 35。以**世界下方**为 0° 的锥形夹逼：
 *   既防止吊坠甩到枪身/手臂里，也让它永远远离"方向与静止方向相反"的旋转奇异点
 *   （最短弧旋转在那一处会翻面）。
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
    val turnResponse: Double = 0.30,

    @SerialName("MaxAngle")
    val maxAngle: Double = 35.0,

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
