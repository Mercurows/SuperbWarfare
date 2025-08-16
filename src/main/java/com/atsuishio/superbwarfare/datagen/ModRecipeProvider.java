package com.atsuishio.superbwarfare.datagen;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.datagen.builder.VehicleAssemblingRecipeBuilder;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.recipe.AmmoBoxAddAmmoRecipe;
import com.atsuishio.superbwarfare.recipe.AmmoBoxExtractAmmoRecipe;
import com.atsuishio.superbwarfare.recipe.PotionMortarShellRecipe;
import com.atsuishio.superbwarfare.recipe.SmokeDyeRecipe;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static com.atsuishio.superbwarfare.datagen.ModItemTagProvider.cTag;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput writer) {
        // special
        SpecialRecipeBuilder.special(PotionMortarShellRecipe::new).save(writer, "potion_mortar_shell");
        SpecialRecipeBuilder.special(AmmoBoxAddAmmoRecipe::new).save(writer, "ammo_box_add_ammo");
        SpecialRecipeBuilder.special(AmmoBoxExtractAmmoRecipe::new).save(writer, "ammo_box_extract_ammo");
        SpecialRecipeBuilder.special(SmokeDyeRecipe::new).save(writer, "smoke_dye");

        // items
        // 材料
        generateMaterialRecipes(writer, ModItems.IRON_MATERIALS, Items.IRON_INGOT);
        generateMaterialRecipes(writer, ModItems.STEEL_MATERIALS, ModItems.STEEL_INGOT.get());
        generateMaterialRecipes(writer, ModItems.CEMENTED_CARBIDE_MATERIALS, ModItems.CEMENTED_CARBIDE_INGOT.get());
        generateSmithingMaterialRecipe(writer, ModItems.CEMENTED_CARBIDE_MATERIALS, ModItems.NETHERITE_MATERIALS, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Items.NETHERITE_INGOT);

        // 材料包
        generateMaterialPackRecipe(writer, ModItems.IRON_MATERIALS, ModItems.COMMON_MATERIAL_PACK.get());
        generateMaterialPackRecipe(writer, ModItems.STEEL_MATERIALS, ModItems.RARE_MATERIAL_PACK.get());
        generateMaterialPackRecipe(writer, ModItems.CEMENTED_CARBIDE_MATERIALS, ModItems.EPIC_MATERIAL_PACK.get());
        generateMaterialPackRecipe(writer, ModItems.NETHERITE_MATERIALS, ModItems.LEGENDARY_MATERIAL_PACK.get());

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ARTILLERY_INDICATOR.get())
                .pattern(" b ")
                .pattern("aca")
                .define('a', Items.SPYGLASS)
                .define('b', ModItems.MONITOR.get())
                .define('c', ModItems.FIRING_PARAMETERS.get())
                .unlockedBy(getHasName(Items.SPYGLASS), has(Items.SPYGLASS))
                .save(writer, Mod.loc(getItemName(ModItems.ARTILLERY_INDICATOR.get())));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ARTILLERY_INDICATOR.get())
                .requires(ModItems.ARTILLERY_INDICATOR.get())
                .unlockedBy(getHasName(ModItems.ARTILLERY_INDICATOR.get()), has(ModItems.ARTILLERY_INDICATOR.get()))
                .save(writer, Mod.loc(getItemName(ModItems.ARTILLERY_INDICATOR.get()) + "_clear"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.STEEL_PIPE.get())
                .pattern(" a")
                .pattern("a ")
                .define('a', ModItems.STEEL_MATERIALS.barrel().get())
                .unlockedBy(getHasName(ModItems.STEEL_MATERIALS.barrel().get()), has(ModItems.STEEL_MATERIALS.barrel().get()))
                .save(writer, Mod.loc(getItemName(ModItems.STEEL_PIPE.get())));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MEDICAL_KIT.get(), 2)
                .pattern("aba")
                .pattern("bcb")
                .pattern("aba")
                .define('a', Items.STRING)
                .define('b', ItemTags.WOOL_CARPETS)
                .define('c', getPotionIngredient(Potions.REGENERATION))
                .unlockedBy(getHasName(Items.STRING), has(Items.STRING))
                .save(writer, Mod.loc(getItemName(ModItems.MEDICAL_KIT.get())));

        // 弹药
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.SMALL_ROCKET.get(), 4)
                .pattern(" a ")
                .pattern("bcb")
                .pattern(" d ")
                .define('a', ModItems.FUSEE.get())
                .define('b', Items.COPPER_INGOT)
                .define('c', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('d', ModItems.GRAIN.get())
                .unlockedBy(getHasName(ModItems.FUSEE.get()), has(ModItems.FUSEE.get()))
                .save(writer, Mod.loc(getItemName(ModItems.SMALL_ROCKET.get())));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.RPG_ROCKET_TBG.get(), 2)
                .pattern(" a ")
                .pattern("bcb")
                .pattern(" d ")
                .define('a', ModItems.FUSEE.get())
                .define('b', Items.IRON_INGOT)
                .define('c', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('d', ModItems.GRAIN.get())
                .unlockedBy(getHasName(ModItems.FUSEE.get()), has(ModItems.FUSEE.get()))
                .save(writer, Mod.loc(getItemName(ModItems.RPG_ROCKET_TBG.get())));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.RPG_ROCKET_STANDARD.get(), 2)
                .pattern(" a ")
                .pattern("bcb")
                .pattern("ede")
                .define('a', ModItems.FUSEE.get())
                .define('b', Items.IRON_INGOT)
                .define('c', cTag("plates/copper"))
                .define('d', ModItems.GRAIN.get())
                .define('e', Items.GUNPOWDER)
                .unlockedBy(getHasName(ModItems.FUSEE.get()), has(ModItems.FUSEE.get()))
                .save(writer, Mod.loc(getItemName(ModItems.RPG_ROCKET_STANDARD.get())));

        // 方块
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.SUPERB_ITEM_INTERFACE.get())
                .pattern("cac")
                .pattern("aba")
                .pattern("cac")
                .define('a', Items.HOPPER)
                .define('b', Items.DROPPER)
                .define('c', ModTags.Items.INGOTS_STEEL)
                .unlockedBy(getHasName(Items.HOPPER), has(Items.DROPPER))
                .save(writer, Mod.loc(getItemName(ModItems.SUPERB_ITEM_INTERFACE.get())));

        // vehicles
        VehicleAssemblingRecipeBuilder.entity(ModEntities.TOM_6.get(), VehicleAssemblingRecipe.Category.AIRCRAFT)
                .require(ItemTags.PLANKS, 5)
                .require(ModItems.BATTERY.get())
                .require(Items.MINECART)
                .unlockedBy(getHasName(Items.MINECART), has(Items.MINECART))
                .save(writer, Mod.loc(getEntityTypeName(ModEntities.TOM_6.get())));

        VehicleAssemblingRecipeBuilder.entity(ModEntities.ANNIHILATOR.get(), VehicleAssemblingRecipe.Category.DEFENSE)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 24)
                .require(Items.NETHERITE_BLOCK, 3)
                .require(Items.BEACON, 3)
                .require(ModItems.LARGE_BATTERY_PACK.get())
                .require(ModItems.ANNIHILATOR_BLUEPRINT.get())
                .unlockedBy(getHasName(ModItems.ANNIHILATOR_BLUEPRINT.get()), has(ModItems.ANNIHILATOR_BLUEPRINT.get()))
                .save(writer, Mod.loc(getEntityTypeName(ModEntities.ANNIHILATOR.get())));

        VehicleAssemblingRecipeBuilder.entity(ModEntities.BL_132.get(), VehicleAssemblingRecipe.Category.DEFENSE)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 10)
                .require(ModItems.BL_132_BLUEPRINT.get())
                .require(ModItems.CANNON_CORE.get(), 4)
                .unlockedBy(getHasName(ModItems.BL_132_BLUEPRINT.get()), has(ModItems.BL_132_BLUEPRINT.get()))
                .save(writer, Mod.loc(getEntityTypeName(ModEntities.BL_132.get())));

        VehicleAssemblingRecipeBuilder.entity(ModEntities.MLE_1934.get(), VehicleAssemblingRecipe.Category.DEFENSE)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 8)
                .require(ModItems.MLE_1934_BLUEPRINT.get())
                .require(ModItems.CANNON_CORE.get(), 2)
                .unlockedBy(getHasName(ModItems.MLE_1934_BLUEPRINT.get()), has(ModItems.MLE_1934_BLUEPRINT.get()))
                .save(writer, Mod.loc(getEntityTypeName(ModEntities.MLE_1934.get())));

        VehicleAssemblingRecipeBuilder.entity(ModEntities.MK_42.get(), VehicleAssemblingRecipe.Category.DEFENSE)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 6)
                .require(ModItems.MK_42_BLUEPRINT.get())
                .require(ModItems.CANNON_CORE.get())
                .unlockedBy(getHasName(ModItems.MK_42_BLUEPRINT.get()), has(ModItems.MK_42_BLUEPRINT.get()))
                .save(writer, Mod.loc(getEntityTypeName(ModEntities.MK_42.get())));

        VehicleAssemblingRecipeBuilder.entity(ModEntities.TYPE_63.get(), VehicleAssemblingRecipe.Category.DEFENSE)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 1)
                .require(ModItems.MORTAR_BARREL.get(), 12)
                .require(ModItems.WHEEL.get(), 2)
                .unlockedBy(getHasName(ModItems.MORTAR_BARREL.get()), has(ModItems.MORTAR_BARREL.get()))
                .save(writer, Mod.loc(getEntityTypeName(ModEntities.TYPE_63.get())));

        VehicleAssemblingRecipeBuilder.entity(ModEntities.HPJ_11.get(), VehicleAssemblingRecipe.Category.DEFENSE)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 5)
                .require(ModItems.HPJ_11_BLUEPRINT.get())
                .require(ModItems.CANNON_CORE.get())
                .require(ModItems.MEDIUM_BATTERY_PACK.get())
                .require(ModItems.LARGE_MOTOR.get())
                .require(Items.OBSERVER)
                .unlockedBy(getHasName(ModItems.HPJ_11_BLUEPRINT.get()), has(ModItems.HPJ_11_BLUEPRINT.get()))
                .save(writer, Mod.loc(getEntityTypeName(ModEntities.HPJ_11.get())));

        VehicleAssemblingRecipeBuilder.entity(ModEntities.LASER_TOWER.get(), VehicleAssemblingRecipe.Category.DEFENSE)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 1)
                .require(Items.BEACON)
                .require(ModItems.SMALL_BATTERY_PACK.get())
                .require(ModItems.MOTOR.get())
                .unlockedBy(getHasName(Items.BEACON), has(Items.BEACON))
                .save(writer, Mod.loc(getEntityTypeName(ModEntities.LASER_TOWER.get())));

        VehicleAssemblingRecipeBuilder.entity(ModEntities.WHEEL_CHAIR.get(), VehicleAssemblingRecipe.Category.CIVILIAN)
                .require(ModItems.WHEEL.get(), 2)
                .require(ModItems.CELL.get())
                .require(ModItems.MOTOR.get())
                .require(Items.MINECART)
                .unlockedBy(getHasName(Items.MINECART), has(Items.MINECART))
                .save(writer, Mod.loc(getEntityTypeName(ModEntities.WHEEL_CHAIR.get())));

        VehicleAssemblingRecipeBuilder.entity(ModEntities.LAV_150.get(), VehicleAssemblingRecipe.Category.LAND)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 6)
                .require(ModItems.LIGHT_ARMAMENT_MODULE.get())
                .require(ModItems.MEDIUM_BATTERY_PACK.get())
                .require(ModItems.WHEEL.get(), 4)
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, Mod.loc(getEntityTypeName(ModEntities.LAV_150.get())));

        VehicleAssemblingRecipeBuilder.entity(ModEntities.BMP_2.get(), VehicleAssemblingRecipe.Category.LAND)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 8)
                .require(ModItems.MEDIUM_ARMAMENT_MODULE.get())
                .require(ModItems.MEDIUM_BATTERY_PACK.get())
                .require(ModItems.TRACK.get(), 2)
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, Mod.loc(getEntityTypeName(ModEntities.BMP_2.get())));

        VehicleAssemblingRecipeBuilder.entity(ModEntities.PRISM_TANK.get(), VehicleAssemblingRecipe.Category.LAND)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 9)
                .require(Items.BEACON)
                .require(ModItems.LARGE_BATTERY_PACK.get())
                .require(ModItems.TRACK.get(), 2)
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, Mod.loc(getEntityTypeName(ModEntities.PRISM_TANK.get())));

        VehicleAssemblingRecipeBuilder.entity(ModEntities.YX_100.get(), VehicleAssemblingRecipe.Category.LAND)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 12)
                .require(ModItems.CEMENTED_CARBIDE_BLOCK.get(), 2)
                .require(ModItems.HEAVY_ARMAMENT_MODULE.get())
                .require(ModItems.LARGE_BATTERY_PACK.get())
                .require(ModItems.TRACK.get(), 2)
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, Mod.loc(getEntityTypeName(ModEntities.YX_100.get())));

        VehicleAssemblingRecipeBuilder.entity(ModEntities.SPEEDBOAT.get(), VehicleAssemblingRecipe.Category.WATER)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 2)
                .require(ItemTags.BOATS)
                .require(ModItems.M_2_HB.get())
                .require(ModItems.SMALL_BATTERY_PACK.get())
                .require(ModItems.LARGE_PROPELLER.get())
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.M_2_HB.get()), has(ModItems.M_2_HB.get()))
                .save(writer, Mod.loc(getEntityTypeName(ModEntities.SPEEDBOAT.get())));

        VehicleAssemblingRecipeBuilder.entity(ModEntities.AH_6.get(), VehicleAssemblingRecipe.Category.AIRCRAFT)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 3)
                .require(ModItems.LIGHT_ARMAMENT_MODULE.get())
                .require(ModItems.MEDIUM_BATTERY_PACK.get())
                .require(ModItems.LARGE_PROPELLER.get(), 2)
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LARGE_PROPELLER.get()), has(ModItems.LARGE_PROPELLER.get()))
                .save(writer, Mod.loc(getEntityTypeName(ModEntities.AH_6.get())));

        VehicleAssemblingRecipeBuilder.entity(ModEntities.A_10A.get(), VehicleAssemblingRecipe.Category.AIRCRAFT)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 10)
                .require(ModItems.HEAVY_ARMAMENT_MODULE.get())
                .require(ModItems.LARGE_BATTERY_PACK.get())
                .require(ModItems.LARGE_PROPELLER.get(), 2)
                .require(ModItems.LARGE_MOTOR.get(), 2)
                .require(ModItems.WHEEL.get(), 3)
                .unlockedBy(getHasName(ModItems.LARGE_PROPELLER.get()), has(ModItems.LARGE_PROPELLER.get()))
                .save(writer, Mod.loc(getEntityTypeName(ModEntities.A_10A.get())));

        // guns
        gunSmithing(writer, ModItems.TRACHELIUM_BLUEPRINT.get(), GunRarity.EPIC, ModTags.Items.INGOTS_CEMENTED_CARBIDE, ModItems.TRACHELIUM.get());
        gunSmithing(writer, ModItems.GLOCK_17_BLUEPRINT.get(), GunRarity.COMMON, Items.IRON_INGOT, ModItems.GLOCK_17.get());
        gunSmithing(writer, ModItems.MP_443_BLUEPRINT.get(), GunRarity.COMMON, Items.IRON_INGOT, ModItems.MP_443.get());
        gunSmithing(writer, ModItems.GLOCK_18_BLUEPRINT.get(), GunRarity.RARE, Items.GOLD_INGOT, ModItems.GLOCK_18.get());
        gunSmithing(writer, ModItems.HUNTING_RIFLE_BLUEPRINT.get(), GunRarity.RARE, ItemTags.LOGS, ModItems.HUNTING_RIFLE.get());
        gunSmithing(writer, ModItems.M_79_BLUEPRINT.get(), GunRarity.RARE, Items.DISPENSER, ModItems.M_79.get());
        gunSmithing(writer, ModItems.RPG_BLUEPRINT.get(), GunRarity.RARE, Items.DISPENSER, ModItems.RPG.get());
        gunSmithing(writer, ModItems.BOCEK_BLUEPRINT.get(), GunRarity.EPIC, Items.BOW, ModItems.BOCEK.get());
        gunSmithing(writer, ModItems.M_4_BLUEPRINT.get(), GunRarity.RARE, ModTags.Items.INGOTS_STEEL, ModItems.M_4.get());
        gunSmithing(writer, ModItems.AA_12_BLUEPRINT.get(), GunRarity.LEGENDARY, Items.NETHERITE_INGOT, ModItems.AA_12.get());
        gunSmithing(writer, ModItems.HK_416_BLUEPRINT.get(), GunRarity.RARE, ModTags.Items.INGOTS_STEEL, ModItems.HK_416.get());
        gunSmithing(writer, ModItems.RPK_BLUEPRINT.get(), GunRarity.EPIC, ItemTags.LOGS, ModItems.RPK.get());
        gunSmithing(writer, ModItems.SKS_BLUEPRINT.get(), GunRarity.RARE, ItemTags.LOGS, ModItems.SKS.get());
        gunSmithing(writer, ModItems.NTW_20_BLUEPRINT.get(), GunRarity.LEGENDARY, Items.SPYGLASS, ModItems.NTW_20.get());
        gunSmithing(writer, ModItems.MP_5_BLUEPRINT.get(), GunRarity.RARE, Items.IRON_INGOT, ModItems.MP_5.get());
        gunSmithing(writer, ModItems.VECTOR_BLUEPRINT.get(), GunRarity.EPIC, ModTags.Items.INGOTS_CEMENTED_CARBIDE, ModItems.VECTOR.get());
        gunSmithing(writer, ModItems.MINIGUN_BLUEPRINT.get(), GunRarity.LEGENDARY, ModItems.MOTOR.get(), ModItems.MINIGUN.get());
        gunSmithing(writer, ModItems.MK_14_BLUEPRINT.get(), GunRarity.EPIC, ModTags.Items.INGOTS_CEMENTED_CARBIDE, ModItems.MK_14.get());
        gunSmithing(writer, ModItems.SENTINEL_BLUEPRINT.get(), GunRarity.EPIC, ModItems.CELL.get(), ModItems.SENTINEL.get());
        gunSmithing(writer, ModItems.M_60_BLUEPRINT.get(), GunRarity.EPIC, ModTags.Items.INGOTS_CEMENTED_CARBIDE, ModItems.M_60.get());
        gunSmithing(writer, ModItems.SVD_BLUEPRINT.get(), GunRarity.EPIC, ModTags.Items.INGOTS_CEMENTED_CARBIDE, ModItems.SVD.get());
        gunSmithing(writer, ModItems.MARLIN_BLUEPRINT.get(), GunRarity.COMMON, ItemTags.LOGS, ModItems.MARLIN.get());
        gunSmithing(writer, ModItems.M_870_BLUEPRINT.get(), GunRarity.RARE, ModTags.Items.INGOTS_STEEL, ModItems.M_870.get());
        gunSmithing(writer, ModItems.M_98B_BLUEPRINT.get(), GunRarity.EPIC, Items.SPYGLASS, ModItems.M_98B.get());
        gunSmithing(writer, ModItems.AK_47_BLUEPRINT.get(), GunRarity.RARE, ItemTags.LOGS, ModItems.AK_47.get());
        gunSmithing(writer, ModItems.AK_12_BLUEPRINT.get(), GunRarity.RARE, ModTags.Items.INGOTS_STEEL, ModItems.AK_12.get());
        gunSmithing(writer, ModItems.DEVOTION_BLUEPRINT.get(), GunRarity.EPIC, ModTags.Items.INGOTS_CEMENTED_CARBIDE, ModItems.DEVOTION.get());
        gunSmithing(writer, ModItems.TASER_BLUEPRINT.get(), GunRarity.COMMON, Items.YELLOW_CONCRETE, ModItems.TASER.get());
        gunSmithing(writer, ModItems.M_1911_BLUEPRINT.get(), GunRarity.COMMON, ModTags.Items.INGOTS_STEEL, ModItems.M_1911.get());
        gunSmithing(writer, ModItems.QBZ_95_BLUEPRINT.get(), GunRarity.RARE, ModTags.Items.INGOTS_STEEL, ModItems.QBZ_95.get());
        gunSmithing(writer, ModItems.QBZ_191_BLUEPRINT.get(), GunRarity.EPIC, ModTags.Items.INGOTS_CEMENTED_CARBIDE, ModItems.QBZ_191.get());
        gunSmithing(writer, ModItems.AWM_BLUEPRINT.get(), GunRarity.EPIC, Items.SPYGLASS, ModItems.AWM.get());
        gunSmithing(writer, ModItems.K_98_BLUEPRINT.get(), GunRarity.RARE, ItemTags.LOGS, ModItems.K_98.get());
        gunSmithing(writer, ModItems.MOSIN_NAGANT_BLUEPRINT.get(), GunRarity.RARE, ItemTags.LOGS, ModItems.MOSIN_NAGANT.get());
        gunSmithing(writer, ModItems.JAVELIN_BLUEPRINT.get(), GunRarity.LEGENDARY, ModItems.ANCIENT_CPU.get(), ModItems.JAVELIN.get());
        gunSmithing(writer, ModItems.M_2_HB_BLUEPRINT.get(), GunRarity.RARE, ModTags.Items.STORAGE_BLOCK_STEEL, ModItems.M_2_HB.get());
        gunSmithing(writer, ModItems.SECONDARY_CATACLYSM_BLUEPRINT.get(), GunRarity.LEGENDARY, ModItems.KNIFE.get(), ModItems.SECONDARY_CATACLYSM.get());
        gunSmithing(writer, ModItems.INSIDIOUS_BLUEPRINT.get(), GunRarity.EPIC, ModTags.Items.INGOTS_CEMENTED_CARBIDE, ModItems.INSIDIOUS.get());
        gunSmithing(writer, ModItems.AURELIA_SCEPTRE_BLUEPRINT.get(), GunRarity.LEGENDARY, Items.END_CRYSTAL, ModItems.AURELIA_SCEPTRE.get());

        // blueprints
        copyBlueprint(writer, ModItems.TRACHELIUM_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.GLOCK_17_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.MP_443_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.GLOCK_18_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.HUNTING_RIFLE_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.M_79_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.RPG_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.BOCEK_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.M_4_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.AA_12_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.HK_416_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.RPK_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.SKS_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.NTW_20_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.MP_5_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.VECTOR_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.MINIGUN_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.MK_14_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.SENTINEL_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.M_60_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.SVD_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.MARLIN_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.M_870_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.AWM_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.M_98B_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.AK_47_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.AK_12_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.DEVOTION_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.TASER_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.M_1911_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.QBZ_95_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.QBZ_191_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.K_98_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.MOSIN_NAGANT_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.JAVELIN_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.M_2_HB_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.SECONDARY_CATACLYSM_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.INSIDIOUS_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.AURELIA_SCEPTRE_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.MK_42_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.MLE_1934_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.BL_132_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.HPJ_11_BLUEPRINT.get());
        copyBlueprint(writer, ModItems.ANNIHILATOR_BLUEPRINT.get());
    }

    public static void copyBlueprint(RecipeOutput writer, ItemLike result) {
        copySmithingTemplate(writer, result, Items.LAPIS_LAZULI);
    }

    public static void gunSmithing(RecipeOutput writer, ItemLike blueprint, GunRarity rarity, TagKey<Item> tagKey, Item pResultItem) {
        gunSmithing(writer, blueprint, rarity, Ingredient.of(tagKey), pResultItem);
    }

    public static void gunSmithing(RecipeOutput writer, ItemLike blueprint, GunRarity rarity, ItemLike ingredient, Item pResultItem) {
        gunSmithing(writer, blueprint, rarity, Ingredient.of(ingredient), pResultItem);
    }

    public static void gunSmithing(RecipeOutput writer, ItemLike blueprint, GunRarity rarity, Ingredient ingredient, Item pResultItem) {
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
                .save(writer, Mod.loc(getItemName(pResultItem) + "_smithing"));
    }

    public enum GunRarity {
        COMMON,
        RARE,
        EPIC,
        LEGENDARY,
    }

    protected static String getEntityTypeName(EntityType<?> entityType) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entityType).getPath();
    }

    // 生成材料包所有材料的配方
    public static void generateMaterialRecipes(@NotNull RecipeOutput writer, ModItems.Materials material, Item ingredient) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, material.barrel().get())
                .pattern("AAA")
                .define('A', ingredient)
                .unlockedBy(getHasName(ingredient), has(ingredient))
                .save(writer, Mod.loc(getItemName(material.barrel().get())));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, material.action().get())
                .pattern("AAA")
                .pattern("  A")
                .define('A', ingredient)
                .unlockedBy(getHasName(ingredient), has(ingredient))
                .save(writer, Mod.loc(getItemName(material.action().get())));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, material.spring().get())
                .pattern("A")
                .pattern("A")
                .pattern("A")
                .define('A', ingredient)
                .unlockedBy(getHasName(ingredient), has(ingredient))
                .save(writer, Mod.loc(getItemName(material.spring().get())));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, material.trigger().get())
                .pattern("BA")
                .pattern(" A")
                .define('A', ingredient)
                .define('B', Items.TRIPWIRE_HOOK)
                .unlockedBy(getHasName(ingredient), has(ingredient))
                .save(writer, Mod.loc(getItemName(material.trigger().get())));
    }

    public static void generateSmithingMaterialRecipe(@NotNull RecipeOutput writer, ModItems.Materials material, ModItems.Materials result, Item template, Item ingredient) {
        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(template),
                        Ingredient.of(material.barrel().get()),
                        Ingredient.of(ingredient),
                        RecipeCategory.MISC,
                        result.barrel().get()
                )
                .unlocks(getHasName(template), has(template))
                .unlocks(getHasName(material.barrel().get()), has(material.barrel().get()))
                .save(writer, Mod.loc(getItemName(result.barrel().get())));

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(template),
                        Ingredient.of(material.action().get()),
                        Ingredient.of(ingredient),
                        RecipeCategory.MISC,
                        result.action().get()
                )
                .unlocks(getHasName(template), has(template))
                .unlocks(getHasName(material.action().get()), has(material.action().get()))
                .save(writer, Mod.loc(getItemName(result.action().get())));

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(template),
                        Ingredient.of(material.spring().get()),
                        Ingredient.of(ingredient),
                        RecipeCategory.MISC,
                        result.spring().get()
                )
                .unlocks(getHasName(template), has(template))
                .unlocks(getHasName(material.spring().get()), has(material.spring().get()))
                .save(writer, Mod.loc(getItemName(result.spring().get())));

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(template),
                        Ingredient.of(material.trigger().get()),
                        Ingredient.of(ingredient),
                        RecipeCategory.MISC,
                        result.trigger().get()
                )
                .unlocks(getHasName(template), has(template))
                .unlocks(getHasName(material.trigger().get()), has(material.trigger().get()))
                .save(writer, Mod.loc(getItemName(result.trigger().get())));
    }

    public static void generateMaterialPackRecipe(@NotNull RecipeOutput writer, ModItems.Materials material, Item pack) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, pack)
                .requires(material.barrel().get())
                .requires(material.action().get())
                .requires(material.spring().get())
                .requires(material.trigger().get())
                .unlockedBy(getHasName(material.barrel().get()), has(material.barrel().get()))
                .unlockedBy(getHasName(material.action().get()), has(material.action().get()))
                .unlockedBy(getHasName(material.spring().get()), has(material.spring().get()))
                .unlockedBy(getHasName(material.trigger().get()), has(material.trigger().get()))
                .save(writer, Mod.loc(getItemName(pack)));
    }

    public static Ingredient getPotionIngredient(Holder<Potion> potion) {
        return DataComponentIngredient.of(false, DataComponentMap.builder().set(DataComponents.POTION_CONTENTS, new PotionContents(potion)).build(), Items.POTION);
    }
}
