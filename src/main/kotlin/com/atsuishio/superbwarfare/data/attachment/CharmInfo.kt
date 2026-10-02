package com.atsuishio.superbwarfare.data.attachment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 吊坠（`AttachmentDefinition.Charm`）的摆动参数
 *
 * 吊坠模型按 `fixed` / `string` / `charm` 三组制作（见 [AttachmentSlots.Bones]），后两组随玩家运动与视角晃动摆动
 * 摆点与摆长从 `string` 分组的绑定包围盒推导（顶部中心与高度），静止方向取世界重力方向，
 * 所以美术只要把绳子竖直向下、本体吊在绳子末端，这里就不必再写任何几何参数
 *
 * 摆角是加速度的函数（`tan(θ) ≈ a / g`），而吊坠绳长往往只有一两厘米，游戏里的加速度远大于等效重力，
 * 因此平移与转身两路伪力各有一个系数，把它们按比例压回合适的量级
 *
 * @param frequency 自然摆动频率（Hz），默认 1.5，与摆长无关：等效重力由 `g = L·(2πf)²` 反推，
 *   换更大或更小的模型观感不变（直接用真实重力会让小吊坠高频发颤），它同时决定回弹的快慢
 * @param gravity 显式指定等效重力（方块/秒²），默认 null 表示用 [frequency] 反推
 * @param damping 速度阻尼（1/秒），默认 4.0，越大停得越快，20 以上基本黏在枪上，越小余摆拖得越久
 * @param response 平移与枪身动画的惯性响应系数（0~1），默认 0.15，0 = 完全跟枪，1 = 完全甩在身后
 * @param turnResponse 转身那一路（离心力 + 欧拉力 + 科里奥利力）的响应系数（0~1），默认 0.02，
 *   与 [response] 分开是因为两路需要的量级不同
 * @param maxAngle 朝枪身一侧的摆角上限（度），默认 35，防止吊坠甩进枪身或手臂里
 * @param maxAngleFree 其余方位（无遮挡侧、正前、正后）的摆角上限（度），默认 90，
 *   [lateralAngle] 为 0 时它不生效，摆角上限是对称的
 * @param limitStiffness 软限位强度 `k`，默认 6.0，额外回正力随偏离增大，越接近上限越难推，
 *   表现上是靠近上限时明显减速而不是撞墙，0 = 关闭，别写太大，解算器为稳定起见钳到 8
 * @param aimResponseScale 开镜时 [response] 与 [turnResponse] 的缩放，默认 0.08，按开镜进度插值
 * @param idleSway 站立不动时的余摆幅度（度），默认 0.4，避免完全静止显得死板，0 = 关闭
 * @param lateralAngle 侧向限位：限制朝模型局部 +X（玩家视角右侧）的偏转角度，负值改为限制 −X 侧，
 *   0 = 不限制，它的符号同时决定哪一侧算"枪身侧"，也就是 [maxAngle] 与 [maxAngleFree] 的过渡朝哪边收，
 *   注意模型烘焙时 X 轴取负，这里的 +X 对应 Blockbench 里的 −X
 * @param smoothing 输入平滑时间常数（秒），默认 0.06，用来压掉走路/飞行时 20Hz 位置插值带来的抖动，0 = 关闭
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
        @JvmField
        val DEFAULT: CharmInfo = CharmInfo()
    }
}
