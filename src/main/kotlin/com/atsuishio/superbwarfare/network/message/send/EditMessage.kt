package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.LivingEventHandler
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.playLocalSound
import kotlinx.serialization.Serializable

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
                0 -> {
                    var att = data.attachment.get(AttachmentType.BARREL)
                    att = setAttachment(item.validBarrels, att, add)
                    data.attachment.set(AttachmentType.BARREL, att)
                }

                1 -> {
                    var att = data.attachment.get(AttachmentType.SCOPE)
                    att = setAttachment(item.validScopes, att, add)
                    data.attachment.set(AttachmentType.SCOPE, att)
                }

                2 -> {
                    var att = data.attachment.get(AttachmentType.GRIP)
                    att = setAttachment(item.validGrips, att, add)
                    data.attachment.set(AttachmentType.GRIP, att)
                }

                3 -> {
                    var att = data.attachment.get(AttachmentType.STOCK)
                    att = setAttachment(item.validStocks, att, add)
                    data.attachment.set(AttachmentType.STOCK, att)
                }

                4 -> {
                    var att = data.attachment.get(AttachmentType.MAGAZINE)
                    att = setAttachment(item.validMagazines, att, add)
                    data.withdrawAmmo(player)
                    data.attachment.set(AttachmentType.MAGAZINE, att)
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

    private fun setAttachment(arr: IntArray, value: Int, add: Boolean): Int {
        if (arr.isEmpty()) return 0

        val sorted = arr.copyOf(arr.size).sorted()
        var index = sorted.binarySearch(value)
        if (index < 0) index = -index - 1

        index = (if (add) (index + 1) else (index + arr.size - 1)) % arr.size
        return sorted[index]
    }
}


