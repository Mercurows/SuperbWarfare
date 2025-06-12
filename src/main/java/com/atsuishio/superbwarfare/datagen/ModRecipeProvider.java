package com.atsuishio.superbwarfare.datagen;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.recipe.AmmoBoxAddAmmoRecipe;
import com.atsuishio.superbwarfare.recipe.AmmoBoxExtractAmmoRecipe;
import com.atsuishio.superbwarfare.recipe.PotionMortarShellRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput output) {
        SpecialRecipeBuilder.special(PotionMortarShellRecipe::new).save(output, "potion_mortar_shell");
        SpecialRecipeBuilder.special(AmmoBoxAddAmmoRecipe::new).save(output, "ammo_box_add_ammo");
        SpecialRecipeBuilder.special(AmmoBoxExtractAmmoRecipe::new).save(output, "ammo_box_extract_ammo");

        gunSmithing(output, ModItems.TRACHELIUM_BLUEPRINT.get(), GunRarity.EPIC, ModTags.Items.INGOTS_CEMENTED_CARBIDE, ModItems.TRACHELIUM.get());
        gunSmithing(output, ModItems.GLOCK_17_BLUEPRINT.get(), GunRarity.COMMON, Items.IRON_INGOT, ModItems.GLOCK_17.get());
        gunSmithing(output, ModItems.MP_443_BLUEPRINT.get(), GunRarity.COMMON, Items.IRON_INGOT, ModItems.MP_443.get());
        gunSmithing(output, ModItems.GLOCK_18_BLUEPRINT.get(), GunRarity.RARE, Items.GOLD_INGOT, ModItems.GLOCK_18.get());
        gunSmithing(output, ModItems.HUNTING_RIFLE_BLUEPRINT.get(), GunRarity.RARE, ItemTags.LOGS, ModItems.HUNTING_RIFLE.get());
        gunSmithing(output, ModItems.M_79_BLUEPRINT.get(), GunRarity.RARE, Items.DISPENSER, ModItems.M_79.get());
        gunSmithing(output, ModItems.RPG_BLUEPRINT.get(), GunRarity.RARE, Items.DISPENSER, ModItems.RPG.get());
        gunSmithing(output, ModItems.BOCEK_BLUEPRINT.get(), GunRarity.EPIC, Items.BOW, ModItems.BOCEK.get());
        gunSmithing(output, ModItems.M_4_BLUEPRINT.get(), GunRarity.RARE, ModTags.Items.INGOTS_STEEL, ModItems.M_4.get());
        gunSmithing(output, ModItems.AA_12_BLUEPRINT.get(), GunRarity.LEGENDARY, Items.NETHERITE_INGOT, ModItems.AA_12.get());
        gunSmithing(output, ModItems.HK_416_BLUEPRINT.get(), GunRarity.RARE, ModTags.Items.INGOTS_STEEL, ModItems.HK_416.get());
        gunSmithing(output, ModItems.RPK_BLUEPRINT.get(), GunRarity.EPIC, ItemTags.LOGS, ModItems.RPK.get());
        gunSmithing(output, ModItems.SKS_BLUEPRINT.get(), GunRarity.RARE, ItemTags.LOGS, ModItems.SKS.get());
        gunSmithing(output, ModItems.NTW_20_BLUEPRINT.get(), GunRarity.LEGENDARY, Items.SPYGLASS, ModItems.NTW_20.get());
        gunSmithing(output, ModItems.MP_5_BLUEPRINT.get(), GunRarity.RARE, Items.IRON_INGOT, ModItems.MP_5.get());
        gunSmithing(output, ModItems.VECTOR_BLUEPRINT.get(), GunRarity.EPIC, ModTags.Items.INGOTS_CEMENTED_CARBIDE, ModItems.VECTOR.get());
        gunSmithing(output, ModItems.MINIGUN_BLUEPRINT.get(), GunRarity.LEGENDARY, ModItems.MOTOR.get(), ModItems.MINIGUN.get());
        gunSmithing(output, ModItems.MK_14_BLUEPRINT.get(), GunRarity.EPIC, ModTags.Items.INGOTS_CEMENTED_CARBIDE, ModItems.MK_14.get());
        gunSmithing(output, ModItems.SENTINEL_BLUEPRINT.get(), GunRarity.EPIC, ModItems.CELL.get(), ModItems.SENTINEL.get());
        gunSmithing(output, ModItems.M_60_BLUEPRINT.get(), GunRarity.EPIC, ModTags.Items.INGOTS_CEMENTED_CARBIDE, ModItems.M_60.get());
        gunSmithing(output, ModItems.SVD_BLUEPRINT.get(), GunRarity.EPIC, ModTags.Items.INGOTS_CEMENTED_CARBIDE, ModItems.SVD.get());
        gunSmithing(output, ModItems.MARLIN_BLUEPRINT.get(), GunRarity.COMMON, ItemTags.LOGS, ModItems.MARLIN.get());
        gunSmithing(output, ModItems.M_870_BLUEPRINT.get(), GunRarity.RARE, ModTags.Items.INGOTS_STEEL, ModItems.M_870.get());
        gunSmithing(output, ModItems.M_98B_BLUEPRINT.get(), GunRarity.EPIC, Items.SPYGLASS, ModItems.M_98B.get());


        copyBlueprint(output, ModItems.TRACHELIUM_BLUEPRINT.get());
        copyBlueprint(output, ModItems.GLOCK_17_BLUEPRINT.get());
        copyBlueprint(output, ModItems.MP_443_BLUEPRINT.get());
        copyBlueprint(output, ModItems.GLOCK_18_BLUEPRINT.get());
        copyBlueprint(output, ModItems.HUNTING_RIFLE_BLUEPRINT.get());
        copyBlueprint(output, ModItems.M_79_BLUEPRINT.get());
        copyBlueprint(output, ModItems.RPG_BLUEPRINT.get());
        copyBlueprint(output, ModItems.BOCEK_BLUEPRINT.get());
        copyBlueprint(output, ModItems.M_4_BLUEPRINT.get());
        copyBlueprint(output, ModItems.AA_12_BLUEPRINT.get());
        copyBlueprint(output, ModItems.HK_416_BLUEPRINT.get());
        copyBlueprint(output, ModItems.RPK_BLUEPRINT.get());
        copyBlueprint(output, ModItems.SKS_BLUEPRINT.get());
        copyBlueprint(output, ModItems.NTW_20_BLUEPRINT.get());
        copyBlueprint(output, ModItems.MP_5_BLUEPRINT.get());
        copyBlueprint(output, ModItems.VECTOR_BLUEPRINT.get());
        copyBlueprint(output, ModItems.MINIGUN_BLUEPRINT.get());
        copyBlueprint(output, ModItems.MK_14_BLUEPRINT.get());
        copyBlueprint(output, ModItems.SENTINEL_BLUEPRINT.get());
        copyBlueprint(output, ModItems.M_60_BLUEPRINT.get());
        copyBlueprint(output, ModItems.SVD_BLUEPRINT.get());
        copyBlueprint(output, ModItems.MARLIN_BLUEPRINT.get());
        copyBlueprint(output, ModItems.M_870_BLUEPRINT.get());
        copyBlueprint(output, ModItems.M_98B_BLUEPRINT.get());
        copyBlueprint(output, ModItems.AK_47_BLUEPRINT.get());
        copyBlueprint(output, ModItems.AK_12_BLUEPRINT.get());
        copyBlueprint(output, ModItems.DEVOTION_BLUEPRINT.get());
        copyBlueprint(output, ModItems.TASER_BLUEPRINT.get());
        copyBlueprint(output, ModItems.M_1911_BLUEPRINT.get());
        copyBlueprint(output, ModItems.QBZ_95_BLUEPRINT.get());
        copyBlueprint(output, ModItems.K_98_BLUEPRINT.get());
        copyBlueprint(output, ModItems.MOSIN_NAGANT_BLUEPRINT.get());
        copyBlueprint(output, ModItems.JAVELIN_BLUEPRINT.get());
        copyBlueprint(output, ModItems.M_2_HB_BLUEPRINT.get());
        copyBlueprint(output, ModItems.SECONDARY_CATACLYSM_BLUEPRINT.get());
        copyBlueprint(output, ModItems.INSIDIOUS_BLUEPRINT.get());
        copyBlueprint(output, ModItems.AURELIA_SCEPTRE_BLUEPRINT.get());
        copyBlueprint(output, ModItems.MK_42_BLUEPRINT.get());
        copyBlueprint(output, ModItems.MLE_1934_BLUEPRINT.get());
        copyBlueprint(output, ModItems.HPJ_11_BLUEPRINT.get());
        copyBlueprint(output, ModItems.ANNIHILATOR_BLUEPRINT.get());
    }

    public static void copyBlueprint(RecipeOutput output, ItemLike result) {
        copySmithingTemplate(output, result, Items.LAPIS_LAZULI);
    }

    public static void gunSmithing(RecipeOutput output, ItemLike blueprint, GunRarity rarity, TagKey<Item> tagKey, Item pResultItem) {
        gunSmithing(output, blueprint, rarity, Ingredient.of(tagKey), pResultItem);
    }

    public static void gunSmithing(RecipeOutput output, ItemLike blueprint, GunRarity rarity, ItemLike ingredient, Item pResultItem) {
        gunSmithing(output, blueprint, rarity, Ingredient.of(ingredient), pResultItem);
    }

    public static void gunSmithing(RecipeOutput output, ItemLike blueprint, GunRarity rarity, Ingredient ingredient, Item pResultItem) {
        ItemLike pack = switch (rarity) {
            case COMMON -> ModItems.COMMON_MATERIAL_PACK.get();
            case RARE -> ModItems.RARE_MATERIAL_PACK.get();
            case EPIC -> ModItems.EPIC_MATERIAL_PACK.get();
            case LEGENDARY -> ModItems.LEGENDARY_MATERIAL_PACK.get();
        };

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(blueprint),
                        Ingredient.of(pack),
                        ingredient,
                        RecipeCategory.COMBAT,
                        pResultItem
                )
                .unlocks(getHasName(blueprint), has(blueprint))
                .save(output, Mod.loc(getItemName(pResultItem) + "_smithing"));
    }

    public enum GunRarity {
        COMMON,
        RARE,
        EPIC,
        LEGENDARY,
    }
}
