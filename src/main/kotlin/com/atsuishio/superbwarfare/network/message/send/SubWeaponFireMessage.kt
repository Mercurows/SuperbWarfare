package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.event.GunEventHandler
import com.atsuishio.superbwarfare.event.GunEventHandler.tryStartReload
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVector3f
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime
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
            val instance = SubWeaponRuntime.find(gun, slotName)
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

            // ↓ 开火 / 装填的**唯一**判定点
            if (subWeapon.canShoot(player)) {
                if (targetPos == null) {
                    subWeapon.shoot(player, spread, zoom, uuid, power)
                } else {
                    subWeapon.shoot(player, spread, zoom, uuid, targetPos.toVec3(), power)
                }
                gun.cooldown.set(instance.cooldownKey, instance.cooldownTicks())
                debug { "subweapon '$slotName' fired (${subWeapon.id})" }
            } else {
                // 空仓 / 状态不允许开火 → 走它自己的换弹流程
                // （`ReloadTypes` / 换弹时间 / 备弹扣除 / 能量换算全都是现成的）
                val timeBefore = subWeapon.reload.time()
                tryStartReload(player, subWeapon)
                val timeAfterTry = subWeapon.reload.time()

                // 换弹是**两段式**的：`startReload()` 只 markStart，真正的状态切换发生在
                // 下一 tick 该枪自己的 `gunTick` 里。这里立刻推进一 tick，
                // 好处是**本 tick 内**状态就变成 RELOADING ——
                // 客户端下一次读状态时不会再把这次触发判成"什么都没发生"。
                GunEventHandler.gunTick(player, subWeapon, inMainHand = true)
                val timeAfterTick = subWeapon.reload.time()

                debug {
                    // `inst=` 是 GunData 实例身份：**每次按 G 都在变**就说明
                    // `SubWeaponRuntime` 的缓存没命中，同一个合成 tag 上会同时存在多个
                    // GunData（各自的 state 是"解码一次就缓存"的镜像），它们互相覆盖 →
                    // 换弹计时器会被打回原值，永远走不到 0。
                    "subweapon '$slotName' reload probe: " +
                            "t=$timeBefore/$timeAfterTry/$timeAfterTick " +
                            "inst=${System.identityHashCode(subWeapon)} " +
                            "state=${subWeapon.reload.state()} " +
                            "starter=${subWeapon.reload.reloadStarter.shouldStart()} " +
                            "ammo=${subWeapon.ammo.get()}/${subWeapon.get(GunProp.MAGAZINE)} " +
                            "backpackAmmo=${subWeapon.useBackpackAmmo()} meleeOnly=${subWeapon.meleeOnly()} " +
                            "reloadTypes=${subWeapon.get(GunProp.RELOAD_TYPES)} " +
                            "backupAmmo=${subWeapon.hasBackupAmmo(player)} " +
                            "emptyBaseline=${subWeapon.getDefault().isDefaultData}"
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
