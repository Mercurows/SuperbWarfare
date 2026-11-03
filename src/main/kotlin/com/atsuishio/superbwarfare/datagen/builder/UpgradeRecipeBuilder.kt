package com.atsuishio.superbwarfare.datagen.builder

import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.recipe.UpgradeRecipe
import net.minecraft.advancements.AdvancementRequirements
import net.minecraft.advancements.AdvancementRewards
import net.minecraft.advancements.Criterion
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger
import net.minecraft.core.NonNullList
import net.minecraft.data.recipes.RecipeBuilder
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.ShapelessRecipe
import net.minecraft.world.level.ItemLike

/** Serializes upgrade recipes directly, including the base gun and recipe-book unlock. */
class UpgradeRecipeBuilder(base: ItemLike, result: ItemLike) : RecipeBuilder {
    private val base = base.asItem()
    private val result = result.asItem()
    private val materials = mutableListOf<Ingredient>()
    private val criteria = linkedMapOf<String, Criterion<*>>()
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

    override fun unlockedBy(name: String, criterion: Criterion<*>): UpgradeRecipeBuilder {
        criteria[name] = criterion
        return this
    }

    override fun group(name: String?): UpgradeRecipeBuilder {
        group = name ?: ""
        return this
    }

    override fun getResult(): Item = result

    override fun save(writer: RecipeOutput, id: ResourceLocation) {
        check(criteria.isNotEmpty()) { "No way of obtaining upgrade recipe $id" }
        require(materials.isNotEmpty()) { "Upgrade requires at least one material" }
        require(materials.none { it.isEmpty || it.test(ItemStack(base)) || it.items.any { stack -> stack.item is GunItem } }) {
            "Upgrade materials must be nonempty and must not contain guns"
        }
        val advancement = writer.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id))
            .requirements(AdvancementRequirements.Strategy.OR)
        criteria.forEach { (name, criterion) -> advancement.addCriterion(name, criterion) }
        val ingredients = NonNullList.create<Ingredient>().apply {
            add(Ingredient.of(base))
            addAll(materials)
        }
        val recipe = UpgradeRecipe(
            base, ShapelessRecipe(group, CraftingBookCategory.EQUIPMENT, ItemStack(result), ingredients)
        )
        writer.accept(id, recipe, advancement.build(id.withPrefix("recipes/${RecipeCategory.COMBAT.folderName}/")))
    }
}
