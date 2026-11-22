package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedSoundEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.sounds.SoundEvents

/**
 * 枪械近战攻击的音效
 */
@Serializable
data class MeleeSound(
    /** 挥击音效，客户端本地播放；命中与落空都会响 */
    @SerialName("Swing")
    val swing: SerializedSoundEvent = SoundEvents.PLAYER_ATTACK_SWEEP,

    /** 命中实体时在目标位置播放的音效；null 表示回退到 `superbwarfare:melee_hit` */
    @SerialName("Hit")
    val hit: SerializedSoundEvent? = null,

    /** 近战挖掘命中方块时播放的音效 */
    @SerialName("Mine")
    val mine: SerializedSoundEvent? = null,
)
