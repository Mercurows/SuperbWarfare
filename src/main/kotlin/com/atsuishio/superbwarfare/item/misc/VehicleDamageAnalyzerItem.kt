package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.item.IVehicleInteract
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

class VehicleDamageAnalyzerItem : Item(Properties().stacksTo(1).rarity(Rarity.UNCOMMON)), IVehicleInteract {
    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        pTooltipComponents.add(
            Component.translatable("des.superbwarfare.vehicle_damage_analyzer").withStyle(ChatFormatting.GRAY)
        )
    }

    override fun onInteractVehicle(
        vehicle: VehicleEntity,
        stack: ItemStack,
        player: Player,
        hand: InteractionHand
    ): InteractionResult {
        val level = player.level()
        if (!level.isClientSide) {
            if (vehicle.damageDebugResultReceiver != null) {
                vehicle.damageDebugResultReceiver = null
                player.displayClientMessage(
                    Component.translatable(
                        "des.superbwarfare.vehicle_damage_analyzer.unbind",
                        vehicle.displayName
                    ), true
                )
            } else {
                vehicle.damageDebugResultReceiver = player
                player.displayClientMessage(
                    Component.translatable(
                        "des.superbwarfare.vehicle_damage_analyzer.bind",
                        vehicle.displayName
                    ), true
                )
            }
        }
        return InteractionResult.SUCCESS
    }
}