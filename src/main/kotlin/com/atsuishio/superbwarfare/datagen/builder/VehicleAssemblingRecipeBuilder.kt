package com.atsuishio.superbwarfare.datagen.builder

import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe
import com.google.common.collect.Maps
import net.minecraft.advancements.AdvancementRequirements
import net.minecraft.advancements.AdvancementRewards
import net.minecraft.advancements.Criterion
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.recipes.RecipeBuilder
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.ItemLike
import java.util.*

class VehicleAssemblingRecipeBuilder : RecipeBuilder {
    private val result: Item
    private val entityType: EntityType<*>?
    private val count: Int
    private val category: VehicleAssemblingRecipe.Category
    private val ingredients: MutableMap<String, Int> = Maps.newLinkedHashMap()
    private val criteria: MutableMap<String, Criterion<*>> = LinkedHashMap()

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
            BuiltInRegistries.ITEM.getKey(item.asItem()).toString(),
            count
        ) { _, v -> count + v }
        return this
    }

    @JvmOverloads
    fun require(tag: TagKey<Item>, count: Int = 1): VehicleAssemblingRecipeBuilder {
        this.ingredients.merge("#" + tag.location(), count) { _, v -> count + v }
        return this
    }

    override fun unlockedBy(s: String, criterion: Criterion<*>): RecipeBuilder {
        this.criteria[s] = criterion
        return this
    }

    override fun group(s: String?): RecipeBuilder {
        return this
    }

    override fun getResult(): Item {
        return this.result
    }

    override fun save(recipeOutput: RecipeOutput, pRecipeId: ResourceLocation) {
        this.ensureValid(pRecipeId)
        val builder =
            recipeOutput.advancement().addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pRecipeId))
                .rewards(AdvancementRewards.Builder.recipe(pRecipeId)).requirements(AdvancementRequirements.Strategy.OR)
        Objects.requireNonNull(builder)
        this.criteria.forEach { (key, criterion) -> builder.addCriterion(key, criterion) }
        val recipe = if (this.entityType != null) {
            VehicleAssemblingRecipe.create(this.ingredients, this.category, this.entityType)
        } else {
            VehicleAssemblingRecipe.create(this.ingredients, this.category, this.result, this.count)
        }
        recipeOutput.accept(
            pRecipeId,
            recipe,
            builder.build(pRecipeId.withPrefix("recipes/" + RecipeCategory.MISC.folderName + "/"))
        )
    }

    private fun ensureValid(id: ResourceLocation?) {
        check(!this.criteria.isEmpty()) { "No way of obtaining recipe $id" }
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