package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedSoundEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.sounds.SoundEvents

/**
 * 枪械近战攻击的音效。
 *
 * 刻意与 [SoundInfo] 分开成两个对象：属性覆盖（`Override`）的合并粒度是**顶层属性**，
 * 写 `"SoundInfo": {...}` 会整块替换掉枪械原本的开火/换弹音效（见
 * [com.atsuishio.superbwarfare.data.JsonOverrideApplier]）。近战音效独立成对象后，
 * 配件只写 `"MeleeSound": {"Hit": "..."}` 就不会碰到任何开火音效，
 * 没写的字段落回本类的默认值。
 *
 * 注意 [hit] 的默认值是 `null` 而不是 `ModSounds.MELEE_HIT`：默认值会在构造
 * [DefaultGunData] 时求值，而 `EmptyGunItem.EMPTY_GUN_DATA` 是注册期的静态字段，
 * 那时音效注册未必完成，`RegistryObject.get()` 会失败。回退放在播放处（见
 * `MeleeAttackMessage`）。
 */
@Serializable
data class MeleeSound(
    /** 挥击音效，客户端本地播放；命中与落空都会响 */
    @SerialName("Swing")
    val swing: SerializedSoundEvent = SoundEvents.PLAYER_ATTACK_SWEEP,

    /** 命中实体时在目标位置播放的音效；null 表示回退到 `superbwarfare:melee_hit` */
    @SerialName("Hit")
    val hit: SerializedSoundEvent? = null,
)
