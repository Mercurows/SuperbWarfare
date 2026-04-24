package com.atsuishio.superbwarfare.datagen.builder

import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModRecipes
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementRewards
import net.minecraft.advancements.CriterionTriggerInstance
import net.minecraft.advancements.RequirementsStrategy
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.data.recipes.RecipeBuilder
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.ItemLike
import net.minecraftforge.registries.ForgeRegistries
import java.util.function.Consumer

class VehicleAssemblingRecipeBuilder : RecipeBuilder {
    private val result: Item
    private val entityType: EntityType<*>?
    private val count: Int
    private val category: VehicleAssemblingRecipe.Category
    private val ingredients: MutableMap<String, Int> = linkedMapOf()
    private val advancement: Advancement.Builder = Advancement.Builder.recipeAdvancement()

    constructor(pResult: ItemLike, pCount: Int, category: VehicleAssemblingRecipe.Category) {
        this.result = pResult.asItem()
        this.entityType = null
        this.count = pCount
        this.category = category
    }

    constructor(type: EntityType<*>, category: VehicleAssemblingRecipe.Category) {
        this.result = ModItems.CONTAINER.get()
        this.entityType = type
        this.count = 1
        this.category = category
    }

    @JvmOverloads
    fun require(item: ItemLike, count: Int = 1): VehicleAssemblingRecipeBuilder {
        this.ingredients.merge(
            ForgeRegistries.ITEMS.getKey(item.asItem()).toString(),
            count
        ) { _, v -> count + v }
        return this
    }

    @JvmOverloads
    fun require(tag: TagKey<Item>, count: Int = 1): VehicleAssemblingRecipeBuilder {
        this.ingredients.merge("#" + tag.location(), count) { _, v -> count + v }
        return this
    }

    override fun unlockedBy(pCriterionName: String, pCriterionTrigger: CriterionTriggerInstance): RecipeBuilder {
        this.advancement.addCriterion(pCriterionName, pCriterionTrigger)
        return this
    }

    override fun group(pGroupName: String?): RecipeBuilder {
        return this
    }

    override fun getResult(): Item {
        return this.result
    }

    override fun save(pFinishedRecipeConsumer: Consumer<FinishedRecipe>, pRecipeId: ResourceLocation) {
        this.ensureValid(pRecipeId)
        this.advancement.parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pRecipeId))
            .rewards(AdvancementRewards.Builder.recipe(pRecipeId)).requirements(RequirementsStrategy.OR)
        if (this.entityType == null) {
            pFinishedRecipeConsumer.accept(
                Result(
                    pRecipeId,
                    this.ingredients,
                    this.category,
                    this.result,
                    this.count,
                    this.advancement,
                    pRecipeId.withPrefix("recipes/" + RecipeCategory.MISC.folderName + "/")
                )
            )
        } else {
            pFinishedRecipeConsumer.accept(
                Result(
                    pRecipeId,
                    this.ingredients,
                    this.category,
                    this.entityType,
                    this.advancement,
                    pRecipeId.withPrefix("recipes/" + RecipeCategory.MISC.folderName + "/")
                )
            )
        }
    }

    private fun ensureValid(pId: ResourceLocation) {
        check(!this.advancement.criteria.isEmpty()) { "No way of obtaining recipe $pId" }
    }

    internal class Result : FinishedRecipe {
        private val id: ResourceLocation
        private val ingredients: MutableMap<String, Int>
        private val category: VehicleAssemblingRecipe.Category
        private val result: Item
        private val count: Int
        private val entityType: EntityType<*>?
        private val advancement: Advancement.Builder
        private val advancementId: ResourceLocation?

        constructor(
            id: ResourceLocation,
            ingredients: MutableMap<String, Int>,
            category: VehicleAssemblingRecipe.Category,
            result: Item,
            count: Int,
            advancement: Advancement.Builder,
            advancementId: ResourceLocation?
        ) {
            this.id = id
            this.ingredients = ingredients
            this.category = category
            this.result = result
            this.count = count
            this.entityType = null
            this.advancement = advancement
            this.advancementId = advancementId
        }

        constructor(
            id: ResourceLocation,
            ingredients: MutableMap<String, Int>,
            category: VehicleAssemblingRecipe.Category,
            entityType: EntityType<*>,
            advancement: Advancement.Builder,
            advancementId: ResourceLocation?
        ) {
            this.id = id
            this.ingredients = ingredients
            this.category = category
            this.result = ModItems.CONTAINER.get()
            this.count = 1
            this.entityType = entityType
            this.advancement = advancement
            this.advancementId = advancementId
        }

        override fun serializeRecipeData(json: JsonObject) {
            val jsonArray = JsonArray()

            for (pair in this.ingredients.entries) {
                val ingredient = pair.key
                val count = pair.value

                if (count > 1) {
                    jsonArray.add("$count $ingredient")
                } else {
                    jsonArray.add(ingredient)
                }
            }

            json.add("inputs", jsonArray)
            json.addProperty("category", this.category.typeName)

            val res = JsonObject()
            if (this.entityType != null) {
                res.addProperty("entity", EntityType.getKey(this.entityType).toString())
            } else {
                res.addProperty("item", ForgeRegistries.ITEMS.getKey(this.result).toString())
                if (this.count > 1) {
                    res.addProperty("count", this.count)
                }
            }
            json.add("result", res)
        }

        override fun getId(): ResourceLocation {
            return this.id
        }

        override fun getType(): RecipeSerializer<*> {
            return ModRecipes.VEHICLE_ASSEMBLING_SERIALIZER.get()
        }

        override fun serializeAdvancement(): JsonObject {
            return this.advancement.serializeToJson()
        }

        override fun getAdvancementId(): ResourceLocation? {
            return this.advancementId
        }
    }

    companion object {
        fun item(
            pResult: ItemLike,
            pCount: Int,
            category: VehicleAssemblingRecipe.Category
        ): VehicleAssemblingRecipeBuilder {
            return VehicleAssemblingRecipeBuilder(pResult, pCount, category)
        }

        fun entity(type: EntityType<*>, category: VehicleAssemblingRecipe.Category): VehicleAssemblingRecipeBuilder {
            return VehicleAssemblingRecipeBuilder(type, category)
        }
    }
}
