package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.GunProp
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
data class EditMessage(val type: Int, val add: Boolean, val isVehicle: Boolean) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val vehicle = player.vehicle

        if (isVehicle && vehicle is VehicleEntity) {
            if (type != 5) return

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
            when (type) {
                0 -> data.attachment.cycle(AttachmentType.BARREL, add)
                1 -> data.attachment.cycle(AttachmentType.SCOPE, add)
                2 -> data.attachment.cycle(AttachmentType.GRIP, add)
                3 -> data.attachment.cycle(AttachmentType.STOCK, add)
                4 -> {
                    data.withdrawAmmo(player)
                    data.attachment.cycle(AttachmentType.MAGAZINE, add)
                }

                5 -> {
                    val size = data.get(GunProp.AMMO_CONSUMER).size
                    data.changeAmmoConsumer(
                        (data.selectedAmmoType.get() + (if (add) 1 else -1) + size) % size,
                        player
                    )
                }
            }
            data.save()
            player.playLocalSound(ModSounds.EDIT.get(), 1f, 1f)
        }
    }
}


