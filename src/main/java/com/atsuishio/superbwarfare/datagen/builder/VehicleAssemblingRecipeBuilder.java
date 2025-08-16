package com.atsuishio.superbwarfare.datagen.builder;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModRecipes;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

public class VehicleAssemblingRecipeBuilder implements RecipeBuilder {

    private final Item result;
    @Nullable
    private final EntityType<?> entityType;
    private final int count;
    private final VehicleAssemblingRecipe.Category category;
    private final Map<String, Integer> ingredients = Maps.newLinkedHashMap();
    private final Advancement.Builder advancement = Advancement.Builder.recipeAdvancement();

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
    public RecipeBuilder unlockedBy(String pCriterionName, CriterionTriggerInstance pCriterionTrigger) {
        this.advancement.addCriterion(pCriterionName, pCriterionTrigger);
        return this;
    }

    @Override
    public RecipeBuilder group(@Nullable String pGroupName) {
        return this;
    }

    @Override
    public Item getResult() {
        return this.result;
    }

    @Override
    public void save(Consumer<FinishedRecipe> pFinishedRecipeConsumer, ResourceLocation pRecipeId) {
        this.ensureValid(pRecipeId);
        this.advancement.parent(ROOT_RECIPE_ADVANCEMENT).addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pRecipeId)).rewards(AdvancementRewards.Builder.recipe(pRecipeId)).requirements(RequirementsStrategy.OR);
        if (this.entityType == null) {
            pFinishedRecipeConsumer.accept(
                    new Result(
                            pRecipeId,
                            this.ingredients,
                            this.category,
                            this.result,
                            this.count,
                            this.advancement,
                            pRecipeId.withPrefix("recipes/" + RecipeCategory.MISC.getFolderName() + "/")
                    )
            );
        } else {
            pFinishedRecipeConsumer.accept(
                    new Result(
                            pRecipeId,
                            this.ingredients,
                            this.category,
                            this.entityType,
                            this.advancement,
                            pRecipeId.withPrefix("recipes/" + RecipeCategory.MISC.getFolderName() + "/")
                    )
            );
        }
    }

    private void ensureValid(ResourceLocation pId) {
        if (this.advancement.getCriteria().isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + pId);
        }
    }

    static class Result implements FinishedRecipe {

        private final ResourceLocation id;
        private final Map<String, Integer> ingredients;
        private final VehicleAssemblingRecipe.Category category;
        private final Item result;
        private final int count;
        @Nullable
        private final EntityType<?> entityType;
        private final Advancement.Builder advancement;
        private final ResourceLocation advancementId;

        public Result(ResourceLocation id, Map<String, Integer> ingredients, VehicleAssemblingRecipe.Category category, Item result, int count, Advancement.Builder advancement, ResourceLocation advancementId) {
            this.id = id;
            this.ingredients = ingredients;
            this.category = category;
            this.result = result;
            this.count = count;
            this.entityType = null;
            this.advancement = advancement;
            this.advancementId = advancementId;
        }

        public Result(ResourceLocation id, Map<String, Integer> ingredients, VehicleAssemblingRecipe.Category category, @Nullable EntityType<?> entityType, Advancement.Builder advancement, ResourceLocation advancementId) {
            this.id = id;
            this.ingredients = ingredients;
            this.category = category;
            this.result = ModItems.CONTAINER.get();
            this.count = 1;
            this.entityType = entityType;
            this.advancement = advancement;
            this.advancementId = advancementId;
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            JsonArray jsonarray = new JsonArray();

            for (var pair : this.ingredients.entrySet()) {
                var ingredient = pair.getKey();
                var count = pair.getValue();

                if (count > 1) {
                    jsonarray.add(count + " " + ingredient);
                } else {
                    jsonarray.add(ingredient);
                }
            }

            json.add("inputs", jsonarray);
            json.addProperty("category", this.category.getName());

            JsonObject res = new JsonObject();
            if (this.entityType != null) {
                res.addProperty("entity", EntityType.getKey(this.entityType).toString());
            } else {
                res.addProperty("item", BuiltInRegistries.ITEM.getKey(this.result).toString());
                if (this.count > 1) {
                    res.addProperty("count", this.count);
                }
            }
            json.add("result", res);
        }

        @Override
        public ResourceLocation getId() {
            return this.id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return ModRecipes.VEHICLE_ASSEMBLING_SERIALIZER.get();
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return this.advancement.serializeToJson();
        }

        @Nullable
        @Override
        public ResourceLocation getAdvancementId() {
            return this.advancementId;
        }
    }
}
