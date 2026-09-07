package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.RegistryName
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

@RegistryName("creative_vehicle_key")
class CreativeVehicleKeyItem : VehicleKeyItem(Properties().stacksTo(1).rarity(Rarity.EPIC)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        tooltip.add(Component.translatable("des.superbwarfare.creative_vehicle_key").withStyle(ChatFormatting.GRAY))
    }

    override fun use(
        level: Level,
        player: Player,
        hand: InteractionHand
    ): InteractionResultHolder<ItemStack> {
        return InteractionResultHolder.fail(player.getItemInHand(hand))
    }

    override fun onInteractVehicle(
        vehicle: VehicleEntity,
        stack: ItemStack,
        player: Player,
        hand: InteractionHand
    ): InteractionResult {
        if (!vehicle.passengers.isEmpty()) {
            player.displayClientMessage(
                Component.translatable("tips.superbwarfare.vehicle.lock_not_empty")
                    .withStyle(ChatFormatting.RED), true
            )
            return InteractionResult.FAIL
        }

        if (!vehicle.locked) {
            vehicle.lastDriverUUID = player.stringUUID
            vehicle.locked = true

            player.displayClientMessage(
                Component.translatable(
                    "tips.superbwarfare.vehicle.lock_vehicle",
                    vehicle.displayName
                ).withStyle(ChatFormatting.GREEN), true
            )
            return InteractionResult.SUCCESS
        } else {
            vehicle.lastDriverUUID = player.stringUUID
            vehicle.locked = false

            player.displayClientMessage(
                Component.translatable(
                    "tips.superbwarfare.vehicle.unlock_vehicle",
                    vehicle.displayName
                ).withStyle(ChatFormatting.GREEN), true
            )
            return InteractionResult.SUCCESS
        }
    }
}
