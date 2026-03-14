package com.atsuishio.superbwarfare.datagen.builder

import com.atsuishio.superbwarfare.init.ModRecipes
import com.google.gson.JsonObject
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementRewards
import net.minecraft.advancements.CriterionTriggerInstance
import net.minecraft.advancements.RequirementsStrategy
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.data.recipes.RecipeBuilder
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.ItemLike
import net.minecraftforge.registries.ForgeRegistries
import java.util.function.Consumer

open class ResearchingRecipeBuilder private constructor(
    var resultItem: Item? = null,
    val resultTag: TagKey<Item>? = null,
    val count: Int = 1,
    val input: Ingredient
) : RecipeBuilder {
    private val advancement: Advancement.Builder = Advancement.Builder.recipeAdvancement()
    private var base: Ingredient? = null
    private var addition: Ingredient? = null
    private var special: Ingredient? = null
    private var color: Int = 0
    private var selectable: Boolean = false
    private var time: Int = 1200

    init {
        if (this.resultItem == null && this.resultTag != null) {
            val tags = ForgeRegistries.ITEMS.tags()
            val list = tags?.getTag(this.resultTag)
            if (list != null && !list.isEmpty) {
                this.resultItem = list.first()
            }
        }
    }

    override fun unlockedBy(
        pCriterionName: String,
        pCriterionTrigger: CriterionTriggerInstance
    ): RecipeBuilder {
        this.advancement.addCriterion(pCriterionName, pCriterionTrigger)
        return this
    }

    override fun group(pGroupName: String?): RecipeBuilder {
        return this
    }

    override fun getResult(): Item {
        return this.resultItem ?: Items.AIR
    }

    override fun save(
        consumer: Consumer<FinishedRecipe>,
        id: ResourceLocation
    ) {
        this.ensureValid(id)
        this.advancement.parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id))
            .requirements(RequirementsStrategy.OR)
        if (this.resultTag != null) {
            consumer.accept(
                Result(
                    id,
                    input,
                    base,
                    addition,
                    special,
                    resultTag = resultTag,
                    color = color,
                    count = count,
                    selectable = selectable,
                    time = time,
                    advancement = advancement,
                    advancementLocation = id.withPrefix("recipes/" + RecipeCategory.MISC.folderName + "/")
                )
            )
        } else if (this.resultItem != null) {
            consumer.accept(
                Result(
                    id,
                    input,
                    base,
                    addition,
                    special,
                    resultItem,
                    color = color,
                    count = count,
                    selectable = selectable,
                    time = time,
                    advancement = advancement,
                    advancementLocation = id.withPrefix("recipes/" + RecipeCategory.MISC.folderName + "/")
                )
            )
        }
    }

    private fun ensureValid(pId: ResourceLocation) {
        check(!this.advancement.criteria.isEmpty()) { "No way of obtaining recipe $pId" }
    }

    fun base(ingredient: Ingredient): ResearchingRecipeBuilder {
        this.base = ingredient
        return this
    }

    fun base(item: ItemLike) = this.base(Ingredient.of(item))

    fun base(tag: TagKey<Item>) = this.base(Ingredient.of(tag))

    fun addition(ingredient: Ingredient): ResearchingRecipeBuilder {
        this.addition = ingredient
        return this
    }

    fun addition(item: ItemLike) = this.addition(Ingredient.of(item))

    fun addition(tag: TagKey<Item>) = this.addition(Ingredient.of(tag))

    fun special(ingredient: Ingredient): ResearchingRecipeBuilder {
        this.special = ingredient
        return this
    }

    fun special(item: ItemLike) = this.special(Ingredient.of(item))

    fun special(tag: TagKey<Item>) = this.special(Ingredient.of(tag))

    fun color(color: Int): ResearchingRecipeBuilder {
        this.color = color.coerceIn(0, 4)
        return this
    }

    fun time(time: Int): ResearchingRecipeBuilder {
        this.time = time
        return this
    }

    fun selectable(): ResearchingRecipeBuilder {
        this.selectable = true
        return this
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun item(result: Item, count: Int = 1, input: ItemLike) =
            ResearchingRecipeBuilder(resultItem = result, count = count, input = Ingredient.of(input))

        @JvmStatic
        @JvmOverloads
        fun item(result: Item, count: Int = 1, input: TagKey<Item>) =
            ResearchingRecipeBuilder(resultItem = result, count = count, input = Ingredient.of(input))

        @JvmStatic
        @JvmOverloads
        fun tag(resultTag: TagKey<Item>, count: Int = 1, input: ItemLike) =
            ResearchingRecipeBuilder(resultTag = resultTag, count = count, input = Ingredient.of(input))

        @JvmStatic
        @JvmOverloads
        fun tag(resultTag: TagKey<Item>, count: Int = 1, input: TagKey<Item>) =
            ResearchingRecipeBuilder(resultTag = resultTag, count = count, input = Ingredient.of(input))
    }

    class Result(
        val recipeId: ResourceLocation,
        val input: Ingredient,
        val base: Ingredient? = null,
        val addition: Ingredient? = null,
        val special: Ingredient? = null,
        val resultItem: Item? = null,
        val resultTag: TagKey<Item>? = null,
        val count: Int = 1,
        val selectable: Boolean = false,
        val time: Int = 1200,
        val color: Int = 0,
        val advancement: Advancement.Builder,
        val advancementLocation: ResourceLocation
    ) : FinishedRecipe {
        @Suppress("DEPRECATION")
        override fun serializeRecipeData(json: JsonObject) {
            json.add("input", input.toJson())
            if (base != null) {
                json.add("base", base.toJson())
            }
            if (addition != null) {
                json.add("addition", addition.toJson())
            }
            if (special != null) {
                json.add("special", special.toJson())
            }
            if (selectable) {
                json.addProperty("selectable", true)
            }
            json.addProperty("time", time)
            if (color != 0) {
                json.addProperty("color", color)
            }
            val res = JsonObject()
            if (resultItem != null) {
                res.addProperty("item", BuiltInRegistries.ITEM.getKey(resultItem).toString())
            } else if (resultTag != null) {
                res.addProperty("tag", resultTag.location.toString())
            }
            if (count != 1) {
                res.addProperty("count", count)
            }
            json.add("result", res)
        }

        override fun getId(): ResourceLocation = this.recipeId

        override fun getType(): RecipeSerializer<*> = ModRecipes.RESEARCHING_SERIALIZER.get()

        override fun serializeAdvancement(): JsonObject = this.advancement.serializeToJson()

        override fun getAdvancementId(): ResourceLocation = this.advancementLocation
    }
}