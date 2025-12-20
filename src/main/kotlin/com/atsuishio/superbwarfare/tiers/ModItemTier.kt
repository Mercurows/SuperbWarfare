package com.atsuishio.superbwarfare.tiers

import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.tags.BlockTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Tier
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.block.Block
import java.util.function.Supplier

enum class ModItemTier(
    private val uses: Int,
    private val speed: Float,
    private val damage: Float,
    private val enchantmentValue: Int,
    private val repairIngredient: Supplier<Ingredient>
) : Tier {
    STEEL(400, 6F, 5F, 15, { Ingredient.of(ModItems.STEEL_INGOT.get()) }),
    CEMENTED_CARBIDE(2000, 8F, 8F, 18, { Ingredient.of(ModItems.CEMENTED_CARBIDE_INGOT.get()) });

    val ingredient by lazy { repairIngredient.get() }

    override fun getUses(): Int {
        return uses
    }

    override fun getSpeed(): Float {
        return speed
    }

    override fun getAttackDamageBonus(): Float {
        return damage
    }

    override fun getEnchantmentValue(): Int {
        return enchantmentValue
    }

    override fun getIncorrectBlocksForDrops(): TagKey<Block> {
        return BlockTags.INCORRECT_FOR_IRON_TOOL
    }

    override fun getRepairIngredient(): Ingredient {
        return ingredient
    }
}
