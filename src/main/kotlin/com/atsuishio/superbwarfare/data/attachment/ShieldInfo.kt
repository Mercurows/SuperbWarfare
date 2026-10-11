package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedSoundEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.sounds.SoundEvents
import kotlin.math.cos

/**
 * 枪盾配件数据
 */
@Serializable
data class ShieldInfo(
    /** 耐久上限 */
    @SerialName("Durability")
    val durability: Double = 100.0,

    /** 减伤倍率（0~100），100 = 完全吸收，读侧 clamp */
    @SerialName("Resist")
    val resist: Double = 100.0,

    /** 防护锥半角（度），相对视线，读侧 clamp 到 0~180 */
    @SerialName("CoverAngle")
    val coverAngle: Double = 70.0,

    /** 每秒恢复的耐久 */
    @SerialName("RechargeRate")
    val rechargeRate: Double = 1.0,

    /** 受击后多少 tick 开始回充 */
    @SerialName("RechargeDelay")
    val rechargeDelay: Int = 60,

    /** 破碎后的锁定时长（tick） */
    @SerialName("BrokenLockout")
    val brokenLockout: Int = 40,

    /** 恢复 1 点耐久消耗的 FE，0 = 免费回充 */
    @SerialName("EnergyPerCharge")
    val energyPerCharge: Int = 0,

    /** 破盾那一发是否穿透：0 = 全额穿透，100 = 仍然完全吸收 */
    @SerialName("BreakOverflow")
    val breakOverflow: Double = 0.0,

    /** 吸收伤害时的音效 id */
    @SerialName("BlockSound")
    val blockSound: SerializedSoundEvent? = SoundEvents.SHIELD_BLOCK,

    /** 破碎时的音效 id */
    @SerialName("BreakSound")
    val breakSound: SerializedSoundEvent? = SoundEvents.SHIELD_BREAK,

    /** 开始恢复时的音效 id，一次恢复只播一次，且只播给自己听 */
    @SerialName("RechargeSound")
    val rechargeSound: SerializedSoundEvent? = SoundEvents.BEACON_ACTIVATE,
) {
    /** 实际生效的减伤比例（0~1） */
    fun resistRate(): Double = (resist / 100.0).coerceIn(0.0, 1.0)

    /** 实际生效的防护锥半角余弦阈值 */
    fun coverCos(): Double = cos(Math.toRadians(coverAngle.coerceIn(0.0, 180.0)))

    companion object {
        @JvmField
        val DEFAULT: ShieldInfo = ShieldInfo()
    }
}
