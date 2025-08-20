package com.atsuishio.superbwarfare.datagen;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.datagen.builder.NBTShapedRecipeBuilder;
import com.atsuishio.superbwarfare.datagen.builder.VehicleAssemblingRecipeBuilder;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.atsuishio.superbwarfare.init.ModTags.commonItemTag;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {

    public ModRecipeProvider(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> writer) {
        buildToolRecipes(writer);
        buildAmmoRecipes(writer);
        buildMaterialRecipes(writer);
        buildBlockRecipes(writer);
        buildVehicleRecipes(writer);
        buildGunRecipes(writer);
        buildBlueprintRecipes(writer);
        buildPerkRecipes(writer);
        buildSpecialRecipes(writer);
    }

    private static void buildToolRecipes(Consumer<FinishedRecipe> writer) {
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
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ARMOR_PLATE.get(), 4)
                .pattern("aba")
                .pattern("ccc")
                .pattern("ada")
                .define('a', Items.STRING)
                .define('b', ItemTags.TERRACOTTA)
                .define('c', ModTags.Items.INGOTS_STEEL)
                .define('d', ItemTags.WOOL)
                .unlockedBy(getHasName(Items.STRING), has(Items.STRING))
                .save(writer, Mod.loc(getItemName(ModItems.ARMOR_PLATE.get())));
    }

    private static void buildAmmoRecipes(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AMMO_BOX.get())
                .pattern("aba")
                .pattern("aaa")
                .define('a', Tags.Items.INGOTS_IRON)
                .define('b', Tags.Items.DYES_GREEN)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, Mod.loc(getItemName(ModItems.AMMO_BOX.get())));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AGM.get())
                .pattern(" b ")
                .pattern("ada")
                .pattern("cec")
                .define('a', commonItemTag("plates/copper"))
                .define('b', ModItems.SEEKER.get())
                .define('c', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('d', Items.TNT)
                .define('e', ModItems.MISSILE_ENGINE.get())
                .unlockedBy(getHasName(ModItems.MISSILE_ENGINE.get()), has(ModItems.MISSILE_ENGINE.get()))
                .save(writer, Mod.loc(getItemName(ModItems.AGM.get())));
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
                .define('c', commonItemTag("plates/copper"))
                .define('d', ModItems.GRAIN.get())
                .define('e', Items.GUNPOWDER)
                .unlockedBy(getHasName(ModItems.FUSEE.get()), has(ModItems.FUSEE.get()))
                .save(writer, Mod.loc(getItemName(ModItems.RPG_ROCKET_STANDARD.get())));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.C4_BOMB.get(), 2)
                .pattern("aaa")
                .pattern("aba")
                .pattern("aaa")
                .define('a', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('b', Items.CLOCK)
                .unlockedBy(getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()), has(ModItems.HIGH_ENERGY_EXPLOSIVES.get()))
                .save(writer, Mod.loc(getItemName(ModItems.C4_BOMB.get())));
        NBTShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.C4_BOMB.get(), 2)
                .withNBT(tag -> tag.putBoolean("Control", true))
                .pattern("aaa")
                .pattern("aba")
                .pattern("aaa")
                .define('a', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('b', Items.COMPARATOR)
                .unlockedBy(getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()), has(ModItems.HIGH_ENERGY_EXPLOSIVES.get()))
                .save(writer, Mod.loc(getItemName(ModItems.C4_BOMB.get()) + "_rc"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AP_5_INCHES.get())
                .pattern("c")
                .pattern("a")
                .pattern("b")
                .define('a', ModItems.AP_HEAD.get())
                .define('b', ModItems.GRAIN.get())
                .define('c', ModItems.FUSEE.get())
                .unlockedBy(getHasName(ModItems.AP_HEAD.get()), has(ModItems.AP_HEAD.get()))
                .save(writer, Mod.loc(getItemName(ModItems.AP_5_INCHES.get())));
    }

    private static void buildMaterialRecipes(Consumer<FinishedRecipe> writer) {
        generateMaterialRecipes(writer, ModItems.IRON_MATERIALS, Items.IRON_INGOT);
        generateMaterialRecipes(writer, ModItems.STEEL_MATERIALS, ModItems.STEEL_INGOT.get());
        generateMaterialRecipes(writer, ModItems.CEMENTED_CARBIDE_MATERIALS, ModItems.CEMENTED_CARBIDE_INGOT.get());
        generateSmithingMaterialRecipe(writer, ModItems.CEMENTED_CARBIDE_MATERIALS, ModItems.NETHERITE_MATERIALS, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Items.NETHERITE_INGOT);

        generateMaterialPackRecipe(writer, ModItems.IRON_MATERIALS, ModItems.COMMON_MATERIAL_PACK.get());
        generateMaterialPackRecipe(writer, ModItems.STEEL_MATERIALS, ModItems.RARE_MATERIAL_PACK.get());
        generateMaterialPackRecipe(writer, ModItems.CEMENTED_CARBIDE_MATERIALS, ModItems.EPIC_MATERIAL_PACK.get());
        generateMaterialPackRecipe(writer, ModItems.NETHERITE_MATERIALS, ModItems.LEGENDARY_MATERIAL_PACK.get());

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ANCIENT_CPU.get(), 2)
                .pattern("bcb")
                .pattern("cac")
                .pattern("bcb")
                .define('a', ModItems.ANCIENT_CPU.get())
                .define('b', Tags.Items.GEMS_DIAMOND)
                .define('c', Tags.Items.ORES_NETHERITE_SCRAP)
                .unlockedBy(getHasName(ModItems.ANCIENT_CPU.get()), has(ModItems.ANCIENT_CPU.get()))
                .save(writer, Mod.loc(getItemName(ModItems.ANCIENT_CPU.get()) + "_copy"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.AP_HEAD.get(), 2)
                .pattern(" b ")
                .pattern("bdb")
                .pattern("cac")
                .define('a', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('b', Tags.Items.INGOTS_IRON)
                .define('c', commonItemTag("ingots/steel"))
                .define('d', ModItems.TUNGSTEN_ROD.get())
                .unlockedBy(getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()), has(ModItems.HIGH_ENERGY_EXPLOSIVES.get()))
                .save(writer, Mod.loc(getItemName(ModItems.AP_HEAD.get())));
    }

    private static void buildBlockRecipes(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.AIRCRAFT_CATAPULT.get(), 8)
                .pattern("aaa")
                .pattern("cbc")
                .pattern("ddd")
                .define('a', Items.POWERED_RAIL)
                .define('b', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .define('c', Tags.Items.INGOTS_COPPER)
                .define('d', Tags.Items.INGOTS_IRON)
                .unlockedBy(getHasName(Items.POWERED_RAIL), has(Items.POWERED_RAIL))
                .save(writer, Mod.loc(getItemName(ModItems.AIRCRAFT_CATAPULT.get())));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.SUPERB_ITEM_INTERFACE.get())
                .pattern("cac")
                .pattern("aba")
                .pattern("cac")
                .define('a', Items.HOPPER)
                .define('b', Items.DROPPER)
                .define('c', ModTags.Items.INGOTS_STEEL)
                .unlockedBy(getHasName(Items.HOPPER), has(Items.DROPPER))
                .save(writer, Mod.loc(getItemName(ModItems.SUPERB_ITEM_INTERFACE.get())));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.VEHICLE_ASSEMBLING_TABLE.get())
                .pattern("aaa")
                .pattern("bcd")
                .pattern("eee")
                .define('a', Items.IRON_INGOT)
                .define('b', Tags.Items.STORAGE_BLOCKS_IRON)
                .define('c', Items.SMITHING_TABLE)
                .define('d', Tags.Items.GLASS_PANES)
                .define('e', ModTags.Items.INGOTS_STEEL)
                .unlockedBy(getHasName(Items.SMITHING_TABLE), has(Items.SMITHING_TABLE))
                .save(writer, Mod.loc(getItemName(ModItems.VEHICLE_ASSEMBLING_TABLE.get())));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.BARBED_WIRE.get(), 2)
                .pattern("aba")
                .define('a', ItemTags.PLANKS)
                .define('b', Items.IRON_BARS)
                .unlockedBy(getHasName(Items.IRON_BARS), has(Items.IRON_BARS))
                .save(writer, Mod.loc(getItemName(ModItems.BARBED_WIRE.get())));
    }

    private static void buildVehicleRecipes(Consumer<FinishedRecipe> writer) {
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

        VehicleAssemblingRecipeBuilder.item(ModItems.SMALL_BATTERY_PACK.get(), 1, VehicleAssemblingRecipe.Category.MISC)
                .require(commonItemTag("plates/copper"), 4)
                .require(commonItemTag("glass_panes"), 8)
                .require(Items.REDSTONE, 4)
                .require(Items.IRON_INGOT, 4)
                .unlockedBy(getHasName(ModItems.COPPER_PLATE.get()), has(ModItems.COPPER_PLATE.get()))
                .save(writer, Mod.loc(getItemName(ModItems.SMALL_BATTERY_PACK.get())));
        VehicleAssemblingRecipeBuilder.item(ModItems.MEDIUM_BATTERY_PACK.get(), 1, VehicleAssemblingRecipe.Category.MISC)
                .require(commonItemTag("plates/copper"), 36)
                .require(commonItemTag("glass_panes"), 72)
                .require(Items.REDSTONE, 36)
                .require(Items.IRON_INGOT, 36)
                .unlockedBy(getHasName(ModItems.COPPER_PLATE.get()), has(ModItems.COPPER_PLATE.get()))
                .save(writer, Mod.loc(getItemName(ModItems.MEDIUM_BATTERY_PACK.get())));
        VehicleAssemblingRecipeBuilder.item(ModItems.LARGE_BATTERY_PACK.get(), 1, VehicleAssemblingRecipe.Category.MISC)
                .require(commonItemTag("plates/copper"), 144)
                .require(commonItemTag("glass_panes"), 288)
                .require(Items.REDSTONE, 144)
                .require(Items.IRON_INGOT, 144)
                .unlockedBy(getHasName(ModItems.COPPER_PLATE.get()), has(ModItems.COPPER_PLATE.get()))
                .save(writer, Mod.loc(getItemName(ModItems.LARGE_BATTERY_PACK.get())));
    }

    private static void buildGunRecipes(Consumer<FinishedRecipe> writer) {
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
    }

    private static void buildBlueprintRecipes(Consumer<FinishedRecipe> writer) {
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

    private static void buildPerkRecipes(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EMPTY_PERK.get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', Items.PAPER)
                .define('b', Items.LAPIS_LAZULI)
                .define('c', Tags.Items.INGOTS_IRON)
                .unlockedBy(getHasName(Items.PAPER), has(Items.PAPER))
                .save(writer, Mod.loc(getItemName(ModItems.EMPTY_PERK.get())));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS.get(ModPerks.AP_BULLET).get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', commonItemTag("storage_blocks/tungsten"))
                .define('c', commonItemTag("ingots/tungsten"))
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.AP_BULLET));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS.get(ModPerks.CUPID_ARROW).get())
                .pattern("cbc")
                .pattern("dad")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Items.BOW)
                .define('c', ItemTags.ARROWS)
                .define('d', getPotionIngredient(Potions.HEALING))
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.CUPID_ARROW));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS.get(ModPerks.FIREFLY).get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Ingredient.of(Items.OCHRE_FROGLIGHT, Items.VERDANT_FROGLIGHT, Items.PEARLESCENT_FROGLIGHT))
                .define('c', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.FIREFLY));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS.get(ModPerks.HE_BULLET).get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Items.TNT)
                .define('c', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.HE_BULLET));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS.get(ModPerks.INCENDIARY_BULLET).get())
                .pattern("bbb")
                .pattern("cac")
                .pattern("bbb")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Items.BLAZE_POWDER)
                .define('c', Items.DRAGON_BREATH)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.INCENDIARY_BULLET));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS.get(ModPerks.INTELLIGENT_CHIP).get())
                .pattern("bbb")
                .pattern("bab")
                .pattern("bbb")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', ModItems.ANCIENT_CPU.get())
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.INTELLIGENT_CHIP));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS.get(ModPerks.JHP_BULLET).get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Tags.Items.STORAGE_BLOCKS_COPPER)
                .define('c', Tags.Items.INGOTS_COPPER)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.JHP_BULLET));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS.get(ModPerks.LONGER_WIRE).get())
                .pattern("bbb")
                .pattern("bab")
                .pattern("bbb")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Items.STRING)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.LONGER_WIRE));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS.get(ModPerks.MICRO_MISSILE).get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', ModItems.GRAIN.get())
                .define('c', Items.FIREWORK_ROCKET)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.MICRO_MISSILE));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS.get(ModPerks.PHASE_PENETRATING_BULLET).get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Tags.Items.INGOTS_NETHERITE)
                .define('c', ModItems.AP_HEAD.get())
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.PHASE_PENETRATING_BULLET));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS.get(ModPerks.POISONOUS_BULLET).get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', commonItemTag("storage_blocks/lead"))
                .define('c', Items.SPIDER_EYE)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.POISONOUS_BULLET));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS.get(ModPerks.POWERFUL_ATTRACTION).get())
                .pattern("dbe")
                .pattern("cac")
                .pattern(" c ")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Tags.Items.ENDER_PEARLS)
                .define('c', Tags.Items.INGOTS_IRON)
                .define('d', Tags.Items.DUSTS_REDSTONE)
                .define('e', Tags.Items.GEMS_LAPIS)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.POWERFUL_ATTRACTION));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS.get(ModPerks.REGENERATION).get())
                .pattern("ccc")
                .pattern("bab")
                .pattern("ddd")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', ModItems.CELL.get())
                .define('c', Items.DAYLIGHT_DETECTOR)
                .define('d', Tags.Items.INGOTS_GOLD)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.REGENERATION));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS.get(ModPerks.RIOT_BULLET).get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Items.SLIME_BLOCK)
                .define('c', Items.COBWEB)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.RIOT_BULLET));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS.get(ModPerks.SILVER_BULLET).get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', commonItemTag("storage_blocks/silver"))
                .define('c', commonItemTag("ingots/silver"))
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.SILVER_BULLET));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS.get(ModPerks.TURBO_CHARGER).get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Items.PISTON)
                .define('c', commonItemTag("ingots/steel"))
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.TURBO_CHARGER));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS.get(ModPerks.VOLT_OVERLOAD).get())
                .pattern("cec")
                .pattern("bab")
                .pattern("bdb")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', ModItems.CELL.get())
                .define('c', Items.LIGHTNING_ROD)
                .define('d', commonItemTag("dusts/coal_coke"))
                .define('e', Tags.Items.INGOTS_IRON)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.VOLT_OVERLOAD));
    }

    private static void buildSpecialRecipes(Consumer<FinishedRecipe> writer) {
        SpecialRecipeBuilder.special(ModRecipes.POTION_MORTAR_SHELL_SERIALIZER.get()).save(writer, "potion_mortar_shell");
        SpecialRecipeBuilder.special(ModRecipes.AMMO_BOX_ADD_AMMO_SERIALIZER.get()).save(writer, "ammo_box_add_ammo");
        SpecialRecipeBuilder.special(ModRecipes.AMMO_BOX_EXTRACT_AMMO_SERIALIZER.get()).save(writer, "ammo_box_extract_ammo");
        SpecialRecipeBuilder.special(ModRecipes.SMOKE_DYE_SERIALIZER.get()).save(writer, "smoke_dye");
    }

    public static void copyBlueprint(Consumer<FinishedRecipe> writer, ItemLike result) {
        copySmithingTemplate(writer, result, Items.LAPIS_LAZULI);
    }

    public static void gunSmithing(Consumer<FinishedRecipe> writer, ItemLike blueprint, GunRarity rarity, TagKey<Item> tagKey, Item pResultItem) {
        gunSmithing(writer, blueprint, rarity, Ingredient.of(tagKey), pResultItem);
    }

    public static void gunSmithing(Consumer<FinishedRecipe> writer, ItemLike blueprint, GunRarity rarity, ItemLike ingredient, Item pResultItem) {
        gunSmithing(writer, blueprint, rarity, Ingredient.of(ingredient), pResultItem);
    }

    public static void gunSmithing(Consumer<FinishedRecipe> writer, ItemLike blueprint, GunRarity rarity, Ingredient ingredient, Item pResultItem) {
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

    public static ResourceLocation perkLoc(RegistryObject<Perk> perk) {
        return Mod.loc("perk/" + getItemName(ModItems.PERK_ITEMS.get(perk).get()));
    }

    protected static String getEntityTypeName(EntityType<?> entityType) {
        return EntityType.getKey(entityType).getPath();
    }

    // 生成材料包所有材料的配方
    public static void generateMaterialRecipes(@NotNull Consumer<FinishedRecipe> writer, ModItems.Materials material, Item ingredient) {
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

    public static void generateSmithingMaterialRecipe(@NotNull Consumer<FinishedRecipe> writer, ModItems.Materials material, ModItems.Materials result, Item template, Item ingredient) {
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

    public static void generateMaterialPackRecipe(@NotNull Consumer<FinishedRecipe> writer, ModItems.Materials material, Item pack) {
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

    public static StrictNBTIngredient getPotionIngredient(Potion potion) {
        var stack = new ItemStack(Items.POTION);
        PotionUtils.setPotion(stack, potion);
        return StrictNBTIngredient.of(stack);
    }
}
