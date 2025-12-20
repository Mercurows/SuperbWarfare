package com.atsuishio.superbwarfare.tiers

import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.world.item.Tier
import net.minecraft.world.item.crafting.Ingredient
import java.util.function.Supplier

enum class ModItemTier(
    private val level: Int,
    private val uses: Int,
    private val speed: Float,
    private val damage: Float,
    private val enchantmentValue: Int,
    private val repairIngredient: Supplier<Ingredient>
) : Tier {
    STEEL(2, 400, 6.0f, 5.0f, 15, Supplier { Ingredient.of(ModItems.STEEL_INGOT.get()) }),
    CEMENTED_CARBIDE(4, 2000, 8.0f, 8.0f, 18, Supplier { Ingredient.of(ModItems.CEMENTED_CARBIDE_INGOT.get()) });

    val ingredient by lazy { repairIngredient.get() }

    @Deprecated("Deprecated in Java")
    override fun getLevel(): Int {
        return level
    }

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

    override fun getRepairIngredient(): Ingredient {
        return ingredient
    }
}
