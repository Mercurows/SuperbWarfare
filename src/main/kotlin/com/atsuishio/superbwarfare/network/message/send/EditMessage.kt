package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.attachment.AttachmentEditTarget
import com.atsuishio.superbwarfare.data.attachment.AttachmentSlots
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.LivingEventHandler
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.playLocalSound
import kotlinx.serialization.Serializable

@Serializable
@RegisterPacket
data class EditMessage(val type: Int, val add: Boolean, val isVehicle: Boolean) : ServerPacketPayload() {
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
            val stack = player.mainHandItem
            val item = stack.item
            if (item !is GunItem) return

            val data = from(stack)
            // 下标 → 目标 的映射只有 AttachmentSlots.EDIT_ORDER 一份（与改装界面的按钮下标对齐），
            // 服务端不再自己维护一张 when 表
            when (val target = AttachmentSlots.EDIT_ORDER.getOrNull(type) ?: return) {
                is AttachmentEditTarget.Slot -> {
                    if (target.slot.withdrawAmmoOnChange) {
                        data.withdrawAmmo(player)
                    }
                    data.attachment.cycle(target.slot.type, add)
                }

                AttachmentEditTarget.AmmoType -> {
                    val size = data.get(GunProp.AMMO_CONSUMER).size
                    data.changeAmmoConsumer(
                        (data.selectedAmmoType.get() + (if (add) 1 else -1) + size) % size,
                        player
                    )
                    if (!player.isCreative) {
                        data.closeStrike.set(true)
                    }
                }
            }
            data.save()
            player.playLocalSound(ModSounds.EDIT.get(), 1f, 1f)
        }
    }
}


