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
 * 摆角**不取决于重力**，而取决于「这一帧悬挂点相对相机挪了多少」与摆长之比：
 * 玩家转头时悬挂点在参考系里绕相机画弧，一帧就能挪好几厘米，而吊坠的绳子往往只有一两厘米长
 * （当前这条鱼是 0.019 方块），于是稍微快一点的转视角就能把它甩到最大摆角。
 * 所以调摆幅**先调 [response]**（它直接按比例缩小这个"甩"的量），
 * [maxAngle] 只是兜底的上限，[frequency]/[gravity] 管的是"摆得快不快、回弹利不利索"。
 *
 * @param frequency 自然摆动频率（Hz），默认 1.5（周期约 0.67 秒）。
 *   **与摆长无关**：等效重力由 `g = L·(2πf)²` 反推，所以换一个更大/更小的吊坠模型观感不变。
 *   这是刻意的 —— 摆长来自模型，用它直接套真实重力时小吊坠会以三四赫兹高频抖，像在发颤而不是在晃。
 * @param gravity 显式指定等效重力（方块/秒²）；`null`（默认）= 用 [frequency] 反推。
 *   想要"真实重力"就写 `9.8`（代价见 [frequency] 的说明），想更飘就写小一点。
 * @param damping 速度阻尼（1/秒），默认 6.0。速度按 `exp(-damping · dt)` 衰减。
 *   默认值配上 1.5Hz 大约是**临界阻尼的三分之一**：甩出去一两下就停住，不会一直荡。
 *   调到 3 以下会明显"荡秋千"，调到 15 以上会变成"黏在枪上"。
 * @param response 惯性响应系数（0..1），默认 **0.15**。
 *   **1 = 完全物理**（吊坠把枪的运动完全甩在身后），**0 = 完全跟枪**（挂在枪上不动，
 *   只在重力下垂直下垂）。这是**摆幅的主旋钮**：初版用 0.6，玩家稍快转视角就会被甩满 60°，
 *   现在按比例压到四分之一左右。
 * @param maxAngle 最大摆角（度），默认 35。以**世界下方**为 0° 的锥形夹逼：
 *   既防止吊坠甩到枪身/手臂里，也让它永远远离"方向与静止方向相反"的旋转奇异点
 *   （最短弧旋转在那一处会翻面）。
 * @param aimResponseScale 开镜时 [response] 的缩放（默认 0.08）。
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
 *   每 tick 一个速度值，于是"悬挂点位移"和"相机位移"里都混着 20Hz 的量化噪声。
 *   逐帧对它做二阶差分就是一根脉冲串，而吊坠的绳长只有一两厘米，一丁点位移就能放大成几十度。
 *   把这个时间常数调大更平滑、更迟钝；调到 0.15 以上连起步甩动都会变糊。
 */
@Serializable
data class CharmInfo(
    @SerialName("Frequency")
    val frequency: Double = 1.5,

    @SerialName("Gravity")
    val gravity: Double? = null,

    @SerialName("Damping")
    val damping: Double = 6.0,

    @SerialName("Response")
    val response: Double = 0.15,

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
