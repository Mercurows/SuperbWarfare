package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.item.IVehicleInteract
import com.atsuishio.superbwarfare.network.message.receive.OpenVehicleSkinScreenMessage
import com.atsuishio.superbwarfare.tools.sendPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

class SkinSprayItem : Item(Properties().stacksTo(1)), IVehicleInteract {
    override fun onInteractVehicle(
        vehicle: VehicleEntity,
        stack: ItemStack,
        player: Player,
        hand: InteractionHand
    ): InteractionResult {
        val level = player.level()
        if (!level.isClientSide) {
            player.sendPacket(OpenVehicleSkinScreenMessage(vehicle.id))
        }
        return InteractionResult.CONSUME
    }
}
