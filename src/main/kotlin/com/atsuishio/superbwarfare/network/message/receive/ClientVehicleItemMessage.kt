package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedTag
import com.atsuishio.superbwarfare.tools.clientLevel
import kotlinx.serialization.Serializable
import net.minecraft.nbt.CompoundTag

@Serializable
data class ClientVehicleItemMessage(
    val id: Int,
    val tag: SerializedTag
) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        val entity = clientLevel?.getEntity(id) as? VehicleEntity ?: return
        val tag = tag as? CompoundTag ?: return
        entity.inventory.deserializeNBT(tag)
    }
}