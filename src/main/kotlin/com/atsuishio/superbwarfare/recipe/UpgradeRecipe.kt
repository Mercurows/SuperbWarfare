package com.atsuishio.superbwarfare.recipe

import com.atsuishio.superbwarfare.data.gun.GunState
import com.atsuishio.superbwarfare.init.ModRecipes
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.tools.tag
import com.mojang.serialization.DataResult
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.HolderLookup
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NumericTag
import net.minecraft.nbt.StringTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.ShapelessRecipe
import net.minecraft.world.level.Level
import net.neoforged.neoforge.common.util.RecipeMatcher

/** Workbench gun upgrades: fresh target weapon, with only the source's progression and name. */
class UpgradeRecipe(val base: Item, recipe: ShapelessRecipe) : ShapelessRecipe(
    recipe.group, recipe.category(), recipe.getResultItem(RegistryAccess.EMPTY), recipe.ingredients
) {
    init {
        val result = getResultItem(RegistryAccess.EMPTY)
        require(base is GunItem && result.item is GunItem) { "Upgrade base and result must be guns" }
        require(result.count == 1) { "Upgrade result must be one gun" }
        require(result.componentsPatch.isEmpty) { "Upgrade result must not specify components" }
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

    override fun matches(container: CraftingInput, level: Level): Boolean = input(container) != null

    private fun input(container: CraftingInput): ItemStack? {
        val stacks = container.items().filterNot(ItemStack::isEmpty)
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

    override fun assemble(container: CraftingInput, access: HolderLookup.Provider): ItemStack {
        val source = input(container) ?: return ItemStack.EMPTY
        val result = ItemStack(getResultItem(access).item)
        val oldState = source.tag?.getCompound(GunState.KEY_GUN_DATA)
        val exp = oldState?.getDouble("Exp") ?: 0.0
        val state = CompoundTag()
        GunState(
            level = (oldState?.getInt("Level") ?: 0).coerceAtLeast(0),
            exp = if (exp.isFinite()) exp.coerceAtLeast(0.0) else 0.0,
        ).writeInto(state)
        result.tag = CompoundTag().apply { put(GunState.KEY_GUN_DATA, state) }
        source.get(DataComponents.CUSTOM_NAME)?.let { result.set(DataComponents.CUSTOM_NAME, it.copy()) }
        // UUID, manufacture date and other runtime state follow the normal new-gun initialization.
        return result
    }

    class Serializer : RecipeSerializer<UpgradeRecipe> {
        private val shapeless = ShapelessRecipe.Serializer()

        private val codec: MapCodec<UpgradeRecipe> = RecordCodecBuilder.mapCodec { builder ->
            builder.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("base").forGetter<Pair<Item, ShapelessRecipe>> { it.first },
                shapeless.codec().forGetter { it.second }
            ).apply(builder) { base, recipe -> base to recipe }
        }.flatXmap(
            { (base, recipe) ->
                try {
                    DataResult.success(UpgradeRecipe(base, recipe))
                } catch (error: IllegalArgumentException) {
                    DataResult.error { error.message ?: "Invalid upgrade recipe" }
                }
            },
            { DataResult.success(it.base to it) }
        )

        private val streamCodec: StreamCodec<RegistryFriendlyByteBuf, UpgradeRecipe> = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM), { it.base },
            shapeless.streamCodec(), { it },
            ::UpgradeRecipe
        )

        override fun codec(): MapCodec<UpgradeRecipe> = codec

        override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, UpgradeRecipe> = streamCodec
    }
}
