package com.atsuishio.superbwarfare.datagen.builder

import com.atsuishio.superbwarfare.init.ModRecipes
import com.atsuishio.superbwarfare.item.gun.GunItem
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
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.ItemLike
import net.minecraftforge.registries.ForgeRegistries
import java.util.function.Consumer

/** Serializes upgrade recipes directly, including the base gun and recipe-book unlock. */
class UpgradeRecipeBuilder(base: ItemLike, result: ItemLike) : RecipeBuilder {
    private val base = base.asItem()
    private val result = result.asItem()
    private val materials = mutableListOf<Ingredient>()
    private val advancement = Advancement.Builder.recipeAdvancement()
    private var group = ""

    init {
        require(this.base is GunItem && this.result is GunItem) { "Upgrade base and result must be guns" }
    }

    fun requires(item: ItemLike, count: Int = 1) = requires(Ingredient.of(item), count)

    fun requires(tag: TagKey<Item>, count: Int = 1) = requires(Ingredient.of(tag), count)

    fun requires(ingredient: Ingredient, count: Int = 1): UpgradeRecipeBuilder {
        require(count in 1..8 && materials.size <= 8 - count) { "Upgrade needs 1-8 material slots" }
        repeat(count) { materials += ingredient }
        return this
    }

    override fun unlockedBy(name: String, criterion: CriterionTriggerInstance): UpgradeRecipeBuilder {
        advancement.addCriterion(name, criterion)
        return this
    }

    override fun group(name: String?): UpgradeRecipeBuilder {
        group = name ?: ""
        return this
    }

    override fun getResult(): Item = result

    override fun save(writer: Consumer<FinishedRecipe>, id: ResourceLocation) {
        check(advancement.criteria.isNotEmpty()) { "No way of obtaining upgrade recipe $id" }
        require(materials.isNotEmpty()) { "Upgrade requires at least one material" }
        require(materials.none { it.isEmpty || it.test(ItemStack(base)) || it.items.any { stack -> stack.item is GunItem } }) {
            "Upgrade materials must be nonempty and must not contain guns"
        }
        advancement.parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id))
            .requirements(RequirementsStrategy.OR)
        val ingredients = listOf(Ingredient.of(base)) + materials
        val recipeGroup = group
        writer.accept(object : FinishedRecipe {
            override fun getId() = id

            override fun getType(): RecipeSerializer<*> = ModRecipes.UPGRADE_SERIALIZER.get()

            override fun serializeRecipeData(json: JsonObject) {
                json.addProperty("base", ForgeRegistries.ITEMS.getKey(base).toString())
                json.addProperty("category", "equipment")
                if (recipeGroup.isNotEmpty()) json.addProperty("group", recipeGroup)
                json.add("ingredients", JsonArray().apply { ingredients.forEach { add(it.toJson()) } })
                json.add("result", JsonObject().apply {
                    addProperty("item", ForgeRegistries.ITEMS.getKey(result).toString())
                })
            }

            override fun serializeAdvancement(): JsonObject = advancement.serializeToJson()

            override fun getAdvancementId(): ResourceLocation = id.withPrefix("recipes/${RecipeCategory.COMBAT.folderName}/")
        })
    }
}
