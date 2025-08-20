package com.atsuishio.superbwarfare.datagen.builder;

import com.atsuishio.superbwarfare.tools.NBTTool;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.CraftingRecipeBuilder;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * SB ojng写的玩意全是private的，我怎么extends
 */
public class NBTShapedRecipeBuilder extends CraftingRecipeBuilder implements RecipeBuilder {

    public final RecipeCategory category;
    public final Item result;
    public final int count;
    public final List<String> rows = Lists.newArrayList();
    public final Map<Character, Ingredient> key = Maps.newLinkedHashMap();
    public final Advancement.Builder advancement = Advancement.Builder.recipeAdvancement();
    @Nullable
    public String group;
    public boolean showNotification = true;
    public CompoundTag nbt;

    public NBTShapedRecipeBuilder(RecipeCategory pCategory, ItemLike pResult, int pCount) {
        this.category = pCategory;
        this.result = pResult.asItem();
        this.count = pCount;
    }

    public NBTShapedRecipeBuilder withNBT(CompoundTag nbt) {
        this.nbt = nbt;
        return this;
    }

    public NBTShapedRecipeBuilder withNBT(Consumer<CompoundTag> consumer) {
        CompoundTag tag = new CompoundTag();
        consumer.accept(tag);
        this.nbt = tag;
        return this;
    }

    public static NBTShapedRecipeBuilder shaped(RecipeCategory pCategory, ItemLike pResult) {
        return shaped(pCategory, pResult, 1);
    }

    public static NBTShapedRecipeBuilder shaped(RecipeCategory pCategory, ItemLike pResult, int pCount) {
        return new NBTShapedRecipeBuilder(pCategory, pResult, pCount);
    }

    public NBTShapedRecipeBuilder define(Character pSymbol, TagKey<Item> pTag) {
        return this.define(pSymbol, Ingredient.of(pTag));
    }

    public NBTShapedRecipeBuilder define(Character pSymbol, ItemLike pItem) {
        return this.define(pSymbol, Ingredient.of(pItem));
    }

    public NBTShapedRecipeBuilder define(Character pSymbol, Ingredient pIngredient) {
        if (this.key.containsKey(pSymbol)) {
            throw new IllegalArgumentException("Symbol '" + pSymbol + "' is already defined!");
        } else if (pSymbol == ' ') {
            throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
        } else {
            this.key.put(pSymbol, pIngredient);
            return this;
        }
    }

    public NBTShapedRecipeBuilder pattern(String pPattern) {
        if (!this.rows.isEmpty() && pPattern.length() != this.rows.get(0).length()) {
            throw new IllegalArgumentException("Pattern must be the same width on every line!");
        } else {
            this.rows.add(pPattern);
            return this;
        }
    }

    public NBTShapedRecipeBuilder unlockedBy(String pCriterionName, CriterionTriggerInstance pCriterionTrigger) {
        this.advancement.addCriterion(pCriterionName, pCriterionTrigger);
        return this;
    }

    public NBTShapedRecipeBuilder group(@Nullable String pGroupName) {
        this.group = pGroupName;
        return this;
    }

    public NBTShapedRecipeBuilder showNotification(boolean pShowNotification) {
        this.showNotification = pShowNotification;
        return this;
    }

    public Item getResult() {
        return this.result;
    }

    public void save(Consumer<FinishedRecipe> pFinishedRecipeConsumer, ResourceLocation pRecipeId) {
        this.ensureValid(pRecipeId);
        this.advancement.parent(ROOT_RECIPE_ADVANCEMENT).addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pRecipeId)).rewards(AdvancementRewards.Builder.recipe(pRecipeId)).requirements(RequirementsStrategy.OR);
        pFinishedRecipeConsumer.accept(new NBTShapedRecipeBuilder.Result(pRecipeId, this.result, this.count, this.group == null ? "" : this.group,
                determineBookCategory(this.category), this.rows, this.key, this.advancement,
                pRecipeId.withPrefix("recipes/" + this.category.getFolderName() + "/"), this.showNotification,
                this.nbt));
    }

    private void ensureValid(ResourceLocation pId) {
        if (this.rows.isEmpty()) {
            throw new IllegalStateException("No pattern is defined for shaped recipe " + pId + "!");
        } else {
            Set<Character> set = Sets.newHashSet(this.key.keySet());
            set.remove(' ');

            for (String s : this.rows) {
                for (int i = 0; i < s.length(); ++i) {
                    char c0 = s.charAt(i);
                    if (!this.key.containsKey(c0) && c0 != ' ') {
                        throw new IllegalStateException("Pattern in recipe " + pId + " uses undefined symbol '" + c0 + "'");
                    }

                    set.remove(c0);
                }
            }

            if (!set.isEmpty()) {
                throw new IllegalStateException("Ingredients are defined but not used in pattern for recipe " + pId);
            } else if (this.rows.size() == 1 && this.rows.get(0).length() == 1) {
                throw new IllegalStateException("Shaped recipe " + pId + " only takes in a single item - should it be a shapeless recipe instead?");
            } else if (this.advancement.getCriteria().isEmpty()) {
                throw new IllegalStateException("No way of obtaining recipe " + pId);
            }
        }
    }

    public static class Result extends CraftingRecipeBuilder.CraftingResult {
        private final ResourceLocation id;
        private final Item result;
        private final int count;
        private final String group;
        private final List<String> pattern;
        private final Map<Character, Ingredient> key;
        private final Advancement.Builder advancement;
        private final ResourceLocation advancementId;
        private final boolean showNotification;
        @Nullable
        private final CompoundTag nbt;

        public Result(ResourceLocation pId, Item pResult, int pCount, String pGroup, CraftingBookCategory pCategory, List<String> pPattern, Map<Character, Ingredient> pKey,
                      Advancement.Builder pAdvancement, ResourceLocation pAdvancementId, boolean pShowNotification, CompoundTag nbt) {
            super(pCategory);
            this.id = pId;
            this.result = pResult;
            this.count = pCount;
            this.group = pGroup;
            this.pattern = pPattern;
            this.key = pKey;
            this.advancement = pAdvancement;
            this.advancementId = pAdvancementId;
            this.showNotification = pShowNotification;
            this.nbt = nbt;
        }

        public Result(ResourceLocation pId, Item pResult, int pCount, String pGroup, CraftingBookCategory pCategory, List<String> pPattern, Map<Character, Ingredient> pKey, Advancement.Builder pAdvancement, ResourceLocation pAdvancementId, boolean pShowNotification) {
            this(pId, pResult, pCount, pGroup, pCategory, pPattern, pKey, pAdvancement, pAdvancementId, pShowNotification, null);
        }

        public void serializeRecipeData(JsonObject pJson) {
            super.serializeRecipeData(pJson);
            if (!this.group.isEmpty()) {
                pJson.addProperty("group", this.group);
            }

            JsonArray jsonarray = new JsonArray();

            for (String s : this.pattern) {
                jsonarray.add(s);
            }

            pJson.add("pattern", jsonarray);
            JsonObject jsonobject = new JsonObject();

            for (Map.Entry<Character, Ingredient> entry : this.key.entrySet()) {
                jsonobject.add(String.valueOf(entry.getKey()), entry.getValue().toJson());
            }

            pJson.add("key", jsonobject);
            JsonObject result = new JsonObject();
            result.addProperty("item", BuiltInRegistries.ITEM.getKey(this.result).toString());
            if (this.count > 1) {
                result.addProperty("count", this.count);
            }
            if (this.nbt != null) {
                result.add("nbt", NBTTool.parseTag(this.nbt));
            }

            pJson.add("result", result);
            pJson.addProperty("show_notification", this.showNotification);
        }

        public RecipeSerializer<?> getType() {
            return RecipeSerializer.SHAPED_RECIPE;
        }

        public ResourceLocation getId() {
            return this.id;
        }

        @Nullable
        public JsonObject serializeAdvancement() {
            return this.advancement.serializeToJson();
        }

        @Nullable
        public ResourceLocation getAdvancementId() {
            return this.advancementId;
        }
    }
}
