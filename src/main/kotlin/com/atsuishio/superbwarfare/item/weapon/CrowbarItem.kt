package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.item.IVehicleInteract
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.tags.BlockTags
import net.minecraft.tags.TagKey
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity.RemovalReason
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.*
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.block.Block
import net.neoforged.neoforge.items.ItemHandlerHelper

private val TIER = object : Tier {
    override fun getUses(): Int {
        return 400
    }

    override fun getSpeed(): Float {
        return 4f
    }

    override fun getAttackDamageBonus(): Float {
        return 3.5f
    }

    override fun getIncorrectBlocksForDrops(): TagKey<Block?> {
        return BlockTags.INCORRECT_FOR_IRON_TOOL
    }

    override fun getEnchantmentValue(): Int {
        return 9
    }

    override fun getRepairIngredient(): Ingredient {
        return Ingredient.of(ItemStack(Items.IRON_INGOT))
    }
}

class CrowbarItem : SwordItem(
    TIER, Properties().stacksTo(1)
        .attributes(
            createAttributes(TIER, 2, -2f)
                .withModifierAdded(
                    Attributes.BLOCK_INTERACTION_RANGE,
                    AttributeModifier(Mod.ATTRIBUTE_MODIFIER, 3.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND
                )
        )
), IVehicleInteract {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.crowbar").withStyle(ChatFormatting.GRAY))
        tooltipComponents.add(Component.translatable("des.superbwarfare.crowbar_2").withStyle(ChatFormatting.GRAY))
    }

    override fun onInteractVehicle(
        vehicle: VehicleEntity,
        stack: ItemStack,
        player: Player,
        hand: InteractionHand
    ): InteractionResult? {
        return crowbarInteract(vehicle, stack, player, hand)
    }

    companion object {
        @JvmStatic
        fun crowbarInteract(
            vehicle: VehicleEntity,
            stack: ItemStack,
            player: Player,
            hand: InteractionHand
        ): InteractionResult? {
            if (!player.isShiftKeyDown || vehicle.passengers.isNotEmpty()) return null
            if (vehicle.isWreck) {
                return InteractionResult.PASS
            } else {
                for (item in vehicle.getRetrieveItems()) {
                    ItemHandlerHelper.giveItemToPlayer(player, item)
                }
                vehicle.remove(RemovalReason.DISCARDED)
                vehicle.discard()
                return InteractionResult.SUCCESS
            }
        }
    }
}