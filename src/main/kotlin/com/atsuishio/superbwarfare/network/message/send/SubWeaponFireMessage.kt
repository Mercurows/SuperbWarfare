package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVector3f
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime
import com.atsuishio.superbwarfare.tools.playLocalSound
import com.atsuishio.superbwarfare.tools.toVec3
import kotlinx.serialization.Serializable

/**
 * 副武器触发报文（客户端 → 服务端）。
 *
 * 报文只表达**意图**：`这次 G 请操作这些槽位上的副武器`。
 * **开火还是装填由服务端一个人决定**（[handler] 里那次 `canShoot`）——
 * 客户端虽然也看得到副武器的状态，但那份状态是同步过来的、永远慢一拍；
 * 两边各判一次迟早会因为时序差出现"客户端以为能开火、服务端以为不能"的死角，
 * 而客户端已经把冷却预写下去，于是按 G 变成永久静默。
 *
 * 槽位用 `AttachmentType` 的枚举名（`SUBWEAPON`），与 `SubWeaponRuntime.find` 对称。
 *
 * **不做几何复核**（与近战/开火同一套信任模型）：服务端只确认主手是枪、
 * 这个槽位确实装着副武器，不是反作弊。
 */
@RegisterPacket
@Serializable
data class SubWeaponFireMessage(
    /** 本次要操作的副武器槽位（装了多个副武器时逐个触发） */
    val slots: List<String>,

    val spread: Double = 0.0,
    val zoom: Boolean = false,
    val uuid: SerializedUUID? = null,
    val targetPos: SerializedVector3f? = null,
    val power: Double = 1.0,
) : ServerPacketPayload() {

    override fun PayloadContext.handler() {
        val player = sender()
        if (player.isSpectator) return

        val stack = player.mainHandItem
        if (!GunItem.isHeldWeapon(stack)) return

        // 副武器的触发冷却写在**主武器**的冷却表上（键 `sub:<slot>`），所以这里拿的是主武器的 GunData
        val gun = GunData.from(stack)

        for (slotName in slots) {
            val instance = SubWeaponRuntime.find(gun, slotName, client = false)
            if (instance == null) {
                debug { "subweapon '$slotName' is not installed on ${gun.id}" }
                continue
            }

            val subWeapon = instance.data

            if (gun.cooldown.isCoolingDown(instance.cooldownKey)) {
                debug {
                    "subweapon '$slotName' skipped: cooling down " +
                            "(${gun.cooldown.get(instance.cooldownKey)} ticks left)"
                }
                continue
            }

            // ↓ 开火的**唯一**判定点（装填已改由 `SubWeaponRuntime.tick` 自动完成，这里不再触发）
            if (subWeapon.canShoot(player)) {
                if (targetPos == null) {
                    subWeapon.shoot(player, spread, zoom, uuid, power)
                } else {
                    subWeapon.shoot(player, spread, zoom, uuid, targetPos.toVec3(), power)
                }
                gun.cooldown.set(instance.cooldownKey, instance.cooldownTicks())

                // 第一人称开火音：**服务端拍板、只发给射手一个人**。
                //
                // 只有走到这里（`canShoot` 为真、这一发真的打出去了）才会响，
                // 所以"装填中 / 空仓 / 冷却中按 G"不可能再响开火音 ——
                // 之前让客户端拿自己那份同步状态去预测，结果就是装填期间空响一发。
                // 音效参数由 `GunItem.resolveFire1PSounds` 算（与主武器同一套口径），
                // 播放走 `playLocalSound`（`ClientboundSoundPacket`，只发给这个玩家）。
                for (sound in subWeapon.item.resolveFire1PSounds(subWeapon)) {
                    player.playLocalSound(sound.sound, sound.volume, sound.pitch)
                }

                // 开火屏幕抖动：与载具武器同一个入口，幅度由副武器数据自己的 `ShootShake` 决定
                // （`[半径, 时长, 幅度]`，三项都 > 0 才生效；没写就不抖）。
                // 主武器的 `GunItem` 里那一行是**注释掉**的，所以这里得显式调一次。
                subWeapon.shakePlayers(player)

                debug { "subweapon '$slotName' fired (${subWeapon.id})" }
            } else {
                // 打不出去就什么都不做 —— 装填已经由 `SubWeaponRuntime.tick` 的自动装填接管，
                // 客户端也不会再发"请装填"的请求。这里留一行日志方便对账。
                debug {
                    "subweapon '$slotName' cannot shoot: " +
                            "state=${subWeapon.reload.state()} ammo=${subWeapon.ammo.get()}/" +
                            "${subWeapon.get(GunProp.MAGAZINE)} backupAmmo=${subWeapon.hasBackupAmmo(player)}"
                }
            }
        }
    }

    private inline fun debug(message: () -> String) {
        if (DisplayConfig.MELEE_DEBUG_LOG.get()) {
            Mod.LOGGER.info("[SubWeapon] {}", message())
        }
    }
}
