package com.atsuishio.superbwarfare.recipe

import com.atsuishio.superbwarfare.init.ModRecipes
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.ItemTags
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level

// TODO 怎么实现这玩意
class ResearchingRecipe(
    val recipeId: ResourceLocation,
    val input: Ingredient,
    val base: Ingredient,
    val addition: Ingredient,
    val special: Ingredient,
    val time: Int,
    val result: Result
) : Recipe<SimpleContainer> {
    override fun matches(
        container: SimpleContainer,
        level: Level
    ): Boolean {
        if (level.isClientSide || container.containerSize < 4) {
            return false
        }
        return input.test(container.getItem(0))
                && base.test(container.getItem(1))
                && addition.test(container.getItem(2))
                && special.test(container.getItem(3))
    }

    override fun assemble(input: SimpleContainer, registries: HolderLookup.Provider): ItemStack =
        this.result.getResult().copy()

    override fun isSpecial() = true

    override fun canCraftInDimensions(pWidth: Int, pHeight: Int) = true

    override fun getResultItem(registries: HolderLookup.Provider): ItemStack = this.result.getResult().copy()

    override fun getSerializer(): RecipeSerializer<*> = TODO("RESEARCHING_SERIALIZER")
//        ModRecipes.RESEARCHING_SERIALIZER.get()

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
        var list: List<Item>? = null

        fun getResult(): ItemStack {
            if (this.resultStack != null) return this.resultStack!!
            if (!item.isEmpty()) {
                val item = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(item))
                this.resultStack = ItemStack(item, count)
            } else if (!this.getResultList().isEmpty()) {
                this.resultStack = ItemStack(this.getResultList().random(), count)
            } else {
                this.resultStack = ItemStack.EMPTY
            }

            return this.resultStack!!
        }

        fun getResultList(): List<Item> {
            if (this.list != null) return this.list!!
            if (this.tag.isEmpty()) return mutableListOf()

            val tagKey = ItemTags.create(ResourceLocation.withDefaultNamespace(this.tag))
            val tags = BuiltInRegistries.ITEM.getTag(tagKey)
                .map { items -> items.map { it.value() } }

            if (tags.isPresent) {
                val itemsList = tags.get()
                this.list = itemsList
                return itemsList
            }

            return mutableListOf()
        }

        fun isRandom() = this.tag.isNotEmpty()

        fun rollItem(): ItemStack {
            if (this.isRandom()) {
                return ItemStack(this.getResultList().random(), count)
            }
            return this.getResult()
        }
    }

    // TODO 怎么序列化这一坨
//    class Serializer : RecipeSerializer<ResearchingRecipe> {
//        val CODEC = Codec.of<ResearchingRecipe>(Encoder { a, b, c ->
//            return@Encoder ResearchingRecipe()
//        }, Decoder { a, b ->
//            return@Decoder ResearchingRecipe()
//        })
//
//        private fun ingredientOf(json: JsonObject, name: String): Ingredient {
//            return Ingredient.fromJson(
//                if (GsonHelper.isArrayNode(json, name))
//                    GsonHelper.getAsJsonArray(json, name)
//                else GsonHelper.getAsJsonObject(json, name)
//            )
//        }
//
//        override fun fromJson(
//            id: ResourceLocation,
//            json: JsonObject
//        ): ResearchingRecipe {
//            val input = ingredientOf(json, "input")
//            val base = ingredientOf(json, "base")
//            val addition = ingredientOf(json, "addition")
//            val special = ingredientOf(json, "special")
//            val time = GsonHelper.getAsInt(json, "time")
//            val result = DataLoader.GSON.fromJson(json.get("result"), Result::class.java)
//            return ResearchingRecipe(id, input, base, addition, special, time, result)
//        }
//
//        override fun fromNetwork(
//            id: ResourceLocation,
//            buffer: FriendlyByteBuf
//        ): ResearchingRecipe {
//            val input = Ingredient.fromNetwork(buffer)
//            val base = Ingredient.fromNetwork(buffer)
//            val addition = Ingredient.fromNetwork(buffer)
//            val special = Ingredient.fromNetwork(buffer)
//            val time = buffer.readInt()
//            val result = buffer.readItem()
//
//            val res = Result()
//            res.resultStack = result
//            return ResearchingRecipe(id, input, base, addition, special, time, res)
//        }
//
//        override fun toNetwork(
//            buffer: FriendlyByteBuf,
//            recipe: ResearchingRecipe
//        ) {
//            recipe.input.toNetwork(buffer)
//            recipe.base.toNetwork(buffer)
//            recipe.addition.toNetwork(buffer)
//            recipe.special.toNetwork(buffer)
//            buffer.writeInt(recipe.time)
//            buffer.writeItem(recipe.result.getResult())
//        }
//    }
}