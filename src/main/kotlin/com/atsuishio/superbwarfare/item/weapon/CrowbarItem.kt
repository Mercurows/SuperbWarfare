package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.Mod
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.tags.BlockTags
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.*
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.block.Block


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

    val level: Int
        get() = 1

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
) {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.crowbar").withStyle(ChatFormatting.GRAY))
        tooltipComponents.add(Component.translatable("des.superbwarfare.crowbar_2").withStyle(ChatFormatting.GRAY))
    }
}