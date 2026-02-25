package com.atsuishio.superbwarfare.recipe

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.DataLoader
import com.atsuishio.superbwarfare.init.ModRecipes
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import net.minecraft.core.RegistryAccess
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.ItemTags
import net.minecraft.util.GsonHelper
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.minecraftforge.registries.ForgeRegistries

class ResearchingRecipe(
    val recipeId: ResourceLocation,
    val input: Ingredient,
    val base: Ingredient,
    val addition: Ingredient,
    val special: Ingredient,
    val selectable: Boolean,
    val time: Int,
    val result: Result
) : Recipe<SimpleContainer> {
    override fun matches(
        container: SimpleContainer,
        level: Level
    ): Boolean {
        if (container.containerSize < 4) {
            return false
        }
        return input.test(container.getItem(0))
                && base.test(container.getItem(1))
                && addition.test(container.getItem(2))
                && special.test(container.getItem(3))
    }

    override fun assemble(
        pContainer: SimpleContainer?,
        pRegistryAccess: RegistryAccess?
    ): ItemStack? = this.result.getResult().copy()

    override fun isSpecial() = true

    override fun canCraftInDimensions(pWidth: Int, pHeight: Int) = true

    override fun getResultItem(pRegistryAccess: RegistryAccess?): ItemStack = this.result.getResult().copy()

    override fun getId() = this.recipeId

    override fun getSerializer(): RecipeSerializer<*> = ModRecipes.RESEARCHING_SERIALIZER.get()

    override fun getType(): RecipeType<*> = ModRecipes.RESEARCHING_TYPE.get()

    class Result(
        @SerializedName("item") var item: String = "",
        @SerializedName("tag") var tag: String = "",
        @SerializedName("count") var count: Int = 1,
        @SerializedName("nbt") var nbt: JsonObject? = null,
    ) {
        @Transient
        var resultStack: ItemStack? = null

        @Transient
        var list: MutableList<Item>? = null

        fun getResult(): ItemStack {
            if (this.resultStack != null) return this.resultStack!!
            if (!item.isEmpty()) {
                val item = ForgeRegistries.ITEMS.getValue(ResourceLocation(item))
                if (item == null) {
                    Mod.LOGGER.warn("invalid item: $item")
                    this.resultStack = ItemStack.EMPTY
                } else {
                    this.resultStack = ItemStack(item, count)
                }
            } else if (!this.getResultList().isEmpty()) {
                this.resultStack = ItemStack(this.getResultList().random(), count)
            } else {
                this.resultStack = ItemStack.EMPTY
            }

            return this.resultStack!!
        }

        fun getResultList(): MutableList<Item> {
            if (this.list != null) return this.list!!
            if (this.tag.isEmpty()) return mutableListOf()

            val tags = ForgeRegistries.ITEMS.tags() ?: return mutableListOf()
            val itemTag = tags.getTag(ItemTags.create(ResourceLocation(this.tag)))

            val list = mutableListOf<Item>()
            itemTag.forEach { list.add(it) }
            list.sortBy { it.descriptionId }
            this.list = list
            return this.list!!
        }

        fun isRandom() = this.tag.isNotEmpty()

        fun rollItem(): ItemStack {
            if (this.isRandom() && !this.getResultList().isEmpty()) {
                return ItemStack(this.getResultList().random(), count)
            }
            return this.getResult()
        }
    }

    class Serializer : RecipeSerializer<ResearchingRecipe> {
        private fun ingredientOf(json: JsonObject, name: String): Ingredient {
            if (!json.has(name)) return Ingredient.EMPTY
            return Ingredient.fromJson(
                if (GsonHelper.isArrayNode(json, name))
                    GsonHelper.getAsJsonArray(json, name)
                else GsonHelper.getAsJsonObject(json, name)
            )
        }

        override fun fromJson(
            id: ResourceLocation,
            json: JsonObject
        ): ResearchingRecipe {
            val input = ingredientOf(json, "input")
            val base = ingredientOf(json, "base")
            val addition = ingredientOf(json, "addition")
            val special = ingredientOf(json, "special")
            val selectable = if (json.has("selectable")) GsonHelper.getAsBoolean(json, "selectable") else false
            val time = if (json.has("time")) GsonHelper.getAsInt(json, "time") else 100
            val result = DataLoader.GSON.fromJson(json.get("result"), Result::class.java)
            return ResearchingRecipe(id, input, base, addition, special, selectable, time, result)
        }

        override fun fromNetwork(
            id: ResourceLocation,
            buffer: FriendlyByteBuf
        ): ResearchingRecipe {
            val input = Ingredient.fromNetwork(buffer)
            val base = Ingredient.fromNetwork(buffer)
            val addition = Ingredient.fromNetwork(buffer)
            val special = Ingredient.fromNetwork(buffer)
            val selectable = buffer.readBoolean()
            val time = buffer.readInt()
            val result = buffer.readItem()

            val res = Result()
            res.resultStack = result
            return ResearchingRecipe(id, input, base, addition, special, selectable, time, res)
        }

        override fun toNetwork(
            buffer: FriendlyByteBuf,
            recipe: ResearchingRecipe
        ) {
            recipe.input.toNetwork(buffer)
            recipe.base.toNetwork(buffer)
            recipe.addition.toNetwork(buffer)
            recipe.special.toNetwork(buffer)
            buffer.writeBoolean(recipe.selectable)
            buffer.writeInt(recipe.time)
            buffer.writeItem(recipe.result.getResult())
        }
    }
}