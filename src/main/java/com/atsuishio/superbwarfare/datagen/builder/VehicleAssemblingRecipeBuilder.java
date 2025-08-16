package com.atsuishio.superbwarfare.datagen.builder;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe;
import com.google.common.collect.Maps;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class VehicleAssemblingRecipeBuilder implements RecipeBuilder {

    private final Item result;
    @Nullable
    private final EntityType<?> entityType;
    private final int count;
    private final VehicleAssemblingRecipe.Category category;
    private final Map<String, Integer> ingredients = Maps.newLinkedHashMap();
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    public VehicleAssemblingRecipeBuilder(ItemLike pResult, int pCount, VehicleAssemblingRecipe.Category category) {
        this.result = pResult.asItem();
        this.entityType = null;
        this.count = pCount;
        this.category = category;
    }

    public VehicleAssemblingRecipeBuilder(EntityType<?> type, VehicleAssemblingRecipe.Category category) {
        this.result = ModItems.CONTAINER.get();
        this.entityType = type;
        this.count = 1;
        this.category = category;
    }

    public static VehicleAssemblingRecipeBuilder item(ItemLike pResult, int pCount, VehicleAssemblingRecipe.Category category) {
        return new VehicleAssemblingRecipeBuilder(pResult, pCount, category);
    }

    public static VehicleAssemblingRecipeBuilder entity(EntityType<?> type, VehicleAssemblingRecipe.Category category) {
        return new VehicleAssemblingRecipeBuilder(type, category);
    }

    public VehicleAssemblingRecipeBuilder require(ItemLike item, int count) {
        this.ingredients.merge(BuiltInRegistries.ITEM.getKey(item.asItem()).toString(), count, (k, v) -> count + v);
        return this;
    }

    public VehicleAssemblingRecipeBuilder require(TagKey<Item> tag, int count) {
        this.ingredients.merge("#" + tag.location(), count, (k, v) -> count + v);
        return this;
    }

    public VehicleAssemblingRecipeBuilder require(ItemLike item) {
        return this.require(item, 1);
    }

    public VehicleAssemblingRecipeBuilder require(TagKey<Item> tag) {
        return this.require(tag, 1);
    }

    @Override
    public RecipeBuilder unlockedBy(String s, Criterion<?> criterion) {
        this.criteria.put(s, criterion);
        return this;
    }

    @Override
    public RecipeBuilder group(@Nullable String s) {
        return this;
    }

    @Override
    public Item getResult() {
        return this.result;
    }

    @Override
    public void save(RecipeOutput recipeOutput, ResourceLocation pRecipeId) {
        this.ensureValid(pRecipeId);
        Advancement.Builder builder = recipeOutput.advancement().addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pRecipeId)).rewards(AdvancementRewards.Builder.recipe(pRecipeId)).requirements(AdvancementRequirements.Strategy.OR);
        Objects.requireNonNull(builder);
        this.criteria.forEach(builder::addCriterion);
        VehicleAssemblingRecipe recipe;
        if (this.entityType != null) {
            recipe = VehicleAssemblingRecipe.create(this.ingredients, this.category, this.entityType);
        } else {
            recipe = VehicleAssemblingRecipe.create(this.ingredients, this.category, this.result, this.count);
        }
        recipeOutput.accept(pRecipeId, recipe, builder.build(pRecipeId.withPrefix("recipes/" + RecipeCategory.MISC.getFolderName() + "/")));
    }

    private void ensureValid(ResourceLocation id) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }
    }
}
