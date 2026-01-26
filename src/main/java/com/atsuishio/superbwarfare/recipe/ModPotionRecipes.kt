package com.atsuishio.superbwarfare.recipe

import com.atsuishio.superbwarfare.init.ModPotions
import com.atsuishio.superbwarfare.tools.isSameItemStack
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.item.alchemy.PotionUtils
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.item.crafting.Ingredient
import net.minecraftforge.common.brewing.BrewingRecipe
import net.minecraftforge.common.brewing.BrewingRecipeRegistry
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
object ModPotionRecipes {
    @SubscribeEvent
    fun register(event: FMLCommonSetupEvent) {
        val water = potion(Potions.WATER)
        val shock = potion(ModPotions.SHOCK.get())
        val strongShock = potion(ModPotions.STRONG_SHOCK.get())
        val longShock = potion(ModPotions.LONG_SHOCK.get())
        event.enqueueWork {
            BrewingRecipeRegistry.addRecipe(
                PotionRecipe(
                    Ingredient.of(water),
                    Ingredient.of(Items.LIGHTNING_ROD),
                    shock
                )
            )
            BrewingRecipeRegistry.addRecipe(
                PotionRecipe(
                    Ingredient.of(shock),
                    Ingredient.of(Items.GLOWSTONE_DUST),
                    strongShock
                )
            )
            BrewingRecipeRegistry.addRecipe(
                PotionRecipe(
                    Ingredient.of(shock),
                    Ingredient.of(Items.REDSTONE),
                    longShock
                )
            )
        }
    }

    private fun potion(potion: Potion): ItemStack {
        return PotionUtils.setPotion(Items.POTION.defaultInstance, potion)
    }

    class PotionRecipe(private val input: Ingredient, private val ingredient: Ingredient, output: ItemStack) :
        BrewingRecipe(
            input,
            ingredient, output
        ) {
        override fun isInput(stack: ItemStack): Boolean {
            val matchingStacks = this.input.getItems()
            return if (matchingStacks.size == 0) stack.isEmpty
            else matchingStacks.any { isSameItemStack(it, stack) }
        }

        override fun isIngredient(ingredient: ItemStack): Boolean {
            val matchingStacks = this.ingredient.getItems()
            return if (matchingStacks.size == 0) ingredient.isEmpty
            else matchingStacks.any { isSameItemStack(it, ingredient) }
        }
    }
}
