package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.attachment.AttachmentEditTarget
import com.atsuishio.superbwarfare.data.attachment.AttachmentSlots
import com.atsuishio.superbwarfare.data.gun.ActiveGun
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.subweapon.SubWeaponRuntime
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.LivingEventHandler
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.playLocalSound
import kotlinx.serialization.Serializable

@RegisterPacket
@Serializable
data class EditMessage(
    val type: Int,
    val add: Boolean,
    val isVehicle: Boolean,
    /**
     * 弹种切换是否跟随「当前操控的枪」。
     *
     * - `true`：键位切换（⬅️/➡️）用。副武器（下挂榴弹等）部署时切它自己的弹种；
     * - `false`（默认）：改装界面的弹种面板用 —— 那里编辑的始终是主手那把枪。
     *
     * 只影响 [AttachmentSlots.EDIT_ORDER] 里 `AmmoType` 那一项，其余槽位永远只改主手。
     */
    val operated: Boolean = false,
    /**
     * 直接选中某个弹种下标（改装界面的弹种面板点哪格就换哪个）。
     * `< 0` 表示按 [add] 前后循环一格（键位切换）。
     */
    val index: Int = -1,
    /**
     * 弹种切换的目标是不是**主手枪上装着的副武器**（下挂榴弹等）。
     * 改装界面里副武器另有一个弹种面板，它不需要副武器处于部署状态。
     */
    val subWeapon: Boolean = false,
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val vehicle = player.vehicle

        if (isVehicle && vehicle is VehicleEntity) {
            // 载具改装只支持弹种切换
            if (type != AttachmentSlots.AMMO_TYPE_EDIT_INDEX) return

            vehicle.modifyGunData(vehicle.getSeatIndex(player)) { data ->
                val size = data.get(GunProp.AMMO_CONSUMER).size
                LivingEventHandler.stopGunReloadSound(player, data)
                data.changeAmmoConsumer(
                    (data.selectedAmmoType.get() + (if (add) 1 else -1) + size) % size,
                    vehicle.ammoSupplier
                )

                val sound = data.get(GunProp.SOUND_INFO).change ?: return@modifyGunData
                player.playLocalSound(sound, 4f, 1f)
            }
        } else {
            val mainStack = player.mainHandItem
            if (mainStack.item !is GunItem) return

            // 下标 → 目标 的映射只有 AttachmentSlots.EDIT_ORDER 一份（与改装界面的按钮下标对齐），
            // 服务端不再自己维护一张 when 表
            val target = AttachmentSlots.EDIT_ORDER.getOrNull(type) ?: return
            // 弹种切换的三类目标：
            // - `subWeapon`：改装界面里主手枪上装着的副武器（下挂榴弹等），不要求它已部署；
            // - `operated`：键位切换，跟随「当前操控的枪」；
            // - 其余：主手那把枪（改装界面编辑的对象）。槽位循环永远只改主手。
            val data = if (target is AttachmentEditTarget.AmmoType) {
                when {
                    subWeapon -> SubWeaponRuntime.find(from(mainStack), AttachmentType.SUBWEAPON, client = false)?.data
                    operated -> ActiveGun.dataOf(player)
                    else -> null
                } ?: from(mainStack)
            } else {
                from(mainStack)
            }

            when (target) {
                is AttachmentEditTarget.Slot -> {
                    if (target.slot.withdrawAmmoOnChange) {
                        data.withdrawAmmo(player)
                    }
                    data.attachment.cycle(target.slot.type, add)
                }

                AttachmentEditTarget.AmmoType -> {
                    val size = data.get(GunProp.AMMO_CONSUMER).size
                    // 面板点选给具体下标；键位切换按 add 前后循环一格
                    val picked = when {
                        index in 0 until size -> index
                        size > 1 -> (data.selectedAmmoType.get() + (if (add) 1 else -1) + size) % size
                        else -> -1
                    }
                    if (picked >= 0 && picked != data.selectedAmmoType.get()) {
                        data.changeAmmoConsumer(picked, player)
                        if (!player.isCreative) {
                            data.closeStrike.set(true)
                        }
                    }
                }
            }
            data.save()
            player.playLocalSound(ModSounds.EDIT.get(), 1f, 1f)
        }
    }
}


