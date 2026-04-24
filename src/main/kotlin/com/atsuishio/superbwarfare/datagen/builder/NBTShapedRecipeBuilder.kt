package com.atsuishio.superbwarfare.datagen.builder

import com.atsuishio.superbwarfare.tools.NBTTool
import com.google.common.collect.Sets
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementRewards
import net.minecraft.advancements.CriterionTriggerInstance
import net.minecraft.advancements.RequirementsStrategy
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger
import net.minecraft.data.recipes.CraftingRecipeBuilder
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.data.recipes.RecipeBuilder
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.ItemLike
import net.minecraftforge.registries.ForgeRegistries
import java.util.function.Consumer

/**
 * SB ojng写的玩意全是private的，我怎么extends
 */
class NBTShapedRecipeBuilder(val category: RecipeCategory, pResult: ItemLike, val count: Int) : CraftingRecipeBuilder(),
    RecipeBuilder {
    val itemResult: Item = pResult.asItem()
    val rows: MutableList<String> = arrayListOf()
    val key: MutableMap<Char?, Ingredient?> = linkedMapOf()
    val advancement: Advancement.Builder = Advancement.Builder.recipeAdvancement()
    var group: String? = null
    var showNotification: Boolean = true
    var nbt: CompoundTag? = null

    fun withNBT(nbt: CompoundTag): NBTShapedRecipeBuilder {
        this.nbt = nbt
        return this
    }

    fun withNBT(consumer: Consumer<CompoundTag>): NBTShapedRecipeBuilder {
        val tag = CompoundTag()
        consumer.accept(tag)
        this.nbt = tag
        return this
    }

    fun define(pSymbol: Char, pTag: TagKey<Item>): NBTShapedRecipeBuilder {
        return this.define(pSymbol, Ingredient.of(pTag))
    }

    fun define(pSymbol: Char, pItem: ItemLike): NBTShapedRecipeBuilder {
        return this.define(pSymbol, Ingredient.of(pItem))
    }

    fun define(pSymbol: Char, pIngredient: Ingredient): NBTShapedRecipeBuilder {
        require(!this.key.containsKey(pSymbol)) { "Symbol '$pSymbol' is already defined!" }
        require(pSymbol != ' ') { "Symbol ' ' (whitespace) is reserved and cannot be defined" }
        this.key[pSymbol] = pIngredient
        return this
    }

    fun pattern(pPattern: String): NBTShapedRecipeBuilder {
        require(!(!this.rows.isEmpty() && pPattern.length != this.rows[0].length)) { "Pattern must be the same width on every line!" }
        this.rows.add(pPattern)
        return this
    }

    override fun unlockedBy(
        pCriterionName: String,
        pCriterionTrigger: CriterionTriggerInstance
    ): NBTShapedRecipeBuilder {
        this.advancement.addCriterion(pCriterionName, pCriterionTrigger)
        return this
    }

    override fun group(pGroupName: String?): NBTShapedRecipeBuilder {
        this.group = pGroupName
        return this
    }

    fun showNotification(pShowNotification: Boolean): NBTShapedRecipeBuilder {
        this.showNotification = pShowNotification
        return this
    }

    override fun getResult(): Item {
        return this.itemResult
    }

    override fun save(pFinishedRecipeConsumer: Consumer<FinishedRecipe>, pRecipeId: ResourceLocation) {
        this.ensureValid(pRecipeId)
        this.advancement.parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pRecipeId))
            .rewards(AdvancementRewards.Builder.recipe(pRecipeId)).requirements(RequirementsStrategy.OR)
        pFinishedRecipeConsumer.accept(
            Result(
                pRecipeId, this.itemResult, this.count, (if (this.group == null) "" else this.group)!!,
                determineBookCategory(this.category), this.rows, this.key, this.advancement,
                pRecipeId.withPrefix("recipes/" + this.category.folderName + "/"), this.showNotification,
                this.nbt
            )
        )
    }

    private fun ensureValid(pId: ResourceLocation?) {
        check(!this.rows.isEmpty()) { "No pattern is defined for shaped recipe $pId!" }
        val set: MutableSet<Char?> = Sets.newHashSet<Char?>(this.key.keys)
        set.remove(' ')

        for (s in this.rows) {
            for (i in 0..<s.length) {
                val c0 = s[i]
                check(!(!this.key.containsKey(c0) && c0 != ' ')) { "Pattern in recipe $pId uses undefined symbol '$c0'" }

                set.remove(c0)
            }
        }

        check(set.isEmpty()) { "Ingredients are defined but not used in pattern for recipe $pId" }
        check(!(this.rows.size == 1 && this.rows[0].length == 1)) { "Shaped recipe $pId only takes in a single item - should it be a shapeless recipe instead?" }
        check(!this.advancement.criteria.isEmpty()) { "No way of obtaining recipe $pId" }
    }

    private class Result @JvmOverloads constructor(
        private val id: ResourceLocation,
        private val result: Item,
        private val count: Int,
        private val group: String,
        pCategory: CraftingBookCategory,
        private val pattern: MutableList<String>,
        private val key: MutableMap<Char?, Ingredient?>,
        private val advancement: Advancement.Builder,
        private val advancementId: ResourceLocation,
        private val showNotification: Boolean,
        private val nbt: CompoundTag? = null
    ) : CraftingResult(pCategory) {
        override fun serializeRecipeData(pJson: JsonObject) {
            super.serializeRecipeData(pJson)
            if (!this.group.isEmpty()) {
                pJson.addProperty("group", this.group)
            }

            val jsonArray = JsonArray()
            for (s in this.pattern) {
                jsonArray.add(s)
            }
            pJson.add("pattern", jsonArray)

            val jsonObject = JsonObject()
            for (entry in this.key.entries) {
                jsonObject.add(entry.key.toString(), entry.value!!.toJson())
            }

            pJson.add("key", jsonObject)
            val result = JsonObject()
            result.addProperty("item", ForgeRegistries.ITEMS.getKey(this.result).toString())
            if (this.count > 1) {
                result.addProperty("count", this.count)
            }
            if (this.nbt != null) {
                result.add("nbt", NBTTool.parseTag(this.nbt))
            }

            pJson.add("result", result)
            pJson.addProperty("show_notification", this.showNotification)
        }

        override fun getType(): RecipeSerializer<*> {
            return RecipeSerializer.SHAPED_RECIPE
        }

        override fun getId(): ResourceLocation {
            return this.id
        }

        override fun serializeAdvancement(): JsonObject {
            return this.advancement.serializeToJson()
        }

        override fun getAdvancementId(): ResourceLocation {
            return this.advancementId
        }
    }

    companion object {
        @JvmOverloads
        fun shaped(pCategory: RecipeCategory, pResult: ItemLike, pCount: Int = 1): NBTShapedRecipeBuilder {
            return NBTShapedRecipeBuilder(pCategory, pResult, pCount)
        }
    }
}
