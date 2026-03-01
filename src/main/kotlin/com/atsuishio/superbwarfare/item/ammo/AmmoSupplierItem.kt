package com.atsuishio.superbwarfare.item.ammo

import com.atsuishio.superbwarfare.capability.player.PlayerVariable
import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.InventoryTool
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

open class AmmoSupplierItem(val type: Ammo, val ammoToAdd: Int, properties: Properties) : Item(properties) {
    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        pTooltipComponents.add(Component.translatable("des.superbwarfare.ammo_supplier").withStyle(ChatFormatting.AQUA))
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack?> {
        val stack = player.getItemInHand(hand)
        var count = stack.count

        if (player.isShiftKeyDown) {
            count = InventoryTool.countItem(player, stack.item)
        }

        val offhandItem = player.offhandItem

        val addedCount = if (offhandItem.`is`(ModItems.AMMO_BOX.get())) {
            val canAddAmount = type.ammoBoxLimit - type.get(offhandItem)
            val toAddCount = (canAddAmount / ammoToAdd).coerceAtMost(count)
            if (toAddCount <= 0) {
                player.displayClientMessage(
                    Component.translatable("item.superbwarfare.ammo_supplier.fail").withStyle(ChatFormatting.RED), true
                )
                return InteractionResultHolder.fail(stack)
            }

            this.type.add(offhandItem, ammoToAdd * toAddCount)

            toAddCount
        } else {
            val capability = PlayerVariable.getOrDefault(player)

            val canAddAmount = type.limit - type.get(capability)
            val toAddCount = (canAddAmount / ammoToAdd).coerceAtMost(count)
            if (toAddCount <= 0) {
                player.displayClientMessage(
                    Component.translatable("item.superbwarfare.ammo_supplier.fail").withStyle(ChatFormatting.RED), true
                )
                return InteractionResultHolder.fail(stack)
            }

            PlayerVariable.modify(player) { this.type.add(it, ammoToAdd * toAddCount) }

            toAddCount
        }

        player.cooldowns.addCooldown(this, 10)

        if (!player.isCreative) {
            InventoryTool.consumeItem(player, stack.item, addedCount)
        }

        if (!level.isClientSide()) {
            player.displayClientMessage(
                Component.translatable(
                    "item.superbwarfare.ammo_supplier.supply",
                    Component.translatable(this.type.translationKey),
                    ammoToAdd * count
                ), true
            )
            level.playSound(null, player.blockPosition(), ModSounds.BULLET_SUPPLY.get(), SoundSource.PLAYERS, 1f, 1f)
        }

        return InteractionResultHolder.success<ItemStack?>(stack)
    }
}
