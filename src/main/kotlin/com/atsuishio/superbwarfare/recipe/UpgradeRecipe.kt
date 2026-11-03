package com.atsuishio.superbwarfare.recipe

import com.atsuishio.superbwarfare.data.gun.GunState
import com.atsuishio.superbwarfare.init.ModRecipes
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import net.minecraft.core.RegistryAccess
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NumericTag
import net.minecraft.nbt.StringTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.ShapelessRecipe
import net.minecraft.world.level.Level
import net.minecraftforge.common.util.RecipeMatcher
import net.minecraftforge.registries.ForgeRegistries

/** Workbench gun upgrades: fresh target weapon, with only the source's progression and name. */
class UpgradeRecipe(val base: Item, recipe: ShapelessRecipe) : ShapelessRecipe(
    recipe.id, recipe.group, recipe.category(), recipe.getResultItem(RegistryAccess.EMPTY), recipe.ingredients
) {
    init {
        val result = getResultItem(RegistryAccess.EMPTY)
        require(base is GunItem && result.item is GunItem) { "Upgrade base and result must be guns" }
        require(result.count == 1) { "Upgrade result must be one gun" }
        require(ingredients.size >= 2) { "Upgrade requires a gun and at least one material" }
        val baseIngredients = ingredients.filter { it.test(ItemStack(base)) }
        require(baseIngredients.size == 1 && baseIngredients.single().items.let { it.size == 1 && it[0].`is`(base) }) {
            "Upgrade must contain exactly one unambiguous base gun ingredient"
        }
        require(ingredients.filterNot { it === baseIngredients.single() }.none { ingredient ->
            ingredient.items.any { it.item is GunItem }
        }) { "Upgrade materials must not contain other guns" }
    }

    override fun getSerializer(): RecipeSerializer<*> = ModRecipes.UPGRADE_SERIALIZER.get()

    override fun matches(container: CraftingContainer, level: Level): Boolean = input(container) != null

    private fun input(container: CraftingContainer): ItemStack? {
        val stacks = (0 until container.containerSize).map(container::getItem).filterNot(ItemStack::isEmpty)
        if (stacks.size != ingredients.size || RecipeMatcher.findMatches(stacks, ingredients) == null) return null
        return stacks.singleOrNull { it.`is`(base) }?.takeIf(::readyToUpgrade)
    }

    private fun readyToUpgrade(stack: ItemStack): Boolean {
        // Read tags directly: even recipe matching/preview must never initialize or mutate the input.
        // Ammunition is discarded with the old gun; installed perks and attachments must be removed.
        val tag = stack.tag ?: return true
        return listOf("Perks", "Attachments").all { section ->
            val entries = tag.getCompound(section)
            entries.allKeys.none { key ->
                when (val value = entries.get(key)) {
                    is CompoundTag -> !value.isEmpty
                    is ListTag -> !value.isEmpty()
                    is StringTag -> value.asString.isNotBlank()
                    is NumericTag -> value.asDouble != 0.0
                    else -> value != null
                }
            }
        }
    }

    override fun assemble(container: CraftingContainer, access: RegistryAccess): ItemStack {
        val source = input(container) ?: return ItemStack.EMPTY
        val result = ItemStack(getResultItem(access).item)
        val oldState = source.tag?.getCompound(GunState.KEY_GUN_DATA)
        val exp = oldState?.getDouble("Exp") ?: 0.0
        GunState(
            level = (oldState?.getInt("Level") ?: 0).coerceAtLeast(0),
            exp = if (exp.isFinite()) exp.coerceAtLeast(0.0) else 0.0,
        ).writeInto(result.getOrCreateTagElement(GunState.KEY_GUN_DATA))
        if (source.hasCustomHoverName()) result.hoverName = source.hoverName.copy()
        // UUID, manufacture date and other runtime state follow the normal new-gun initialization.
        return result
    }

    class Serializer : RecipeSerializer<UpgradeRecipe> {
        private val shapeless = ShapelessRecipe.Serializer()

        override fun fromJson(id: ResourceLocation, json: JsonObject): UpgradeRecipe {
            if (GsonHelper.getAsJsonObject(json, "result").has("nbt")) {
                throw JsonSyntaxException("Upgrade result must not specify NBT")
            }
            return create(GsonHelper.getAsString(json, "base"), shapeless.fromJson(id, json))
        }

        override fun fromNetwork(id: ResourceLocation, buffer: FriendlyByteBuf): UpgradeRecipe {
            val recipe = requireNotNull(shapeless.fromNetwork(id, buffer)) { "Invalid upgrade recipe packet: $id" }
            return create(buffer.readResourceLocation().toString(), recipe)
        }

        override fun toNetwork(buffer: FriendlyByteBuf, recipe: UpgradeRecipe) {
            shapeless.toNetwork(buffer, recipe)
            buffer.writeResourceLocation(ForgeRegistries.ITEMS.getKey(recipe.base)!!)
        }

        private fun create(base: String, recipe: ShapelessRecipe): UpgradeRecipe {
            val item = ResourceLocation.tryParse(base)?.let(ForgeRegistries.ITEMS::getValue)
                ?: throw JsonSyntaxException("Unknown upgrade base: $base")
            try {
                return UpgradeRecipe(item, recipe)
            } catch (error: IllegalArgumentException) {
                throw JsonSyntaxException(error.message, error)
            }
        }
    }
}
