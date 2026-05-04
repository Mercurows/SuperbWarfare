package com.atsuishio.superbwarfare.recipe

import com.atsuishio.superbwarfare.init.ModPotions
import com.atsuishio.superbwarfare.tools.isSameItemStack
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.item.crafting.Ingredient
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.common.brewing.BrewingRecipe
import net.neoforged.neoforge.common.brewing.IBrewingRecipe
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent

@EventBusSubscriber
object ModPotionRecipes {
    @SubscribeEvent
    fun register(event: RegisterBrewingRecipesEvent) {
        val water = potion(Potions.WATER)
        val shock = potion(ModPotions.SHOCK)
        val strongShock = potion(ModPotions.STRONG_SHOCK)
        val longShock = potion(ModPotions.LONG_SHOCK)

        event.builder.addRecipe(createRecipe(Ingredient.of(water), Ingredient.of(Items.LIGHTNING_ROD), shock))
        event.builder.addRecipe(
            createRecipe(Ingredient.of(shock), Ingredient.of(Items.GLOWSTONE_DUST), strongShock)
        )
        event.builder.addRecipe(createRecipe(Ingredient.of(shock), Ingredient.of(Items.REDSTONE), longShock))
    }

    fun potion(potion: Holder<Potion>): ItemStack {
        val stack = Items.POTION.defaultInstance
        val contents = stack.get(DataComponents.POTION_CONTENTS)

        if (contents != null) {
            stack.set(DataComponents.POTION_CONTENTS, contents.withPotion(potion))
        }

        return stack
    }

    private fun createRecipe(input: Ingredient, ingredient: Ingredient, output: ItemStack): IBrewingRecipe {
        return object : BrewingRecipe(input, ingredient, output) {
            override fun isInput(stack: ItemStack): Boolean {
                val matchingStacks = input.getItems()
                return if (matchingStacks.size == 0) stack.isEmpty else matchingStacks
                    .any { s -> isSameItemStack(s, stack) }
            }

            override fun isIngredient(stack: ItemStack): Boolean {
                val matchingStacks = ingredient.getItems()
                return if (matchingStacks.size == 0) stack.isEmpty else matchingStacks
                    .any { s -> isSameItemStack(s, stack) }
            }
        }
    }
}
