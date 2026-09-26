package com.atsuishio.superbwarfare.tools

import net.minecraft.sounds.SoundEvent

/**
 * 一条「本地音效」：音效 + 音量 + 音高。
 *
 * 由 `GunItem.resolveFire1PSounds` 算出来（第一人称开火音的口径），只用来**传参**：
 * - 主武器在客户端直接 `player.playSound(...)`（客户端播放天然只有自己听得到）；
 * - 副武器在服务端用 `player.playLocalSound(...)`（`SoundTool` → `ClientboundSoundPacket`，
 *   只发给射手一个人）。
 *
 * 两条链路共用同一份参数，所以"副武器开火听起来和 m_79 一样"是构造上成立的，
 * 而不是靠两处代码各自对齐。
 */
data class LocalSound(
    val sound: SoundEvent,
    val volume: Float = 1f,
    val pitch: Float = 1f,
)
