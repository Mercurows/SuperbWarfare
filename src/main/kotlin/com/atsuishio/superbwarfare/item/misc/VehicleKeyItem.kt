package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.item.IVehicleInteract
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class VehicleKeyItem : Item(Properties().stacksTo(1)), IVehicleInteract {
    override fun use(
        level: Level,
        player: Player,
        hand: InteractionHand
    ): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (stack.tag == null || !stack.tag!!.contains(TAG_UUID)) {
            stack.orCreateTag.putString(TAG_UUID, player.stringUUID)
            return InteractionResultHolder.success(stack)
        }
        return InteractionResultHolder.fail(stack)
    }

    override fun onInteractVehicle(
        vehicle: VehicleEntity,
        stack: ItemStack,
        player: Player,
        hand: InteractionHand
    ): InteractionResult {
        val uuid = stack.tag?.getString(TAG_UUID) ?: return InteractionResult.FAIL
        if (!vehicle.locked) {
            if (vehicle.lastDriverUUID == uuid || vehicle.lastDriverUUID == "undefined") {
                vehicle.lastDriverUUID = uuid
                vehicle.locked = true

                player.displayClientMessage(
                    Component.translatable(
                        "tips.superbwarfare.vehicle.lock_vehicle",
                        vehicle.displayName
                    ).withStyle(ChatFormatting.GREEN), true
                )
                return InteractionResult.SUCCESS
            } else {
                player.displayClientMessage(
                    Component.translatable("tips.superbwarfare.vehicle.lock_fail")
                        .withStyle(ChatFormatting.RED), true
                )
                return InteractionResult.FAIL
            }
        } else {
            if (vehicle.lastDriverUUID == uuid) {
                vehicle.lastDriverUUID = uuid
                vehicle.locked = false

                player.displayClientMessage(
                    Component.translatable(
                        "tips.superbwarfare.vehicle.unlock_vehicle",
                        vehicle.displayName
                    ).withStyle(ChatFormatting.GREEN), true
                )
                return InteractionResult.SUCCESS
            } else {
                player.displayClientMessage(
                    Component.translatable("tips.superbwarfare.vehicle.lock_fail")
                        .withStyle(ChatFormatting.RED), true
                )
                return InteractionResult.FAIL
            }
        }
    }

    companion object {
        const val TAG_UUID = "DriverUUID"
    }
}