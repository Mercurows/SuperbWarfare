package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.datagen.builder.ResearchingRecipeBuilder
import com.atsuishio.superbwarfare.datagen.builder.VehicleAssemblingRecipeBuilder
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.init.ModItems.Materials
import com.atsuishio.superbwarfare.init.ModTags.commonItemTag
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.recipe.PotionMortarShellRecipe
import com.atsuishio.superbwarfare.recipe.SmokeDyeRecipe
import com.atsuishio.superbwarfare.recipe.VehicleResetRecipe
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe
import com.atsuishio.superbwarfare.tools.NBTTool
import net.minecraft.core.Holder
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.*
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.item.crafting.BlastingRecipe
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.SmeltingRecipe
import net.minecraft.world.level.ItemLike
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.common.crafting.DataComponentIngredient
import net.neoforged.neoforge.registries.DeferredHolder
import java.util.concurrent.CompletableFuture

class ModRecipeProvider(output: PackOutput, registries: CompletableFuture<HolderLookup.Provider>) :
    RecipeProvider(output, registries) {
    override fun buildRecipes(writer: RecipeOutput) {
        buildToolRecipes(writer)
        buildArmorRecipes(writer)
        buildAmmoRecipes(writer)
        buildMaterialRecipes(writer)
        buildBlockRecipes(writer)
        buildVehicleRecipes(writer)
        buildGunRecipes(writer)
        buildBlueprintRecipes(writer)
        buildPerkRecipes(writer)
        buildMiscRecipes(writer)
        buildSpecialRecipes(writer)
        buildResearchRecipes(writer)
    }

    enum class GunRarity {
        COMMON,
        RARE,
        EPIC,
        LEGENDARY,
        SUPERB,
        VIRTUAL
    }

    companion object {
        val PLATES_COPPER: TagKey<Item> = commonItemTag("plates/copper")
        val INGOTS_LEAD: TagKey<Item> = commonItemTag("ingots/lead")
        val INGOTS_SILVER: TagKey<Item> = commonItemTag("ingots/silver")
        val INGOTS_TUNGSTEN: TagKey<Item> = commonItemTag("ingots/tungsten")

        private fun buildToolRecipes(writer: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ARTILLERY_INDICATOR.get())
                .pattern(" b ")
                .pattern("aca")
                .define('a', Items.SPYGLASS)
                .define('b', ModItems.MONITOR.get())
                .define('c', ModItems.FIRING_PARAMETERS.get())
                .unlockedBy(getHasName(Items.SPYGLASS), has(Items.SPYGLASS))
                .save(writer, loc(getItemName(ModItems.ARTILLERY_INDICATOR.get())))
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ARTILLERY_INDICATOR.get())
                .requires(ModItems.ARTILLERY_INDICATOR.get())
                .unlockedBy(getHasName(ModItems.ARTILLERY_INDICATOR.get()), has(ModItems.ARTILLERY_INDICATOR.get()))
                .save(writer, loc(getItemName(ModItems.ARTILLERY_INDICATOR.get()) + "_clear"))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.STEEL_PIPE.get())
                .pattern(" a")
                .pattern("a ")
                .define('a', ModItems.STEEL_MATERIALS.barrel.get())
                .unlockedBy(
                    getHasName(ModItems.STEEL_MATERIALS.barrel.get()),
                    has(ModItems.STEEL_MATERIALS.barrel.get())
                )
                .save(writer, loc(getItemName(ModItems.STEEL_PIPE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MEDICAL_KIT.get(), 4)
                .pattern("aba")
                .pattern("bcb")
                .pattern("aba")
                .define('a', Items.STRING)
                .define('b', ItemTags.WOOL_CARPETS)
                .define('c', Items.GOLDEN_APPLE)
                .unlockedBy(getHasName(Items.GOLDEN_APPLE), has(Items.GOLDEN_APPLE))
                .save(writer, loc(getItemName(ModItems.MEDICAL_KIT.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ARMOR_PLATE.get(), 4)
                .pattern("aba")
                .pattern("ccc")
                .pattern("ada")
                .define('a', Items.STRING)
                .define('b', ItemTags.TERRACOTTA)
                .define('c', ModTags.Items.INGOTS_STEEL)
                .define('d', ItemTags.WOOL)
                .unlockedBy(getHasName(Items.STRING), has(Items.STRING))
                .save(writer, loc(getItemName(ModItems.ARMOR_PLATE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.VEHICLE_DAMAGE_ANALYZER.get())
                .pattern("aba")
                .pattern("aca")
                .pattern("ada")
                .define('a', Tags.Items.INGOTS_GOLD)
                .define('b', Items.OBSERVER)
                .define('c', Items.NOTE_BLOCK)
                .define('d', Tags.Items.INGOTS_IRON)
                .unlockedBy(getHasName(Items.OBSERVER), has(Items.OBSERVER))
                .save(writer, loc(getItemName(ModItems.VEHICLE_DAMAGE_ANALYZER.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.HAMMER.get())
                .pattern("aba")
                .pattern(" c ")
                .pattern(" c ")
                .define('a', Tags.Items.INGOTS_IRON)
                .define('b', Tags.Items.STORAGE_BLOCKS_IRON)
                .define('c', Items.STICK)
                .unlockedBy(getHasName(Items.IRON_BLOCK), has(Tags.Items.STORAGE_BLOCKS_IRON))
                .save(writer, loc(getItemName(ModItems.HAMMER.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.GOLDEN_HAMMER.get())
                .pattern("aba")
                .pattern(" c ")
                .pattern(" c ")
                .define('a', Tags.Items.INGOTS_GOLD)
                .define('b', Tags.Items.STORAGE_BLOCKS_GOLD)
                .define('c', Items.STICK)
                .unlockedBy(getHasName(Items.GOLD_BLOCK), has(Tags.Items.STORAGE_BLOCKS_GOLD))
                .save(writer, loc(getItemName(ModItems.GOLDEN_HAMMER.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.STEEL_HAMMER.get())
                .pattern("aba")
                .pattern(" c ")
                .pattern(" c ")
                .define('a', ModTags.Items.INGOTS_STEEL)
                .define('b', ModTags.Items.STORAGE_BLOCK_STEEL)
                .define('c', Items.STICK)
                .unlockedBy(getHasName(ModItems.STEEL_BLOCK.get()), has(ModTags.Items.STORAGE_BLOCK_STEEL))
                .save(writer, loc(getItemName(ModItems.STEEL_HAMMER.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.DIAMOND_HAMMER.get())
                .pattern("aba")
                .pattern(" c ")
                .pattern(" c ")
                .define('a', Tags.Items.GEMS_DIAMOND)
                .define('b', Tags.Items.STORAGE_BLOCKS_DIAMOND)
                .define('c', Items.STICK)
                .unlockedBy(getHasName(Items.DIAMOND_BLOCK), has(Tags.Items.STORAGE_BLOCKS_DIAMOND))
                .save(writer, loc(getItemName(ModItems.DIAMOND_HAMMER.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.CEMENTED_CARBIDE_HAMMER.get())
                .pattern("aba")
                .pattern(" c ")
                .pattern(" c ")
                .define('a', ModTags.Items.INGOTS_CEMENTED_CARBIDE)
                .define('b', ModTags.Items.STORAGE_BLOCK_CEMENTED_CARBIDE)
                .define('c', Items.STICK)
                .unlockedBy(
                    getHasName(ModItems.CEMENTED_CARBIDE_BLOCK.get()),
                    has(ModTags.Items.STORAGE_BLOCK_CEMENTED_CARBIDE)
                )
                .save(writer, loc(getItemName(ModItems.CEMENTED_CARBIDE_HAMMER.get())))
            SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(ModItems.CEMENTED_CARBIDE_HAMMER.get()),
                Ingredient.of(Tags.Items.STORAGE_BLOCKS_NETHERITE),
                RecipeCategory.MISC,
                ModItems.NETHERITE_HAMMER.get()
            )
                .unlocks(
                    getHasName(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                    has(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
                )
                .unlocks(
                    getHasName(ModItems.CEMENTED_CARBIDE_HAMMER.get()),
                    has(ModItems.CEMENTED_CARBIDE_HAMMER.get())
                )
                .save(writer, loc(getItemName(ModItems.NETHERITE_HAMMER.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.CROWBAR.get())
                .pattern("  a")
                .pattern(" b ")
                .pattern("b  ")
                .define('a', ModTags.Items.INGOTS_STEEL)
                .define('b', Tags.Items.INGOTS_IRON)
                .unlockedBy(getHasName(ModItems.STEEL_INGOT.get()), has(ModTags.Items.INGOTS_STEEL))
                .save(writer, loc(getItemName(ModItems.CROWBAR.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.MILITARY_SHOVEL.get())
                .pattern(" aa")
                .pattern(" ba")
                .pattern("a  ")
                .define('a', ModTags.Items.INGOTS_STEEL)
                .define('b', ModTags.Items.INGOTS_CEMENTED_CARBIDE)
                .unlockedBy(getHasName(ModItems.STEEL_INGOT.get()), has(ModTags.Items.INGOTS_STEEL))
                .save(writer, loc(getItemName(ModItems.MILITARY_SHOVEL.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DEFUSER.get())
                .pattern("  a")
                .pattern("cb ")
                .pattern(" c ")
                .define('a', ModTags.Items.INGOTS_STEEL)
                .define('b', Tags.Items.NUGGETS_IRON)
                .define('c', Items.STICK)
                .unlockedBy(getHasName(ModItems.STEEL_INGOT.get()), has(ModTags.Items.INGOTS_STEEL))
                .save(writer, loc(getItemName(ModItems.DEFUSER.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DETONATOR.get())
                .pattern(" a")
                .pattern("bc")
                .define('a', Items.REDSTONE_TORCH)
                .define('b', Items.STONE_BUTTON)
                .define('c', Tags.Items.INGOTS_IRON)
                .unlockedBy(getHasName(Items.REDSTONE_TORCH), has(Items.REDSTONE_TORCH))
                .save(writer, loc(getItemName(ModItems.DETONATOR.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.ELECTRIC_BATON.get())
                .pattern("  a")
                .pattern(" b ")
                .pattern("c  ")
                .define('a', Items.LIGHTNING_ROD)
                .define('b', ModItems.BATTERY.get())
                .define('c', ModTags.Items.INGOTS_STEEL)
                .unlockedBy(getHasName(Items.LIGHTNING_ROD), has(Items.LIGHTNING_ROD))
                .unlockedBy(getHasName(ModItems.BATTERY.get()), has(ModItems.BATTERY.get()))
                .save(writer, loc(getItemName(ModItems.ELECTRIC_BATON.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.KNIFE.get())
                .pattern(" a")
                .pattern("b ")
                .define('a', ModTags.Items.INGOTS_STEEL)
                .define('b', Items.STICK)
                .unlockedBy(getHasName(ModItems.STEEL_INGOT.get()), has(ModTags.Items.INGOTS_STEEL))
                .save(writer, loc(getItemName(ModItems.KNIFE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MONITOR.get())
                .pattern("a a")
                .pattern("bcb")
                .pattern("ded")
                .define('a', Items.LIGHTNING_ROD)
                .define('b', Items.LEVER)
                .define('c', Tags.Items.INGOTS_IRON)
                .define('d', Items.AMETHYST_SHARD)
                .define('e', Tags.Items.GLASS_PANES)
                .unlockedBy(getHasName(Items.LIGHTNING_ROD), has(Items.LIGHTNING_ROD))
                .save(writer, loc(getItemName(ModItems.MONITOR.get())))
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MONITOR.get())
                .requires(ModItems.MONITOR.get())
                .unlockedBy(getHasName(ModItems.MONITOR.get()), has(ModItems.MONITOR.get()))
                .save(writer, loc(getItemName(ModItems.MONITOR.get()) + "_clear"))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MORTAR_DEPLOYER.get())
                .pattern("a ")
                .pattern("bc")
                .define('a', ModItems.MORTAR_BARREL.get())
                .define('b', ModItems.MORTAR_BIPOD.get())
                .define('c', ModItems.MORTAR_BASE_PLATE.get())
                .unlockedBy(getHasName(ModItems.MORTAR_BARREL.get()), has(ModItems.MORTAR_BARREL.get()))
                .unlockedBy(getHasName(ModItems.MORTAR_BIPOD.get()), has(ModItems.MORTAR_BIPOD.get()))
                .unlockedBy(getHasName(ModItems.MORTAR_BASE_PLATE.get()), has(ModItems.MORTAR_BASE_PLATE.get()))
                .save(writer, loc(getItemName(ModItems.MORTAR_DEPLOYER.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.T_BATON.get())
                .pattern("  a")
                .pattern(" a ")
                .pattern("ab ")
                .define('a', Tags.Items.INGOTS_IRON)
                .define('b', ModTags.Items.INGOTS_STEEL)
                .unlockedBy(getHasName(ModItems.STEEL_INGOT.get()), has(ModTags.Items.INGOTS_STEEL))
                .save(writer, loc(getItemName(ModItems.T_BATON.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DPS_GENERATOR_DEPLOYER.get())
                .pattern("a")
                .pattern("b")
                .pattern("c")
                .define('a', ModItems.TARGET_DEPLOYER.get())
                .define('b', ModItems.LARGE_MOTOR.get())
                .define('c', ModItems.CHARGING_STATION.get())
                .unlockedBy(getHasName(ModItems.CHARGING_STATION.get()), has(ModItems.CHARGING_STATION.get()))
                .save(writer, loc(getItemName(ModItems.DPS_GENERATOR_DEPLOYER.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TARGET_DEPLOYER.get())
                .pattern("a")
                .pattern("b")
                .pattern("c")
                .define('a', Items.TARGET)
                .define('b', ModTags.Items.INGOTS_STEEL)
                .define('c', Items.ARMOR_STAND)
                .unlockedBy(getHasName(Items.TARGET), has(Items.TARGET))
                .save(writer, loc(getItemName(ModItems.TARGET_DEPLOYER.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TOW_DEPLOYER.get())
                .pattern("c ")
                .pattern("ab")
                .pattern("d ")
                .define('a', Items.DISPENSER)
                .define('b', ModItems.MORTAR_BARREL.get())
                .define('c', ModItems.ARTILLERY_INDICATOR.get())
                .define('d', ModTags.Items.STORAGE_BLOCK_STEEL)
                .unlockedBy(getHasName(Items.DISPENSER), has(Items.DISPENSER))
                .save(writer, loc(getItemName(ModItems.TOW_DEPLOYER.get())))
        }

        private fun buildArmorRecipes(writer: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.GE_HELMET_M_35.get())
                .pattern("aaa")
                .pattern("aba")
                .define('a', ModTags.Items.INGOTS_STEEL)
                .define('b', Tags.Items.DYES_BLACK)
                .unlockedBy(getHasName(ModItems.STEEL_INGOT.get()), has(ModTags.Items.INGOTS_STEEL))
                .save(writer, loc(getItemName(ModItems.GE_HELMET_M_35.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.RU_HELMET_6B47.get())
                .pattern("aca")
                .pattern("aba")
                .define('a', ModTags.Items.INGOTS_CEMENTED_CARBIDE)
                .define('b', Tags.Items.DYES_GREEN)
                .define('c', ModItems.CEMENTED_CARBIDE_INGOT.get())
                .unlockedBy(
                    getHasName(ModItems.CEMENTED_CARBIDE_INGOT.get()),
                    has(ModItems.CEMENTED_CARBIDE_INGOT.get())
                )
                .save(writer, loc(getItemName(ModItems.RU_HELMET_6B47.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.RU_CHEST_6B43.get())
                .pattern("aba")
                .pattern("aca")
                .pattern("aaa")
                .define('a', ModTags.Items.INGOTS_CEMENTED_CARBIDE)
                .define('b', Tags.Items.DYES_GREEN)
                .define('c', ModItems.CEMENTED_CARBIDE_INGOT.get())
                .unlockedBy(
                    getHasName(ModItems.CEMENTED_CARBIDE_INGOT.get()),
                    has(ModItems.CEMENTED_CARBIDE_INGOT.get())
                )
                .save(writer, loc(getItemName(ModItems.RU_CHEST_6B43.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.US_HELMET_PASGT.get())
                .pattern("aca")
                .pattern("aba")
                .define('a', ModTags.Items.INGOTS_CEMENTED_CARBIDE)
                .define('b', Tags.Items.SANDS)
                .define('c', ModItems.CEMENTED_CARBIDE_INGOT.get())
                .unlockedBy(
                    getHasName(ModItems.CEMENTED_CARBIDE_INGOT.get()),
                    has(ModItems.CEMENTED_CARBIDE_INGOT.get())
                )
                .save(writer, loc(getItemName(ModItems.US_HELMET_PASGT.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.US_CHEST_IOTV.get())
                .pattern("aba")
                .pattern("aca")
                .pattern("aaa")
                .define('a', ModTags.Items.INGOTS_CEMENTED_CARBIDE)
                .define('b', Tags.Items.SANDS)
                .define('c', ModItems.CEMENTED_CARBIDE_INGOT.get())
                .unlockedBy(
                    getHasName(ModItems.CEMENTED_CARBIDE_INGOT.get()),
                    has(ModItems.CEMENTED_CARBIDE_INGOT.get())
                )
                .save(writer, loc(getItemName(ModItems.US_CHEST_IOTV.get())))
        }

        private fun buildAmmoRecipes(writer: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AMMO_BOX.get())
                .pattern("aba")
                .pattern("aaa")
                .define('a', Tags.Items.INGOTS_IRON)
                .define('b', Tags.Items.DYES_GREEN)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, loc(getItemName(ModItems.AMMO_BOX.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.LARGE_ANTI_GROUND_MISSILE.get())
                .pattern(" b ")
                .pattern("ada")
                .pattern("cec")
                .define('a', PLATES_COPPER)
                .define('b', ModItems.SEEKER.get())
                .define('c', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('d', Items.TNT)
                .define('e', ModItems.MISSILE_ENGINE.get())
                .unlockedBy(getHasName(ModItems.MISSILE_ENGINE.get()), has(ModItems.MISSILE_ENGINE.get()))
                .save(writer, loc(getItemName(ModItems.LARGE_ANTI_GROUND_MISSILE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.SMALL_ROCKET.get(), 4)
                .pattern(" a ")
                .pattern("bcb")
                .pattern(" d ")
                .define('a', ModItems.FUSEE.get())
                .define('b', Items.COPPER_INGOT)
                .define('c', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('d', ModItems.GRAIN.get())
                .unlockedBy(getHasName(ModItems.FUSEE.get()), has(ModItems.FUSEE.get()))
                .save(writer, loc(getItemName(ModItems.SMALL_ROCKET.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.RPG_ROCKET_TBG.get(), 2)
                .pattern(" a ")
                .pattern("bcb")
                .pattern(" d ")
                .define('a', ModItems.FUSEE.get())
                .define('b', Items.IRON_INGOT)
                .define('c', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('d', ModItems.GRAIN.get())
                .unlockedBy(getHasName(ModItems.FUSEE.get()), has(ModItems.FUSEE.get()))
                .save(writer, loc(getItemName(ModItems.RPG_ROCKET_TBG.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.RPG_ROCKET_STANDARD.get(), 2)
                .pattern(" a ")
                .pattern("bcb")
                .pattern("ede")
                .define('a', ModItems.FUSEE.get())
                .define('b', Items.IRON_INGOT)
                .define('c', PLATES_COPPER)
                .define('d', ModItems.GRAIN.get())
                .define('e', Items.GUNPOWDER)
                .unlockedBy(getHasName(ModItems.FUSEE.get()), has(ModItems.FUSEE.get()))
                .save(writer, loc(getItemName(ModItems.RPG_ROCKET_STANDARD.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.C4_BOMB.get(), 2)
                .pattern("aaa")
                .pattern("aba")
                .pattern("aaa")
                .define('a', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('b', Items.CLOCK)
                .unlockedBy(
                    getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()),
                    has(ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                )
                .save(writer, loc(getItemName(ModItems.C4_BOMB.get())))
            ShapedRecipeBuilder(
                RecipeCategory.COMBAT,
                NBTTool.withTag(
                    ModItems.C4_BOMB,
                    2
                ) { tag -> tag.putBoolean("Control", true) }
            )
                .pattern("aaa")
                .pattern("aba")
                .pattern("aaa")
                .define('a', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('b', Items.COMPARATOR)
                .unlockedBy(
                    getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()),
                    has(ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                )
                .save(writer, loc(getItemName(ModItems.C4_BOMB.get()) + "_rc"))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.LARGE_SHELL_AP.get())
                .pattern("a")
                .pattern("b")
                .define('a', ModItems.AP_HEAD.get())
                .define('b', ModItems.GRAIN.get())
                .unlockedBy(getHasName(ModItems.AP_HEAD.get()), has(ModItems.AP_HEAD.get()))
                .save(writer, loc(getItemName(ModItems.LARGE_SHELL_AP.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.BLU_43_MINE.get(), 8)
                .pattern("a")
                .pattern("b")
                .pattern("c")
                .define('a', Items.STONE_PRESSURE_PLATE)
                .define('b', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('c', Items.GREEN_CONCRETE)
                .unlockedBy(
                    getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()),
                    has(ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                )
                .save(writer, loc(getItemName(ModItems.BLU_43_MINE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.CLAYMORE_MINE.get(), 2)
                .pattern(" a ")
                .pattern("bcb")
                .pattern("d d")
                .define('a', Items.TRIPWIRE_HOOK)
                .define('b', Tags.Items.INGOTS_IRON)
                .define('c', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('d', Items.STICK)
                .unlockedBy(
                    getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()),
                    has(ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                )
                .save(writer, loc(getItemName(ModItems.CLAYMORE_MINE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.EDD.get(), 4)
                .pattern(" a ")
                .pattern("bcb")
                .pattern(" d ")
                .define('a', ModItems.LASER_UNIT.get())
                .define('b', Tags.Items.INGOTS_IRON)
                .define('c', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('d', ItemTags.PLANKS)
                .unlockedBy(
                    getHasName(ModItems.LASER_UNIT.get()),
                    has(ModItems.LASER_UNIT.get())
                )
                .save(writer, loc(getItemName(ModItems.EDD.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.LARGE_SHELL_CM.get())
                .pattern("a")
                .pattern("b")
                .define('a', ModItems.CM_HEAD.get())
                .define('b', ModItems.GRAIN.get())
                .unlockedBy(getHasName(ModItems.CM_HEAD.get()), has(ModItems.CM_HEAD.get()))
                .save(writer, loc(getItemName(ModItems.LARGE_SHELL_CM.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.GRENADE_40MM.get(), 6)
                .pattern(" a ")
                .pattern("bcb")
                .pattern(" d ")
                .define('a', ModItems.FUSEE.get())
                .define('b', Tags.Items.INGOTS_IRON)
                .define('c', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('d', ModItems.PRIMER.get())
                .unlockedBy(
                    getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()),
                    has(ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                )
                .unlockedBy(getHasName(ModItems.FUSEE.get()), has(ModItems.FUSEE.get()))
                .save(writer, loc(getItemName(ModItems.GRENADE_40MM.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.LARGE_SHELL_GS.get())
                .pattern("a")
                .pattern("b")
                .define('a', ModItems.GS_HEAD.get())
                .define('b', ModItems.GRAIN.get())

                .unlockedBy(getHasName(ModItems.GS_HEAD.get()), has(ModItems.GS_HEAD.get()))
                .save(writer, loc(getItemName(ModItems.LARGE_SHELL_GS.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.LARGE_SHELL_WP.get())
                .pattern("a")
                .pattern("b")
                .define('a', ModItems.WP_HEAD.get())
                .define('b', ModItems.GRAIN.get())
                .unlockedBy(getHasName(ModItems.WP_HEAD.get()), has(ModItems.WP_HEAD.get()))
                .save(writer, loc(getItemName(ModItems.LARGE_SHELL_WP.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.HAND_GRENADE.get(), 4)
                .pattern(" a ")
                .pattern("bcb")
                .pattern("bcb")
                .define('a', Items.TRIPWIRE_HOOK)
                .define('b', Tags.Items.INGOTS_IRON)
                .define('c', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .unlockedBy(
                    getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()),
                    has(ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                )
                .save(writer, loc(getItemName(ModItems.HAND_GRENADE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.HANDGUN_AMMO.get(), 64)
                .pattern(" a ")
                .pattern("bcb")
                .pattern(" d ")
                .define('a', Tags.Items.INGOTS_COPPER)
                .define('b', PLATES_COPPER)
                .define('c', Items.GUNPOWDER)
                .define('d', ModItems.PRIMER.get())
                .unlockedBy(getHasName(ModItems.PRIMER.get()), has(ModItems.PRIMER.get()))
                .save(writer, loc(getItemName(ModItems.HANDGUN_AMMO.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.LARGE_SHELL_HE.get())
                .pattern("a")
                .pattern("b")
                .define('a', ModItems.HE_HEAD.get())
                .define('b', ModItems.GRAIN.get())
                .unlockedBy(getHasName(ModItems.HE_HEAD.get()), has(ModItems.HE_HEAD.get()))
                .save(writer, loc(getItemName(ModItems.LARGE_SHELL_HE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.HEAVY_AMMO.get(), 12)
                .pattern(" a ")
                .pattern("bcb")
                .pattern(" d ")
                .define('a', ModTags.Items.INGOTS_STEEL)
                .define('b', Tags.Items.INGOTS_COPPER)
                .define('c', Items.GUNPOWDER)
                .define('d', ModItems.PRIMER.get())
                .unlockedBy(getHasName(ModItems.PRIMER.get()), has(ModItems.PRIMER.get()))
                .save(writer, loc(getItemName(ModItems.HEAVY_AMMO.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.JAVELIN_MISSILE.get())
                .pattern(" a ")
                .pattern("bcb")
                .pattern(" d ")
                .define('a', ModItems.SEEKER.get())
                .define('b', Tags.Items.INGOTS_IRON)
                .define('c', ModItems.AP_HEAD.get())
                .define('d', ModItems.MISSILE_ENGINE.get())
                .unlockedBy(getHasName(ModItems.AP_HEAD.get()), has(ModItems.AP_HEAD.get()))
                .unlockedBy(getHasName(ModItems.MISSILE_ENGINE.get()), has(ModItems.MISSILE_ENGINE.get()))
                .save(writer, loc(getItemName(ModItems.JAVELIN_MISSILE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MEDIUM_ANTI_AIR_MISSILE.get())
                .pattern("eae")
                .pattern("bcb")
                .pattern(" d ")
                .define('a', ModItems.SEEKER.get())
                .define('b', Tags.Items.INGOTS_IRON)
                .define('c', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('d', ModItems.MISSILE_ENGINE.get())
                .define('e', Items.IRON_BARS)
                .unlockedBy(
                    getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()),
                    has(ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                )
                .unlockedBy(getHasName(ModItems.MISSILE_ENGINE.get()), has(ModItems.MISSILE_ENGINE.get()))
                .save(writer, loc(getItemName(ModItems.MEDIUM_ANTI_AIR_MISSILE.get())))

            ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, ModItems.MEDIUM_ANTI_GROUND_MISSILE.get())
                .requires(ModItems.JAVELIN_MISSILE.get())
                .unlockedBy(getHasName(ModItems.JAVELIN_MISSILE.get()), has(ModItems.JAVELIN_MISSILE.get()))
                .save(writer, loc(getItemName(ModItems.MEDIUM_ANTI_GROUND_MISSILE.get())))

            ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, ModItems.JAVELIN_MISSILE.get())
                .requires(ModItems.MEDIUM_ANTI_GROUND_MISSILE.get())
                .unlockedBy(
                    getHasName(ModItems.MEDIUM_ANTI_GROUND_MISSILE.get()),
                    has(ModItems.MEDIUM_ANTI_GROUND_MISSILE.get())
                )
                .save(writer, loc(getItemName(ModItems.JAVELIN_MISSILE.get()) + "_convert"))

            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.LUNGE_MINE.get(), 2)
                .pattern(" ba")
                .pattern(" cb")
                .pattern("c  ")
                .define('a', Items.TNT)
                .define('b', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('c', Items.STICK)
                .unlockedBy(
                    getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()),
                    has(ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                )
                .save(writer, loc(getItemName(ModItems.LUNGE_MINE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.M18_SMOKE_GRENADE.get(), 2)
                .pattern(" a ")
                .pattern("bcb")
                .pattern("bdb")
                .define('a', Items.TRIPWIRE_HOOK)
                .define('b', Tags.Items.NUGGETS_IRON)
                .define('c', Items.WHEAT)
                .define('d', Items.GUNPOWDER)
                .unlockedBy(getHasName(Items.TRIPWIRE_HOOK), has(Items.TRIPWIRE_HOOK))
                .save(writer, loc(getItemName(ModItems.M18_SMOKE_GRENADE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MEDIUM_AERIAL_BOMB.get())
                .pattern(" c ")
                .pattern("dad")
                .pattern(" b ")
                .define('a', Items.TNT)
                .define('b', Tags.Items.INGOTS_IRON)
                .define('c', ModItems.FUSEE.get())
                .define('d', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .unlockedBy(getHasName(ModItems.FUSEE.get()), has(ModItems.FUSEE.get()))
                .unlockedBy(
                    getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()),
                    has(ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                )
                .save(writer, loc(getItemName(ModItems.MEDIUM_AERIAL_BOMB.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MEDIUM_ROCKET_AP.get())
                .pattern("a")
                .pattern("b")
                .pattern("b")
                .define('a', ModItems.AP_HEAD.get())
                .define('b', ModItems.SMALL_ROCKET.get())
                .unlockedBy(getHasName(ModItems.AP_HEAD.get()), has(ModItems.AP_HEAD.get()))
                .save(writer, loc(getItemName(ModItems.MEDIUM_ROCKET_AP.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MEDIUM_ROCKET_CM.get())
                .pattern("a")
                .pattern("b")
                .pattern("b")
                .define('a', ModItems.CM_HEAD.get())
                .define('b', ModItems.SMALL_ROCKET.get())
                .unlockedBy(getHasName(ModItems.CM_HEAD.get()), has(ModItems.CM_HEAD.get()))
                .save(writer, loc(getItemName(ModItems.MEDIUM_ROCKET_CM.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MEDIUM_ROCKET_HE.get())
                .pattern("a")
                .pattern("b")
                .pattern("b")
                .define('a', ModItems.HE_HEAD.get())
                .define('b', ModItems.SMALL_ROCKET.get())
                .unlockedBy(getHasName(ModItems.HE_HEAD.get()), has(ModItems.HE_HEAD.get()))
                .save(writer, loc(getItemName(ModItems.MEDIUM_ROCKET_HE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MORTAR_SHELL.get(), 4)
                .pattern(" a ")
                .pattern("bcb")
                .pattern(" d ")
                .define('a', ModItems.FUSEE.get())
                .define('b', ModTags.Items.INGOTS_STEEL)
                .define('c', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('d', ModItems.GRAIN.get())
                .unlockedBy(
                    getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()),
                    has(ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                )
                .unlockedBy(getHasName(ModItems.GRAIN.get()), has(ModItems.GRAIN.get()))
                .save(writer, loc(getItemName(ModItems.MORTAR_SHELL.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MORTAR_SHELL_WP.get(), 8)
                .pattern("eaf")
                .pattern("bcb")
                .pattern("fde")
                .define('a', ModItems.FUSEE.get())
                .define('b', ModTags.Items.INGOTS_STEEL)
                .define('c', Items.GUNPOWDER)
                .define('d', ModItems.GRAIN.get())
                .define('e', Items.BLAZE_POWDER)
                .define('f', Items.BONE_MEAL)
                .unlockedBy(
                    getHasName(Items.BLAZE_POWDER),
                    has(Items.BLAZE_POWDER)
                )
                .unlockedBy(getHasName(ModItems.GRAIN.get()), has(ModItems.GRAIN.get()))
                .save(writer, loc(getItemName(ModItems.MORTAR_SHELL_WP.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.PTKM_1R.get())
                .pattern(" b ")
                .pattern("dad")
                .pattern("ece")
                .define('a', Items.GUNPOWDER)
                .define('b', ModItems.LARGE_SHELL_AP.get())
                .define('c', Items.CALIBRATED_SCULK_SENSOR)
                .define('d', Tags.Items.INGOTS_IRON)
                .define('e', Items.IRON_BARS)
                .unlockedBy(getHasName(ModItems.LARGE_SHELL_AP.get()), has(ModItems.LARGE_SHELL_AP.get()))
                .unlockedBy(getHasName(Items.CALIBRATED_SCULK_SENSOR), has(Items.CALIBRATED_SCULK_SENSOR))
                .save(writer, loc(getItemName(ModItems.PTKM_1R.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.RGO_GRENADE.get(), 4)
                .pattern("abc")
                .pattern("aba")
                .pattern(" da")
                .define('a', Tags.Items.INGOTS_IRON)
                .define('b', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('c', Items.TRIPWIRE_HOOK)
                .define('d', Items.STONE_BUTTON)
                .unlockedBy(
                    getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()),
                    has(ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                )
                .save(writer, loc(getItemName(ModItems.RGO_GRENADE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.RIFLE_AMMO.get(), 48)
                .pattern(" a ")
                .pattern("bcb")
                .pattern(" d ")
                .define('a', ModTags.Items.INGOTS_STEEL)
                .define('b', PLATES_COPPER)
                .define('c', Items.GUNPOWDER)
                .define('d', ModItems.PRIMER.get())
                .unlockedBy(getHasName(ModItems.PRIMER.get()), has(ModItems.PRIMER.get()))
                .save(writer, loc(getItemName(ModItems.RIFLE_AMMO.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.SHOTGUN_AMMO.get(), 24)
                .pattern(" a ")
                .pattern("bcb")
                .pattern(" d ")
                .define('a', INGOTS_LEAD)
                .define('b', PLATES_COPPER)
                .define('c', Items.GUNPOWDER)
                .define('d', ModItems.PRIMER.get())
                .unlockedBy(getHasName(ModItems.PRIMER.get()), has(ModItems.PRIMER.get()))
                .save(writer, loc(getItemName(ModItems.SHOTGUN_AMMO.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.SNIPER_AMMO.get(), 16)
                .pattern(" a ")
                .pattern("bcb")
                .pattern(" d ")
                .define('a', INGOTS_TUNGSTEN)
                .define('b', PLATES_COPPER)
                .define('c', Items.GUNPOWDER)
                .define('d', ModItems.PRIMER.get())
                .unlockedBy(getHasName(ModItems.PRIMER.get()), has(ModItems.PRIMER.get()))
                .save(writer, loc(getItemName(ModItems.SNIPER_AMMO.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.SMALL_SHELL_AP.get(), 8)
                .pattern(" a ")
                .pattern("bcb")
                .pattern(" d ")
                .define('a', ModItems.TUNGSTEN_ROD.get())
                .define('b', Tags.Items.INGOTS_COPPER)
                .define('c', Items.GUNPOWDER)
                .define('d', ModItems.PRIMER.get())
                .unlockedBy(getHasName(ModItems.PRIMER.get()), has(ModItems.PRIMER.get()))
                .unlockedBy(getHasName(ModItems.TUNGSTEN_ROD.get()), has(ModItems.TUNGSTEN_ROD.get()))
                .save(writer, loc(getItemName(ModItems.SMALL_SHELL_AP.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.SMALL_SHELL_HE.get(), 8)
                .pattern(" a ")
                .pattern("bcb")
                .pattern(" d ")
                .define('a', ModTags.Items.INGOTS_STEEL)
                .define('b', Tags.Items.INGOTS_COPPER)
                .define('c', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('d', ModItems.PRIMER.get())
                .unlockedBy(getHasName(ModItems.PRIMER.get()), has(ModItems.PRIMER.get()))
                .unlockedBy(
                    getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()),
                    has(ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                )
                .save(writer, loc(getItemName(ModItems.SMALL_SHELL_HE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.SMALL_SHELL_GS.get(), 12)
                .pattern(" a ")
                .pattern("bcb")
                .pattern(" d ")
                .define('a', ModTags.Items.INGOTS_LEAD)
                .define('b', Tags.Items.INGOTS_COPPER)
                .define('c', Items.GUNPOWDER)
                .define('d', ModItems.PRIMER.get())
                .unlockedBy(getHasName(ModItems.PRIMER.get()), has(ModItems.PRIMER.get()))
                .unlockedBy(getHasName(ModItems.LEAD_INGOT.get()), has(ModItems.LEAD_INGOT.get()))
                .save(writer, loc(getItemName(ModItems.SMALL_SHELL_GS.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.SMALL_SHELL_AA.get(), 16)
                .pattern(" a ")
                .pattern("bcb")
                .pattern(" d ")
                .define('a', Items.IRON_INGOT)
                .define('b', Tags.Items.INGOTS_COPPER)
                .define('c', Items.GUNPOWDER)
                .define('d', ModItems.PRIMER.get())
                .unlockedBy(getHasName(ModItems.PRIMER.get()), has(ModItems.PRIMER.get()))
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, loc(getItemName(ModItems.SMALL_SHELL_AA.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.SWARM_DRONE.get(), 4)
                .pattern(" a ")
                .pattern("bcb")
                .pattern("ded")
                .define('a', ModItems.SEEKER.get())
                .define('b', ModItems.PROPELLER.get())
                .define('c', ModItems.MOTOR.get())
                .define('d', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('e', ModItems.CELL.get())
                .unlockedBy(getHasName(ModItems.PROPELLER.get()), has(ModItems.PROPELLER.get()))
                .unlockedBy(
                    getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()),
                    has(ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                )
                .save(writer, loc(getItemName(ModItems.SWARM_DRONE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.TASER_ELECTRODE.get(), 4)
                .pattern("a a")
                .pattern("b b")
                .pattern("b b")
                .define('a', Items.LIGHTNING_ROD)
                .define('b', Items.STRING)
                .unlockedBy(getHasName(Items.LIGHTNING_ROD), has(Items.LIGHTNING_ROD))
                .save(writer, loc(getItemName(ModItems.TASER_ELECTRODE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.TM_62.get(), 2)
                .pattern("cac")
                .pattern("bbb")
                .pattern("bbb")
                .define('a', Items.STONE_PRESSURE_PLATE)
                .define('b', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('c', Items.GREEN_CONCRETE)
                .unlockedBy(
                    getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()),
                    has(ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                )
                .save(writer, loc(getItemName(ModItems.TM_62.get())))
        }

        private fun buildMaterialRecipes(writer: RecipeOutput) {
            generateMaterialRecipes(writer, ModItems.IRON_MATERIALS, Items.IRON_INGOT)
            generateMaterialRecipes(
                writer,
                ModItems.STEEL_MATERIALS,
                ModTags.Items.INGOTS_STEEL,
                ModItems.STEEL_INGOT.get()
            )
            generateMaterialRecipes(
                writer,
                ModItems.CEMENTED_CARBIDE_MATERIALS,
                ModTags.Items.INGOTS_CEMENTED_CARBIDE,
                ModItems.CEMENTED_CARBIDE_INGOT.get()
            )
            generateSmithingMaterialRecipe(
                writer,
                ModItems.CEMENTED_CARBIDE_MATERIALS,
                ModItems.NETHERITE_MATERIALS,
                Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
                Items.NETHERITE_INGOT
            )
            mapOf(
                ModItems.CRYSTAL_MATERIALS.action to ModItems.CEMENTED_CARBIDE_MATERIALS.action,
                ModItems.CRYSTAL_MATERIALS.barrel to ModItems.CEMENTED_CARBIDE_MATERIALS.barrel,
                ModItems.CRYSTAL_MATERIALS.trigger to ModItems.CEMENTED_CARBIDE_MATERIALS.trigger,
                ModItems.CRYSTAL_MATERIALS.spring to ModItems.CEMENTED_CARBIDE_MATERIALS.spring
            ).forEach { (cry, cem) ->
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, cry.get())
                    .pattern(" C ")
                    .pattern("ABA")
                    .pattern(" C ")
                    .define('A', Tags.Items.GEMS_AMETHYST)
                    .define('B', cem.get())
                    .define('C', Tags.Items.GEMS_DIAMOND)
                    .unlockedBy(getHasName(cem.get()), has(cem.get()))
                    .unlockedBy(getHasName(Items.AMETHYST_SHARD), has(Tags.Items.GEMS_AMETHYST))
                    .save(writer, loc(getItemName(cry.get())))
            }

            generateMaterialPackRecipe(writer, ModItems.IRON_MATERIALS, ModItems.COMMON_MATERIAL_PACK.get())
            generateMaterialPackRecipe(writer, ModItems.STEEL_MATERIALS, ModItems.RARE_MATERIAL_PACK.get())
            generateMaterialPackRecipe(writer, ModItems.CEMENTED_CARBIDE_MATERIALS, ModItems.EPIC_MATERIAL_PACK.get())
            generateMaterialPackRecipe(writer, ModItems.NETHERITE_MATERIALS, ModItems.LEGENDARY_MATERIAL_PACK.get())
            generateMaterialPackRecipe(writer, ModItems.CRYSTAL_MATERIALS, ModItems.VIRTUAL_MATERIAL_PACK.get())
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SUPERB_MATERIAL_PACK.get())
                .pattern(" A ")
                .pattern("BEC")
                .pattern(" D ")
                .define('A', ModItems.COMMON_MATERIAL_PACK.get())
                .define('B', ModItems.RARE_MATERIAL_PACK.get())
                .define('C', ModItems.EPIC_MATERIAL_PACK.get())
                .define('D', ModItems.LEGENDARY_MATERIAL_PACK.get())
                .define('E', Items.NETHER_STAR)
                .unlockedBy(getHasName(Items.NETHER_STAR), has(Items.NETHER_STAR))
                .save(writer, loc(getItemName(ModItems.SUPERB_MATERIAL_PACK.get())))

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ANCIENT_CPU.get())
                .pattern("bcb")
                .pattern("cac")
                .pattern("bcb")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Tags.Items.GEMS_DIAMOND)
                .define('c', Tags.Items.ORES_NETHERITE_SCRAP)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, loc(getItemName(ModItems.ANCIENT_CPU.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.AP_HEAD.get(), 2)
                .pattern(" e ")
                .pattern("bdb")
                .pattern("cac")
                .define('a', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('b', Tags.Items.INGOTS_IRON)
                .define('c', ModTags.Items.INGOTS_STEEL)
                .define('d', ModItems.TUNGSTEN_ROD.get())
                .define('e', ModItems.FUSEE.get())
                .unlockedBy(
                    getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()),
                    has(ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                )
                .save(writer, loc(getItemName(ModItems.AP_HEAD.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BATTERY.get())
                .pattern(" b ")
                .pattern("cac")
                .pattern(" d ")
                .define('a', Tags.Items.DUSTS_REDSTONE)
                .define('b', PLATES_COPPER)
                .define('c', Tags.Items.GLASS_PANES)
                .define('d', Tags.Items.INGOTS_IRON)
                .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                .save(writer, loc(getItemName(ModItems.BATTERY.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BATTERY.get())
                .pattern("aa")
                .pattern("aa")
                .define('a', ModItems.CELL.get())
                .unlockedBy(getHasName(ModItems.CELL.get()), has(ModItems.CELL.get()))
                .save(writer, loc(getItemName(ModItems.BATTERY.get()) + "_from_cell"))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MEDIUM_AERIAL_BOMB.get())
                .pattern("aa")
                .pattern("aa")
                .define('a', ModItems.SMALL_AERIAL_BOMB.get())
                .unlockedBy(getHasName(ModItems.SMALL_AERIAL_BOMB.get()), has(ModItems.SMALL_AERIAL_BOMB.get()))
                .save(writer, loc(getItemName(ModItems.MEDIUM_AERIAL_BOMB.get()) + "_from_small_aerial_bomb"))
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.SMALL_AERIAL_BOMB.get(), 4)
                .requires(ModItems.MEDIUM_AERIAL_BOMB.get())
                .unlockedBy(
                    getHasName(ModItems.MEDIUM_AERIAL_BOMB.get()),
                    has(ModItems.MEDIUM_AERIAL_BOMB.get())
                )
                .save(writer, loc(getItemName(ModItems.SMALL_AERIAL_BOMB.get()) + "_from_medium_aerial_bomb"))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CANNON_CORE.get())
                .pattern("aaa")
                .pattern("bcd")
                .pattern("aaa")
                .define('a', ModTags.Items.INGOTS_STEEL)
                .define('b', Items.DISPENSER)
                .define('c', ModItems.CEMENTED_CARBIDE_MATERIALS.action.get())
                .define('d', Items.PISTON)
                .unlockedBy(
                    getHasName(ModItems.CEMENTED_CARBIDE_MATERIALS.action.get()),
                    has(ModItems.CEMENTED_CARBIDE_MATERIALS.action.get())
                )
                .save(writer, loc(getItemName(ModItems.CANNON_CORE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CELL.get())
                .pattern("a")
                .pattern("b")
                .pattern("c")
                .define('a', Tags.Items.NUGGETS_GOLD)
                .define('b', Tags.Items.DUSTS_REDSTONE)
                .define('c', Tags.Items.NUGGETS_IRON)
                .unlockedBy(getHasName(Items.GOLD_NUGGET), has(Items.GOLD_NUGGET))
                .save(writer, loc(getItemName(ModItems.CELL.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.LASER_UNIT.get())
                .pattern("eae")
                .pattern("dbd")
                .pattern("dcd")
                .define('a', Items.AMETHYST_SHARD)
                .define('b', Items.DIAMOND)
                .define('c', Items.REDSTONE)
                .define('d', ModTags.Items.INGOTS_STEEL)
                .define('e', Items.COPPER_INGOT)
                .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                .save(writer, loc(getItemName(ModItems.LASER_UNIT.get())))
            SimpleCookingRecipeBuilder.generic(
                Ingredient.of(ModItems.RAW_CEMENTED_CARBIDE_POWDER.get()),
                RecipeCategory.MISC,
                ModItems.CEMENTED_CARBIDE_INGOT.get(),
                8f,
                200,
                RecipeSerializer.BLASTING_RECIPE
            ) { group, category, ingredient, result, experience, cookingTime ->
                BlastingRecipe(
                    group,
                    category,
                    ingredient,
                    result,
                    experience,
                    cookingTime
                )
            }
                .unlockedBy(
                    getHasName(ModItems.RAW_CEMENTED_CARBIDE_POWDER.get()),
                    has(ModItems.RAW_CEMENTED_CARBIDE_POWDER.get())
                )
                .save(writer, loc(getItemName(ModItems.CEMENTED_CARBIDE_INGOT.get()) + "_blasting"))
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CEMENTED_CARBIDE_INGOT.get(), 9)
                .requires(ModItems.CEMENTED_CARBIDE_BLOCK.get())
                .unlockedBy(
                    getHasName(ModItems.CEMENTED_CARBIDE_BLOCK.get()),
                    has(ModItems.CEMENTED_CARBIDE_BLOCK.get())
                )
                .save(writer, loc(getItemName(ModItems.CEMENTED_CARBIDE_INGOT.get()) + "_from_block"))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CM_HEAD.get(), 2)
                .pattern("ddd")
                .pattern("bdb")
                .pattern("cac")
                .define('a', Items.GUNPOWDER)
                .define('b', Tags.Items.INGOTS_IRON)
                .define('c', ModTags.Items.INGOTS_STEEL)
                .define('d', ModItems.GRENADE_40MM.get())
                .unlockedBy(getHasName(ModItems.GRENADE_40MM.get()), has(ModItems.GRENADE_40MM.get()))
                .save(writer, loc(getItemName(ModItems.CM_HEAD.get())))
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.COAL_IRON_POWDER.get())
                .requires(commonItemTag("dusts/iron"))
                .requires(commonItemTag("dusts/coal_coke"))
                .unlockedBy(getHasName(ModItems.IRON_POWDER.get()), has(ModItems.IRON_POWDER.get()))
                .unlockedBy(getHasName(ModItems.COAL_POWDER.get()), has(ModItems.COAL_POWDER.get()))
                .save(writer, loc(getItemName(ModItems.COAL_IRON_POWDER.get())))
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.COAL_POWDER.get())
                .requires(ItemTags.COALS)
                .requires(ModTags.Items.HAMMER)
                .unlockedBy(getHasName(ModItems.HAMMER.get()), has(ModTags.Items.HAMMER))
                .save(writer, loc(getItemName(ModItems.COAL_POWDER.get())))
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.IRON_POWDER.get())
                .requires(Tags.Items.INGOTS_IRON)
                .requires(ModTags.Items.HAMMER)
                .unlockedBy(getHasName(ModItems.HAMMER.get()), has(ModTags.Items.HAMMER))
                .save(writer, loc(getItemName(ModItems.IRON_POWDER.get())))
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.COPPER_PLATE.get())
                .requires(Tags.Items.INGOTS_COPPER)
                .requires(ModTags.Items.HAMMER)
                .unlockedBy(getHasName(ModItems.HAMMER.get()), has(ModTags.Items.HAMMER))
                .save(writer, loc(getItemName(ModItems.COPPER_PLATE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FUSEE.get(), 4)
                .pattern("a")
                .pattern("b")
                .pattern("c")
                .define('a', Items.STONE_BUTTON)
                .define('b', Tags.Items.DUSTS_REDSTONE)
                .define('c', Tags.Items.NUGGETS_IRON)
                .unlockedBy(getHasName(Items.STONE_BUTTON), has(Items.STONE_BUTTON))
                .save(writer, loc(getItemName(ModItems.FUSEE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GRAIN.get(), 8)
                .pattern("aba")
                .pattern("aba")
                .pattern(" c ")
                .define('a', PLATES_COPPER)
                .define('b', Items.GUNPOWDER)
                .define('c', ModItems.PRIMER.get())
                .unlockedBy(getHasName(ModItems.PRIMER.get()), has(ModItems.PRIMER.get()))
                .save(writer, loc(getItemName(ModItems.GRAIN.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GS_HEAD.get(), 2)
                .pattern("ddd")
                .pattern("bdb")
                .pattern("cac")
                .define('a', Items.GUNPOWDER)
                .define('b', Tags.Items.INGOTS_IRON)
                .define('c', ModTags.Items.INGOTS_STEEL)
                .define('d', INGOTS_LEAD)
                .unlockedBy(getHasName(Items.GUNPOWDER), has(Items.GUNPOWDER))
                .save(writer, loc(getItemName(ModItems.GS_HEAD.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.WP_HEAD.get(), 2)
                .pattern("ede")
                .pattern("bdb")
                .pattern("cac")
                .define('a', Items.GUNPOWDER)
                .define('b', Tags.Items.INGOTS_IRON)
                .define('c', ModTags.Items.INGOTS_STEEL)
                .define('d', Items.BONE_MEAL)
                .define('e', Items.BLAZE_POWDER)
                .unlockedBy(getHasName(Items.GUNPOWDER), has(Items.GUNPOWDER))
                .save(writer, loc(getItemName(ModItems.WP_HEAD.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.HE_HEAD.get(), 2)
                .pattern(" e ")
                .pattern("bab")
                .pattern("cac")
                .define('a', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .define('b', Tags.Items.INGOTS_IRON)
                .define('c', ModTags.Items.INGOTS_STEEL)
                .define('e', ModItems.FUSEE.get())
                .unlockedBy(
                    getHasName(ModItems.HIGH_ENERGY_EXPLOSIVES.get()),
                    has(ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                )
                .save(writer, loc(getItemName(ModItems.HE_HEAD.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.HEAVY_ARMAMENT_MODULE.get())
                .pattern("ddd")
                .pattern("abc")
                .pattern("ddd")
                .define('a', ModItems.CANNON_CORE.get())
                .define('b', ModItems.LEGENDARY_MATERIAL_PACK.get())
                .define('c', ModItems.MEDIUM_ARMAMENT_MODULE.get())
                .define('d', Tags.Items.INGOTS_NETHERITE)
                .unlockedBy(
                    getHasName(ModItems.MEDIUM_ARMAMENT_MODULE.get()),
                    has(ModItems.MEDIUM_ARMAMENT_MODULE.get())
                )
                .save(writer, loc(getItemName(ModItems.HEAVY_ARMAMENT_MODULE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.HIGH_ENERGY_EXPLOSIVES.get(), 4)
                .pattern("aba")
                .pattern("cac")
                .pattern("aba")
                .define('a', Items.GUNPOWDER)
                .define('b', Items.SUGAR)
                .define('c', Tags.Items.SANDS)
                .unlockedBy(getHasName(Items.GUNPOWDER), has(Items.GUNPOWDER))
                .save(writer, loc(getItemName(ModItems.HIGH_ENERGY_EXPLOSIVES.get())))
            SimpleCookingRecipeBuilder.generic(
                Ingredient.of(ModItems.IRON_POWDER.get()),
                RecipeCategory.MISC,
                Items.IRON_INGOT,
                0.7f,
                100,
                RecipeSerializer.BLASTING_RECIPE
            ) { group, category, ingredient, result, experience, cookingTime ->
                BlastingRecipe(
                    group,
                    category,
                    ingredient,
                    result,
                    experience,
                    cookingTime
                )
            }
                .unlockedBy(getHasName(ModItems.IRON_POWDER.get()), has(ModItems.IRON_POWDER.get()))
                .save(writer, loc(getItemName(Items.IRON_INGOT) + "_blasting_from_powder"))
            SimpleCookingRecipeBuilder.generic(
                Ingredient.of(ModItems.IRON_POWDER.get()),
                RecipeCategory.MISC,
                Items.IRON_INGOT,
                0.7f,
                200,
                RecipeSerializer.SMELTING_RECIPE
            ) { group, category, ingredient, result, experience, cookingTime ->
                SmeltingRecipe(
                    group,
                    category,
                    ingredient,
                    result,
                    experience,
                    cookingTime
                )
            }
                .unlockedBy(getHasName(ModItems.IRON_POWDER.get()), has(ModItems.IRON_POWDER.get()))
                .save(writer, loc(getItemName(Items.IRON_INGOT) + "_smelting_from_powder"))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.LARGE_BATTERY_PACK.get())
                .pattern("aa")
                .pattern("aa")
                .define('a', ModItems.MEDIUM_BATTERY_PACK.get())
                .unlockedBy(getHasName(ModItems.MEDIUM_BATTERY_PACK.get()), has(ModItems.MEDIUM_BATTERY_PACK.get()))
                .save(writer, loc(getItemName(ModItems.LARGE_BATTERY_PACK.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.LARGE_MOTOR.get())
                .pattern(" a ")
                .pattern("bcd")
                .pattern("bcd")
                .define('a', Tags.Items.INGOTS_IRON)
                .define('b', Tags.Items.STORAGE_BLOCKS_LAPIS)
                .define('c', Tags.Items.STORAGE_BLOCKS_COPPER)
                .define('d', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .unlockedBy(getHasName(Items.COPPER_BLOCK), has(Tags.Items.STORAGE_BLOCKS_COPPER))
                .save(writer, loc(getItemName(ModItems.LARGE_MOTOR.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.LARGE_PROPELLER.get())
                .pattern(" a ")
                .pattern("aba")
                .pattern(" a ")
                .define('a', Tags.Items.INGOTS_IRON)
                .define('b', ModTags.Items.INGOTS_CEMENTED_CARBIDE)
                .unlockedBy(
                    getHasName(ModItems.CEMENTED_CARBIDE_INGOT.get()),
                    has(ModItems.CEMENTED_CARBIDE_INGOT.get())
                )
                .save(writer, loc(getItemName(ModItems.LARGE_PROPELLER.get())))
            SimpleCookingRecipeBuilder.generic(
                Ingredient.of(ModItems.GALENA.get()),
                RecipeCategory.MISC,
                ModItems.LEAD_INGOT.get(),
                0.7f,
                100,
                RecipeSerializer.BLASTING_RECIPE
            ) { group, category, ingredient, result, experience, cookingTime ->
                BlastingRecipe(
                    group,
                    category,
                    ingredient,
                    result,
                    experience,
                    cookingTime
                )
            }
                .unlockedBy(getHasName(ModItems.GALENA.get()), has(ModItems.GALENA.get()))
                .save(writer, loc(getItemName(ModItems.LEAD_INGOT.get()) + "_blasting"))
            SimpleCookingRecipeBuilder.generic(
                Ingredient.of(ModItems.GALENA_ORE.get(), ModItems.DEEPSLATE_GALENA_ORE.get()),
                RecipeCategory.MISC,
                ModItems.LEAD_INGOT.get(),
                0.7f,
                100,
                RecipeSerializer.BLASTING_RECIPE
            ) { group, category, ingredient, result, experience, cookingTime ->
                BlastingRecipe(
                    group,
                    category,
                    ingredient,
                    result,
                    experience,
                    cookingTime
                )
            }
                .unlockedBy(getHasName(ModItems.GALENA_ORE.get()), has(commonItemTag("ores/lead")))
                .save(writer, loc(getItemName(Items.IRON_INGOT) + "_blasting_from_ore"))
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.LEAD_INGOT.get(), 9)
                .requires(ModItems.LEAD_BLOCK.get())
                .unlockedBy(getHasName(ModItems.LEAD_BLOCK.get()), has(ModItems.LEAD_BLOCK.get()))
                .save(writer, loc(getItemName(ModItems.LEAD_INGOT.get()) + "_from_block"))
            SimpleCookingRecipeBuilder.generic(
                Ingredient.of(ModItems.GALENA.get()),
                RecipeCategory.MISC,
                ModItems.LEAD_INGOT.get(),
                0.7f,
                200,
                RecipeSerializer.SMELTING_RECIPE
            ) { group, category, ingredient, result, experience, cookingTime ->
                SmeltingRecipe(
                    group,
                    category,
                    ingredient,
                    result,
                    experience,
                    cookingTime
                )
            }
                .unlockedBy(getHasName(ModItems.GALENA.get()), has(ModItems.GALENA.get()))
                .save(writer, loc(getItemName(ModItems.LEAD_INGOT.get()) + "_smelting"))
            SimpleCookingRecipeBuilder.generic(
                Ingredient.of(ModItems.GALENA_ORE.get(), ModItems.DEEPSLATE_GALENA_ORE.get()),
                RecipeCategory.MISC,
                ModItems.LEAD_INGOT.get(),
                0.7f,
                200,
                RecipeSerializer.SMELTING_RECIPE
            ) { group, category, ingredient, result, experience, cookingTime ->
                SmeltingRecipe(
                    group,
                    category,
                    ingredient,
                    result,
                    experience,
                    cookingTime
                )
            }
                .unlockedBy(getHasName(ModItems.GALENA_ORE.get()), has(commonItemTag("ores/lead")))
                .save(writer, loc(getItemName(ModItems.LEAD_INGOT.get()) + "_smelting_from_ore"))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.LIGHT_ARMAMENT_MODULE.get())
                .pattern("ddd")
                .pattern("abc")
                .pattern("ddd")
                .define('a', ModItems.STEEL_MATERIALS.barrel.get())
                .define('b', ModItems.RARE_MATERIAL_PACK.get())
                .define('c', Items.DISPENSER)
                .define('d', ModTags.Items.INGOTS_STEEL)
                .unlockedBy(getHasName(ModItems.RARE_MATERIAL_PACK.get()), has(ModItems.RARE_MATERIAL_PACK.get()))
                .save(writer, loc(getItemName(ModItems.LIGHT_ARMAMENT_MODULE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MEDIUM_ARMAMENT_MODULE.get())
                .pattern("ddd")
                .pattern("abc")
                .pattern("ddd")
                .define('a', ModItems.CEMENTED_CARBIDE_MATERIALS.barrel.get())
                .define('b', ModItems.EPIC_MATERIAL_PACK.get())
                .define('c', ModItems.LIGHT_ARMAMENT_MODULE.get())
                .define('d', ModTags.Items.INGOTS_CEMENTED_CARBIDE)
                .unlockedBy(getHasName(ModItems.EPIC_MATERIAL_PACK.get()), has(ModItems.EPIC_MATERIAL_PACK.get()))
                .save(writer, loc(getItemName(ModItems.MEDIUM_ARMAMENT_MODULE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MEDIUM_BATTERY_PACK.get())
                .pattern("aaa")
                .pattern("aaa")
                .pattern("aaa")
                .define('a', ModItems.SMALL_BATTERY_PACK.get())
                .unlockedBy(getHasName(ModItems.SMALL_BATTERY_PACK.get()), has(ModItems.SMALL_BATTERY_PACK.get()))
                .save(writer, loc(getItemName(ModItems.MEDIUM_BATTERY_PACK.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MISSILE_ENGINE.get(), 4)
                .pattern("aba")
                .pattern("cbc")
                .pattern(" d ")
                .define('a', Tags.Items.INGOTS_COPPER)
                .define('b', ModItems.GRAIN.get())
                .define('c', Tags.Items.INGOTS_IRON)
                .define('d', Items.FIREWORK_ROCKET)
                .unlockedBy(getHasName(ModItems.GRAIN.get()), has(ModItems.GRAIN.get()))
                .save(writer, loc(getItemName(ModItems.MISSILE_ENGINE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MORTAR_BARREL.get())
                .pattern("a a")
                .pattern("a a")
                .pattern("aba")
                .define('a', Tags.Items.INGOTS_IRON)
                .define('b', Tags.Items.DYES_GREEN)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Tags.Items.INGOTS_IRON))
                .save(writer, loc(getItemName(ModItems.MORTAR_BARREL.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MORTAR_BASE_PLATE.get())
                .pattern(" b ")
                .pattern("aaa")
                .define('a', Tags.Items.INGOTS_IRON)
                .define('b', Tags.Items.DYES_GREEN)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Tags.Items.INGOTS_IRON))
                .save(writer, loc(getItemName(ModItems.MORTAR_BASE_PLATE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MORTAR_BIPOD.get())
                .pattern(" a ")
                .pattern("bbb")
                .pattern("cdc")
                .define('a', Tags.Items.INGOTS_IRON)
                .define('b', Tags.Items.NUGGETS_IRON)
                .define('c', Items.IRON_BARS)
                .define('d', Tags.Items.DYES_GREEN)
                .unlockedBy(getHasName(Items.IRON_BARS), has(Items.IRON_BARS))
                .save(writer, loc(getItemName(ModItems.MORTAR_BIPOD.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MOTOR.get(), 2)
                .pattern(" a ")
                .pattern("bcd")
                .pattern("bcd")
                .define('a', Tags.Items.NUGGETS_IRON)
                .define('b', Tags.Items.GEMS_LAPIS)
                .define('c', Tags.Items.INGOTS_COPPER)
                .define('d', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy(getHasName(Items.COPPER_INGOT), has(Tags.Items.INGOTS_COPPER))
                .save(writer, loc(getItemName(ModItems.MOTOR.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PRIMER.get(), 4)
                .pattern("a")
                .pattern("b")
                .define('a', Items.FLINT)
                .define('b', PLATES_COPPER)
                .unlockedBy(getHasName(Items.FLINT), has(Items.FLINT))
                .save(writer, loc(getItemName(ModItems.PRIMER.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PROPELLER.get(), 2)
                .pattern(" a ")
                .pattern("aba")
                .pattern(" a ")
                .define('a', ItemTags.PLANKS)
                .define('b', Tags.Items.NUGGETS_IRON)
                .unlockedBy(getHasName(Items.OAK_PLANKS), has(ItemTags.PLANKS))
                .unlockedBy(getHasName(Items.IRON_NUGGET), has(Tags.Items.NUGGETS_IRON))
                .save(writer, loc(getItemName(ModItems.PROPELLER.get())))
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.RAW_CEMENTED_CARBIDE_POWDER.get(), 8)
                .requires(Ingredient.of(commonItemTag("dusts/tungsten")), 7)
                .requires(commonItemTag("dusts/iron"))
                .requires(commonItemTag("dusts/coal_coke"))
                .unlockedBy(getHasName(ModItems.TUNGSTEN_POWDER.get()), has(commonItemTag("dusts/tungsten")))
                .save(writer, loc(getItemName(ModItems.RAW_CEMENTED_CARBIDE_POWDER.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SEEKER.get(), 4)
                .pattern(" a ")
                .pattern("bcb")
                .pattern("ded")
                .define('a', Items.AMETHYST_SHARD)
                .define('b', Tags.Items.INGOTS_IRON)
                .define('c', Items.COMPASS)
                .define('d', Tags.Items.GEMS_QUARTZ)
                .define('e', Items.COMPARATOR)
                .unlockedBy(getHasName(Items.COMPASS), has(Items.COMPASS))
                .save(writer, loc(getItemName(ModItems.SEEKER.get())))
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.SHORTCUT_PACK.get())
                .requires(ModItems.EPIC_MATERIAL_PACK.get())
                .requires(Items.NETHER_STAR)
                .unlockedBy(getHasName(ModItems.EPIC_MATERIAL_PACK.get()), has(ModItems.EPIC_MATERIAL_PACK.get()))
                .unlockedBy(getHasName(Items.NETHER_STAR), has(Items.NETHER_STAR))
                .save(writer, loc(getItemName(ModItems.SHORTCUT_PACK.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.REPAIR_TOOL.get())
                .pattern(" aa")
                .pattern("bcd")
                .pattern("efg")
                .define('a', Items.IRON_INGOT)
                .define('b', ModItems.STEEL_MATERIALS.barrel.get())
                .define('c', Items.FLINT_AND_STEEL)
                .define('d', ModItems.MOTOR.get())
                .define('e', Items.LAVA_BUCKET)
                .define('f', ModItems.BATTERY.get())
                .define('g', ModItems.STEEL_MATERIALS.trigger.get())
                .unlockedBy(getHasName(Items.COMPASS), has(Items.COMPASS))
                .save(writer, loc(getItemName(ModItems.REPAIR_TOOL.get())))
            SimpleCookingRecipeBuilder.generic(
                Ingredient.of(ModItems.RAW_SILVER.get()),
                RecipeCategory.MISC,
                ModItems.SILVER_INGOT.get(),
                0.7f,
                100,
                RecipeSerializer.BLASTING_RECIPE
            ) { group, category, ingredient, result, experience, cookingTime ->
                BlastingRecipe(
                    group,
                    category,
                    ingredient,
                    result,
                    experience,
                    cookingTime
                )
            }
                .unlockedBy(getHasName(ModItems.RAW_SILVER.get()), has(ModItems.RAW_SILVER.get()))
                .save(writer, loc(getItemName(ModItems.SILVER_INGOT.get()) + "_blasting"))
            SimpleCookingRecipeBuilder.generic(
                Ingredient.of(ModItems.SILVER_ORE.get(), ModItems.DEEPSLATE_SILVER_ORE.get()),
                RecipeCategory.MISC,
                ModItems.SILVER_INGOT.get(),
                0.7f,
                100,
                RecipeSerializer.BLASTING_RECIPE
            ) { group, category, ingredient, result, experience, cookingTime ->
                BlastingRecipe(
                    group,
                    category,
                    ingredient,
                    result,
                    experience,
                    cookingTime
                )
            }
                .unlockedBy(getHasName(ModItems.SILVER_ORE.get()), has(commonItemTag("ores/silver")))
                .save(writer, loc(getItemName(ModItems.SILVER_INGOT.get()) + "_blasting_from_ore"))
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.SILVER_INGOT.get(), 9)
                .requires(ModItems.SILVER_BLOCK.get())
                .unlockedBy(getHasName(ModItems.SILVER_BLOCK.get()), has(ModItems.SILVER_BLOCK.get()))
                .save(writer, loc(getItemName(ModItems.SILVER_INGOT.get()) + "_from_block"))
            SimpleCookingRecipeBuilder.generic(
                Ingredient.of(ModItems.RAW_SILVER.get()),
                RecipeCategory.MISC,
                ModItems.SILVER_INGOT.get(),
                0.7f,
                200,
                RecipeSerializer.SMELTING_RECIPE
            ) { group, category, ingredient, result, experience, cookingTime ->
                SmeltingRecipe(
                    group,
                    category,
                    ingredient,
                    result,
                    experience,
                    cookingTime
                )
            }
                .unlockedBy(getHasName(ModItems.RAW_SILVER.get()), has(ModItems.RAW_SILVER.get()))
                .save(writer, loc(getItemName(ModItems.SILVER_INGOT.get()) + "_smelting"))
            SimpleCookingRecipeBuilder.generic(
                Ingredient.of(ModItems.SILVER_ORE.get(), ModItems.DEEPSLATE_SILVER_ORE.get()),
                RecipeCategory.MISC,
                ModItems.SILVER_INGOT.get(),
                0.7f,
                200,
                RecipeSerializer.SMELTING_RECIPE
            ) { group, category, ingredient, result, experience, cookingTime ->
                SmeltingRecipe(
                    group,
                    category,
                    ingredient,
                    result,
                    experience,
                    cookingTime
                )
            }
                .unlockedBy(getHasName(ModItems.GALENA_ORE.get()), has(commonItemTag("ores/silver")))
                .save(writer, loc(getItemName(ModItems.SILVER_INGOT.get()) + "_smelting_from_ore"))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SMALL_BATTERY_PACK.get())
                .pattern("aa")
                .pattern("aa")
                .define('a', ModItems.BATTERY.get())
                .unlockedBy(getHasName(ModItems.BATTERY.get()), has(ModItems.BATTERY.get()))
                .save(writer, loc(getItemName(ModItems.SMALL_BATTERY_PACK.get()) + "_from_battery"))
            SimpleCookingRecipeBuilder.generic(
                Ingredient.of(ModItems.COAL_IRON_POWDER.get()),
                RecipeCategory.MISC,
                ModItems.STEEL_INGOT.get(),
                0.7f,
                100,
                RecipeSerializer.BLASTING_RECIPE
            ) { group, category, ingredient, result, experience, cookingTime ->
                BlastingRecipe(
                    group,
                    category,
                    ingredient,
                    result,
                    experience,
                    cookingTime
                )
            }
                .unlockedBy(getHasName(ModItems.COAL_IRON_POWDER.get()), has(ModItems.COAL_IRON_POWDER.get()))
                .save(writer, loc(getItemName(ModItems.STEEL_INGOT.get()) + "_blasting"))
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.STEEL_INGOT.get(), 9)
                .requires(ModItems.STEEL_BLOCK.get())
                .unlockedBy(getHasName(ModItems.STEEL_BLOCK.get()), has(ModItems.STEEL_BLOCK.get()))
                .save(writer, loc(getItemName(ModItems.STEEL_INGOT.get()) + "_from_block"))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TRACK.get())
                .pattern("aaa")
                .pattern("b b")
                .pattern("aaa")
                .define('a', ModTags.Items.INGOTS_STEEL)
                .define('b', ModItems.WHEEL.get())
                .unlockedBy(getHasName(ModItems.WHEEL.get()), has(ModItems.WHEEL.get()))
                .save(writer, loc(getItemName(ModItems.TRACK.get())))
            SimpleCookingRecipeBuilder.generic(
                Ingredient.of(ModItems.SCHEELITE.get()),
                RecipeCategory.MISC,
                ModItems.TUNGSTEN_INGOT.get(),
                4f,
                100,
                RecipeSerializer.BLASTING_RECIPE
            ) { group, category, ingredient, result, experience, cookingTime ->
                BlastingRecipe(
                    group,
                    category,
                    ingredient,
                    result,
                    experience,
                    cookingTime
                )
            }
                .unlockedBy(getHasName(ModItems.SCHEELITE.get()), has(ModItems.SCHEELITE.get()))
                .save(writer, loc(getItemName(ModItems.TUNGSTEN_INGOT.get()) + "_blasting"))
            SimpleCookingRecipeBuilder.generic(
                Ingredient.of(ModItems.SCHEELITE_ORE.get(), ModItems.DEEPSLATE_SCHEELITE_ORE.get()),
                RecipeCategory.MISC,
                ModItems.TUNGSTEN_INGOT.get(),
                4f,
                100,
                RecipeSerializer.BLASTING_RECIPE
            ) { group, category, ingredient, result, experience, cookingTime ->
                BlastingRecipe(
                    group,
                    category,
                    ingredient,
                    result,
                    experience,
                    cookingTime
                )
            }
                .unlockedBy(getHasName(ModItems.SCHEELITE_ORE.get()), has(commonItemTag("ores/tungsten")))
                .save(writer, loc(getItemName(ModItems.TUNGSTEN_INGOT.get()) + "_blasting_from_ore"))
            SimpleCookingRecipeBuilder.generic(
                Ingredient.of(ModItems.TUNGSTEN_POWDER.get()),
                RecipeCategory.MISC,
                ModItems.TUNGSTEN_INGOT.get(),
                4f,
                100,
                RecipeSerializer.BLASTING_RECIPE
            ) { group, category, ingredient, result, experience, cookingTime ->
                BlastingRecipe(
                    group,
                    category,
                    ingredient,
                    result,
                    experience,
                    cookingTime
                )
            }
                .unlockedBy(getHasName(ModItems.TUNGSTEN_POWDER.get()), has(ModItems.TUNGSTEN_POWDER.get()))
                .save(writer, loc(getItemName(ModItems.TUNGSTEN_INGOT.get()) + "_blasting_from_powder"))
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.TUNGSTEN_INGOT.get(), 9)
                .requires(ModItems.TUNGSTEN_BLOCK.get())
                .unlockedBy(getHasName(ModItems.TUNGSTEN_BLOCK.get()), has(ModItems.TUNGSTEN_BLOCK.get()))
                .save(writer, loc(getItemName(ModItems.TUNGSTEN_INGOT.get()) + "_from_block"))
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.TUNGSTEN_POWDER.get())
                .requires(INGOTS_TUNGSTEN)
                .requires(ModTags.Items.HAMMER)
                .unlockedBy(getHasName(ModItems.HAMMER.get()), has(ModTags.Items.HAMMER))
                .save(writer, loc(getItemName(ModItems.TUNGSTEN_POWDER.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TUNGSTEN_ROD.get(), 4)
                .pattern("a")
                .pattern("a")
                .define('a', INGOTS_TUNGSTEN)
                .unlockedBy(getHasName(ModItems.TUNGSTEN_INGOT.get()), has(INGOTS_TUNGSTEN))
                .save(writer, loc(getItemName(ModItems.TUNGSTEN_ROD.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.WHEEL.get(), 2)
                .pattern(" a ")
                .pattern("aba")
                .pattern(" a ")
                .define('a', Items.BLACK_WOOL)
                .define('b', Tags.Items.INGOTS_IRON)
                .unlockedBy(getHasName(Items.BLACK_WOOL), has(Items.BLACK_WOOL))
                .save(writer, loc(getItemName(ModItems.WHEEL.get())))
        }

        private fun buildBlockRecipes(writer: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.AIRCRAFT_CATAPULT.get(), 8)
                .pattern("aaa")
                .pattern("cbc")
                .pattern("ddd")
                .define('a', Items.POWERED_RAIL)
                .define('b', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .define('c', Tags.Items.INGOTS_COPPER)
                .define('d', Tags.Items.INGOTS_IRON)
                .unlockedBy(getHasName(Items.POWERED_RAIL), has(Items.POWERED_RAIL))
                .save(writer, loc(getItemName(ModItems.AIRCRAFT_CATAPULT.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.SUPERB_ITEM_INTERFACE.get())
                .pattern("cac")
                .pattern("aba")
                .pattern("cac")
                .define('a', Items.HOPPER)
                .define('b', Items.DROPPER)
                .define('c', ModTags.Items.INGOTS_STEEL)
                .unlockedBy(getHasName(Items.HOPPER), has(Items.DROPPER))
                .save(writer, loc(getItemName(ModItems.SUPERB_ITEM_INTERFACE.get())))
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
                .save(writer, loc(getItemName(ModItems.VEHICLE_ASSEMBLING_TABLE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.BARBED_WIRE.get(), 2)
                .pattern("aba")
                .define('a', ItemTags.PLANKS)
                .define('b', Items.IRON_BARS)
                .unlockedBy(getHasName(Items.IRON_BARS), has(Items.IRON_BARS))
                .save(writer, loc(getItemName(ModItems.BARBED_WIRE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.CEMENTED_CARBIDE_BLOCK.get())
                .pattern("aaa")
                .pattern("aaa")
                .pattern("aaa")
                .define('a', ModItems.CEMENTED_CARBIDE_INGOT.get())
                .unlockedBy(
                    getHasName(ModItems.CEMENTED_CARBIDE_INGOT.get()),
                    has(ModItems.CEMENTED_CARBIDE_INGOT.get())
                )
                .save(writer, loc(getItemName(ModItems.CEMENTED_CARBIDE_BLOCK.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.CHARGING_STATION.get())
                .pattern("ada")
                .pattern("dcd")
                .pattern("aba")
                .define('a', PLATES_COPPER)
                .define('b', Tags.Items.INGOTS_IRON)
                .define('c', Items.BLAST_FURNACE)
                .define('d', ModItems.CELL.get())
                .unlockedBy(getHasName(ModItems.CELL.get()), has(ModItems.CELL.get()))
                .save(writer, loc(getItemName(ModItems.CHARGING_STATION.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.DRAGON_TEETH.get(), 4)
                .pattern(" a ")
                .pattern("bbb")
                .pattern("bbb")
                .define('a', Tags.Items.NUGGETS_IRON)
                .define('b', Items.SMOOTH_STONE)
                .unlockedBy(getHasName(Items.SMOOTH_STONE), has(Items.SMOOTH_STONE))
                .save(writer, loc(getItemName(ModItems.DRAGON_TEETH.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.JUMP_PAD.get())
                .pattern(" a ")
                .pattern("bcb")
                .pattern("bcb")
                .define('a', Items.STONE_PRESSURE_PLATE)
                .define('b', Items.LIME_CONCRETE)
                .define('c', Items.PISTON)
                .unlockedBy(getHasName(Items.PISTON), has(Items.PISTON))
                .save(writer, loc(getItemName(ModItems.JUMP_PAD.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.LEAD_BLOCK.get())
                .pattern("aaa")
                .pattern("aba")
                .pattern("aaa")
                .define('a', INGOTS_LEAD)
                .define('b', ModItems.LEAD_INGOT.get())
                .unlockedBy(getHasName(ModItems.LEAD_INGOT.get()), has(ModItems.LEAD_INGOT.get()))
                .save(writer, loc(getItemName(ModItems.LEAD_BLOCK.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.REFORGING_TABLE.get())
                .pattern("abc")
                .pattern("ded")
                .pattern("ddd")
                .define('a', Tags.Items.INGOTS_GOLD)
                .define('b', Tags.Items.GEMS_DIAMOND)
                .define('c', Tags.Items.DUSTS_REDSTONE)
                .define('d', Items.POLISHED_BASALT)
                .define('e', ModItems.ANCIENT_CPU.get())
                .unlockedBy(getHasName(ModItems.ANCIENT_CPU.get()), has(ModItems.ANCIENT_CPU.get()))
                .save(writer, loc(getItemName(ModItems.REFORGING_TABLE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.SANDBAG.get())
                .pattern("aba")
                .define('a', Items.PAPER)
                .define('b', Tags.Items.SANDS)
                .unlockedBy(getHasName(Items.PAPER), has(Items.PAPER))
                .save(writer, loc(getItemName(ModItems.SANDBAG.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.SILVER_BLOCK.get())
                .pattern("aaa")
                .pattern("aba")
                .pattern("aaa")
                .define('a', INGOTS_SILVER)
                .define('b', ModItems.SILVER_INGOT.get())
                .unlockedBy(getHasName(ModItems.SILVER_INGOT.get()), has(ModItems.SILVER_INGOT.get()))
                .save(writer, loc(getItemName(ModItems.SILVER_BLOCK.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.STEEL_BLOCK.get())
                .pattern("aaa")
                .pattern("aaa")
                .pattern("aaa")
                .define('a', ModTags.Items.INGOTS_STEEL)
                .unlockedBy(getHasName(ModItems.STEEL_INGOT.get()), has(ModTags.Items.INGOTS_STEEL))
                .save(writer, loc(getItemName(ModItems.STEEL_BLOCK.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.TUNGSTEN_BLOCK.get())
                .pattern("aaa")
                .pattern("aba")
                .pattern("aaa")
                .define('a', INGOTS_TUNGSTEN)
                .define('b', ModItems.TUNGSTEN_INGOT.get())
                .unlockedBy(getHasName(ModItems.TUNGSTEN_INGOT.get()), has(ModItems.TUNGSTEN_INGOT.get()))
                .save(writer, loc(getItemName(ModItems.TUNGSTEN_BLOCK.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.FUMO_25.get())
                .pattern("ada")
                .pattern(" c ")
                .pattern("beb")
                .define('a', Items.IRON_BARS)
                .define('b', Tags.Items.INGOTS_IRON)
                .define('c', ModItems.MOTOR.get())
                .define('d', Items.OBSERVER)
                .define('e', ModItems.CELL.get())
                .unlockedBy(getHasName(ModItems.MOTOR.get()), has(ModItems.MOTOR.get()))
                .save(writer, loc(getItemName(ModItems.FUMO_25.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.BIOGAS_GENERATOR.get())
                .pattern("aba")
                .pattern("cdc")
                .pattern("efe")
                .define('a', ModTags.Items.INGOTS_STEEL)
                .define('b', Items.HOPPER)
                .define('c', Items.TINTED_GLASS)
                .define('d', Items.CAULDRON)
                .define('e', ModItems.CELL.get())
                .define('f', Items.BLAST_FURNACE)
                .unlockedBy(getHasName(ModItems.CELL.get()), has(ModItems.CELL.get()))
                .save(writer, loc(getItemName(ModItems.BIOGAS_GENERATOR.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.BLUEPRINT_RESEARCH_TABLE.get())
                .pattern("aaa")
                .pattern("dcb")
                .pattern("eee")
                .define('a', Items.IRON_INGOT)
                .define('b', ModItems.BATTERY.get())
                .define('c', Items.CARTOGRAPHY_TABLE)
                .define('d', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .define('e', ModTags.Items.INGOTS_STEEL)
                .unlockedBy(getHasName(Items.CARTOGRAPHY_TABLE), has(Items.CARTOGRAPHY_TABLE))
                .save(writer, loc(getItemName(ModItems.BLUEPRINT_RESEARCH_TABLE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.RAW_GALENA_BLOCK.get())
                .pattern("aaa")
                .pattern("aaa")
                .pattern("aaa")
                .define('a', commonItemTag("raw_materials/lead"))
                .unlockedBy(getHasName(ModItems.GALENA.get()), has(commonItemTag("raw_materials/lead")))
                .save(writer, loc(getItemName(ModItems.RAW_GALENA_BLOCK.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.RAW_SCHEELITE_BLOCK.get())
                .pattern("aaa")
                .pattern("aaa")
                .pattern("aaa")
                .define('a', Ingredient.of(commonItemTag("raw_materials/tungsten")))
                .unlockedBy(getHasName(ModItems.SCHEELITE.get()), has(commonItemTag("raw_materials/tungsten")))
                .save(writer, loc(getItemName(ModItems.RAW_SCHEELITE_BLOCK.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.RAW_SILVER_BLOCK.get())
                .pattern("aaa")
                .pattern("aaa")
                .pattern("aaa")
                .define('a', commonItemTag("raw_materials/silver"))
                .unlockedBy(getHasName(ModItems.RAW_SILVER.get()), has(commonItemTag("raw_materials/silver")))
                .save(writer, loc(getItemName(ModItems.RAW_SILVER_BLOCK.get())))
        }

        private fun buildVehicleRecipes(writer: RecipeOutput) {
            VehicleAssemblingRecipeBuilder.entity(ModEntities.TOM_6.get(), VehicleAssemblingRecipe.Category.AIRCRAFT)
                .require(ItemTags.PLANKS, 5)
                .require(ModItems.BATTERY.get())
                .require(Items.MINECART)
                .unlockedBy(getHasName(Items.MINECART), has(Items.MINECART))
                .save(writer, loc(getEntityTypeName(ModEntities.TOM_6.get())))
            VehicleAssemblingRecipeBuilder.entity(
                ModEntities.ANNIHILATOR.get(),
                VehicleAssemblingRecipe.Category.DEFENSE
            )
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 24)
                .require(Items.NETHERITE_BLOCK, 3)
                .require(ModItems.LASER_UNIT.get(), 32)
                .require(ModItems.LARGE_BATTERY_PACK.get())
                .require(ModItems.ANNIHILATOR_BLUEPRINT.get())
                .unlockedBy(getHasName(ModItems.ANNIHILATOR_BLUEPRINT.get()), has(ModItems.ANNIHILATOR_BLUEPRINT.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.ANNIHILATOR.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.BL_132.get(), VehicleAssemblingRecipe.Category.DEFENSE)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 10)
                .require(ModItems.BL_132_BLUEPRINT.get())
                .require(ModItems.CANNON_CORE.get(), 4)
                .unlockedBy(getHasName(ModItems.BL_132_BLUEPRINT.get()), has(ModItems.BL_132_BLUEPRINT.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.BL_132.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.MLE_1934.get(), VehicleAssemblingRecipe.Category.DEFENSE)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 8)
                .require(ModItems.MLE_1934_BLUEPRINT.get())
                .require(ModItems.CANNON_CORE.get(), 2)
                .unlockedBy(getHasName(ModItems.MLE_1934_BLUEPRINT.get()), has(ModItems.MLE_1934_BLUEPRINT.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.MLE_1934.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.MK_42.get(), VehicleAssemblingRecipe.Category.DEFENSE)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 6)
                .require(ModItems.MK_42_BLUEPRINT.get())
                .require(ModItems.CANNON_CORE.get())
                .unlockedBy(getHasName(ModItems.MK_42_BLUEPRINT.get()), has(ModItems.MK_42_BLUEPRINT.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.MK_42.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.TYPE_63.get(), VehicleAssemblingRecipe.Category.DEFENSE)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 1)
                .require(ModItems.MORTAR_BARREL.get(), 12)
                .require(ModItems.WHEEL.get(), 2)
                .unlockedBy(getHasName(ModItems.MORTAR_BARREL.get()), has(ModItems.MORTAR_BARREL.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.TYPE_63.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.HPJ_11.get(), VehicleAssemblingRecipe.Category.DEFENSE)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 5)
                .require(ModItems.HPJ_11_BLUEPRINT.get())
                .require(ModItems.CANNON_CORE.get())
                .require(ModItems.MEDIUM_BATTERY_PACK.get())
                .require(ModItems.LARGE_MOTOR.get())
                .require(Items.OBSERVER)
                .unlockedBy(getHasName(ModItems.HPJ_11_BLUEPRINT.get()), has(ModItems.HPJ_11_BLUEPRINT.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.HPJ_11.get())))
            VehicleAssemblingRecipeBuilder.entity(
                ModEntities.LASER_TOWER.get(),
                VehicleAssemblingRecipe.Category.DEFENSE
            )
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 1)
                .require(ModItems.LASER_UNIT.get())
                .require(ModItems.SMALL_BATTERY_PACK.get())
                .require(ModItems.MOTOR.get())
                .unlockedBy(getHasName(ModItems.LASER_UNIT.get()), has(ModItems.LASER_UNIT.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.LASER_TOWER.get())))
            VehicleAssemblingRecipeBuilder.entity(
                ModEntities.WAVEFORCE_TOWER.get(),
                VehicleAssemblingRecipe.Category.DEFENSE
            )
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 10)
                .require(ModItems.CEMENTED_CARBIDE_BLOCK.get(), 2)
                .require(Items.REDSTONE_BLOCK, 8)
                .require(ModItems.LASER_UNIT.get(), 9)
                .require(ModItems.MEDIUM_BATTERY_PACK.get(), 2)
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LASER_UNIT.get()), has(ModItems.LASER_UNIT.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.WAVEFORCE_TOWER.get())))
            VehicleAssemblingRecipeBuilder.entity(
                ModEntities.WHEEL_CHAIR.get(),
                VehicleAssemblingRecipe.Category.CIVILIAN
            )
                .require(ModItems.WHEEL.get(), 2)
                .require(ModItems.CELL.get())
                .require(ModItems.MOTOR.get())
                .require(Items.MINECART)
                .unlockedBy(getHasName(Items.MINECART), has(Items.MINECART))
                .save(writer, loc(getEntityTypeName(ModEntities.WHEEL_CHAIR.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.LAV_150.get(), VehicleAssemblingRecipe.Category.LAND)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 6)
                .require(ModItems.LIGHT_ARMAMENT_MODULE.get())
                .require(ModItems.MEDIUM_BATTERY_PACK.get())
                .require(ModItems.WHEEL.get(), 4)
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.LAV_150.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.LAV_AD.get(), VehicleAssemblingRecipe.Category.LAND)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 7)
                .require(ModItems.MEDIUM_ARMAMENT_MODULE.get())
                .require(ModItems.MEDIUM_BATTERY_PACK.get())
                .require(ModItems.WHEEL.get(), 8)
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.LAV_AD.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.LAV_25.get(), VehicleAssemblingRecipe.Category.LAND)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 7)
                .require(ModItems.MEDIUM_ARMAMENT_MODULE.get())
                .require(ModItems.MEDIUM_BATTERY_PACK.get())
                .require(ModItems.WHEEL.get(), 8)
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.LAV_25.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.BMP_2.get(), VehicleAssemblingRecipe.Category.LAND)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 8)
                .require(ModItems.MEDIUM_ARMAMENT_MODULE.get())
                .require(ModItems.MEDIUM_BATTERY_PACK.get())
                .require(ModItems.TRACK.get(), 2)
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.BMP_2.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.BRADLEY.get(), VehicleAssemblingRecipe.Category.LAND)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 8)
                .require(ModItems.MEDIUM_ARMAMENT_MODULE.get())
                .require(ModItems.MEDIUM_BATTERY_PACK.get())
                .require(ModItems.TRACK.get(), 2)
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.BRADLEY.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.PRISM_TANK.get(), VehicleAssemblingRecipe.Category.LAND)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 9)
                .require(ModItems.LASER_UNIT.get(), 16)
                .require(ModItems.LARGE_BATTERY_PACK.get())
                .require(ModItems.TRACK.get(), 2)
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.PRISM_TANK.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.T_90A.get(), VehicleAssemblingRecipe.Category.LAND)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 10)
                .require(ModItems.HEAVY_ARMAMENT_MODULE.get())
                .require(ModItems.MEDIUM_BATTERY_PACK.get(), 2)
                .require(ModItems.TRACK.get(), 2)
                .require(ModItems.LARGE_MOTOR.get())
                .require(Items.GREEN_DYE)
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.T_90A.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.ZTZ_99A.get(), VehicleAssemblingRecipe.Category.LAND)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 10)
                .require(ModItems.HEAVY_ARMAMENT_MODULE.get())
                .require(ModItems.MEDIUM_BATTERY_PACK.get(), 2)
                .require(ModItems.TRACK.get(), 2)
                .require(ModItems.LARGE_MOTOR.get())
                .require(Items.RED_DYE)
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.ZTZ_99A.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.M_1A_2.get(), VehicleAssemblingRecipe.Category.LAND)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 10)
                .require(ModItems.HEAVY_ARMAMENT_MODULE.get())
                .require(ModItems.MEDIUM_BATTERY_PACK.get(), 2)
                .require(ModItems.TRACK.get(), 2)
                .require(ModItems.LARGE_MOTOR.get())
                .require(Items.SAND)
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.M_1A_2.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.YX_100.get(), VehicleAssemblingRecipe.Category.LAND)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 8)
                .require(ModItems.CEMENTED_CARBIDE_BLOCK.get(), 24)
                .require(ModItems.HEAVY_ARMAMENT_MODULE.get())
                .require(ModItems.MEDIUM_ARMAMENT_MODULE.get())
                .require(ModItems.LARGE_BATTERY_PACK.get())
                .require(ModItems.TRACK.get(), 2)
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.YX_100.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.PLZ_05.get(), VehicleAssemblingRecipe.Category.LAND)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 10)
                .require(ModItems.CANNON_CORE.get(), 1)
                .require(ModItems.HEAVY_ARMAMENT_MODULE.get())
                .require(ModItems.MEDIUM_BATTERY_PACK.get())
                .require(ModItems.TRACK.get(), 2)
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.PLZ_05.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.SPEEDBOAT.get(), VehicleAssemblingRecipe.Category.WATER)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 2)
                .require(ItemTags.BOATS)
                .require(ModItems.LIGHT_ARMAMENT_MODULE.get())
                .require(ModItems.SMALL_BATTERY_PACK.get())
                .require(ModItems.LARGE_PROPELLER.get())
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LIGHT_ARMAMENT_MODULE.get()), has(ModItems.LIGHT_ARMAMENT_MODULE.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.SPEEDBOAT.get())))
            VehicleAssemblingRecipeBuilder.entity(
                ModEntities.TINY_SPEEDBOAT.get(),
                VehicleAssemblingRecipe.Category.WATER
            )
                .require(Items.IRON_INGOT, 5)
                .require(ItemTags.BOATS)
                .require(ModItems.BATTERY.get())
                .require(ModItems.PROPELLER.get())
                .require(ModItems.MOTOR.get())
                .unlockedBy(getHasName(ModItems.MOTOR.get()), has(ModItems.MOTOR.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.TINY_SPEEDBOAT.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.AH_6.get(), VehicleAssemblingRecipe.Category.AIRCRAFT)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 3)
                .require(ModItems.LIGHT_ARMAMENT_MODULE.get())
                .require(ModItems.MEDIUM_BATTERY_PACK.get())
                .require(ModItems.LARGE_PROPELLER.get())
                .require(ModItems.PROPELLER.get())
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LARGE_PROPELLER.get()), has(ModItems.LARGE_PROPELLER.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.AH_6.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.KV_16.get(), VehicleAssemblingRecipe.Category.AIRCRAFT)
                .require(Items.BUCKET, 2)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 1)
                .require(ItemTags.PLANKS, 2)
                .require(ModItems.LIGHT_ARMAMENT_MODULE.get())
                .require(ModItems.SMALL_BATTERY_PACK.get())
                .require(ModItems.PROPELLER.get(), 1)
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LARGE_PROPELLER.get()), has(ModItems.LARGE_PROPELLER.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.KV_16.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.JU_87.get(), VehicleAssemblingRecipe.Category.AIRCRAFT)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 3)
                .require(ModItems.MEDIUM_ARMAMENT_MODULE.get(), 1)
                .require(Items.GOAT_HORN, 1)
                .require(ModItems.MEDIUM_BATTERY_PACK.get())
                .require(ModItems.LARGE_PROPELLER.get(), 1)
                .require(ModItems.PROPELLER.get(), 2)
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LARGE_PROPELLER.get()), has(ModItems.LARGE_PROPELLER.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.JU_87.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.A_10A.get(), VehicleAssemblingRecipe.Category.AIRCRAFT)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 6)
                .require(ModItems.HEAVY_ARMAMENT_MODULE.get())
                .require(ModItems.LARGE_BATTERY_PACK.get())
                .require(ModItems.LARGE_PROPELLER.get(), 2)
                .require(ModItems.LARGE_MOTOR.get(), 2)
                .require(ModItems.WHEEL.get(), 3)
                .unlockedBy(getHasName(ModItems.LARGE_PROPELLER.get()), has(ModItems.LARGE_PROPELLER.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.A_10A.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.TRUCK.get(), VehicleAssemblingRecipe.Category.CIVILIAN)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 8)
                .require(Items.CHEST, 4)
                .require(ModItems.MEDIUM_BATTERY_PACK.get())
                .require(ModItems.WHEEL.get(), 6)
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.TRUCK.get())))
            VehicleAssemblingRecipeBuilder.entity(
                ModEntities.SODAYO_PICK_UP.get(),
                VehicleAssemblingRecipe.Category.CIVILIAN
            )
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 2)
                .require(Items.CHEST, 1)
                .require(ModItems.MEDIUM_BATTERY_PACK.get())
                .require(ModItems.WHEEL.get(), 4)
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.SODAYO_PICK_UP.get())))
            VehicleAssemblingRecipeBuilder.entity(
                ModEntities.SODAYO_PICK_UP_HMG.get(),
                VehicleAssemblingRecipe.Category.CIVILIAN
            )
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 2)
                .require(ModItems.MEDIUM_BATTERY_PACK.get())
                .require(ModItems.WHEEL.get(), 4)
                .require(ModItems.LARGE_MOTOR.get())
                .require(ModItems.LIGHT_ARMAMENT_MODULE.get())
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.SODAYO_PICK_UP_HMG.get())))
            VehicleAssemblingRecipeBuilder.entity(
                ModEntities.SODAYO_PICK_UP_ROCKET.get(),
                VehicleAssemblingRecipe.Category.CIVILIAN
            )
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 3)
                .require(ModItems.MEDIUM_BATTERY_PACK.get())
                .require(ModItems.WHEEL.get(), 4)
                .require(ModItems.LARGE_MOTOR.get())
                .require(ModItems.MORTAR_BARREL.get(), 12)
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.SODAYO_PICK_UP_ROCKET.get())))
            VehicleAssemblingRecipeBuilder.entity(
                ModEntities.SODAYO_PICK_UP_TOW.get(),
                VehicleAssemblingRecipe.Category.CIVILIAN
            )
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 3)
                .require(ModItems.MEDIUM_BATTERY_PACK.get())
                .require(ModItems.WHEEL.get(), 4)
                .require(ModItems.LARGE_MOTOR.get())
                .require(ModItems.TOW_DEPLOYER.get())
                .unlockedBy(getHasName(ModItems.LARGE_MOTOR.get()), has(ModItems.LARGE_MOTOR.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.SODAYO_PICK_UP_TOW.get())))
            VehicleAssemblingRecipeBuilder.entity(ModEntities.MI_28.get(), VehicleAssemblingRecipe.Category.AIRCRAFT)
                .require(ModTags.Items.STORAGE_BLOCK_STEEL, 5)
                .require(ModItems.HEAVY_ARMAMENT_MODULE.get())
                .require(ModItems.MEDIUM_BATTERY_PACK.get(), 2)
                .require(ModItems.WHEEL.get(), 3)
                .require(ModItems.LARGE_PROPELLER.get())
                .require(ModItems.PROPELLER.get())
                .require(ModItems.LARGE_MOTOR.get())
                .unlockedBy(getHasName(ModItems.HEAVY_ARMAMENT_MODULE.get()), has(ModItems.HEAVY_ARMAMENT_MODULE.get()))
                .save(writer, loc(getEntityTypeName(ModEntities.MI_28.get())))

            VehicleAssemblingRecipeBuilder.item(
                ModItems.SMALL_BATTERY_PACK.get(),
                1,
                VehicleAssemblingRecipe.Category.MISC
            )
                .require(PLATES_COPPER, 4)
                .require(Tags.Items.GLASS_PANES, 8)
                .require(Items.REDSTONE, 4)
                .require(Items.IRON_INGOT, 4)
                .unlockedBy(getHasName(ModItems.COPPER_PLATE.get()), has(ModItems.COPPER_PLATE.get()))
                .save(writer, loc(getItemName(ModItems.SMALL_BATTERY_PACK.get()) + "_assembling"))
            VehicleAssemblingRecipeBuilder.item(
                ModItems.MEDIUM_BATTERY_PACK.get(),
                1,
                VehicleAssemblingRecipe.Category.MISC
            )
                .require(PLATES_COPPER, 36)
                .require(Tags.Items.GLASS_PANES, 72)
                .require(Items.REDSTONE, 36)
                .require(Items.IRON_INGOT, 36)
                .unlockedBy(getHasName(ModItems.COPPER_PLATE.get()), has(ModItems.COPPER_PLATE.get()))
                .save(writer, loc(getItemName(ModItems.MEDIUM_BATTERY_PACK.get()) + "_assembling"))
            VehicleAssemblingRecipeBuilder.item(
                ModItems.LARGE_BATTERY_PACK.get(),
                1,
                VehicleAssemblingRecipe.Category.MISC
            )
                .require(PLATES_COPPER, 144)
                .require(Tags.Items.GLASS_PANES, 288)
                .require(Items.REDSTONE, 144)
                .require(Items.IRON_INGOT, 144)
                .unlockedBy(getHasName(ModItems.COPPER_PLATE.get()), has(ModItems.COPPER_PLATE.get()))
                .save(writer, loc(getItemName(ModItems.LARGE_BATTERY_PACK.get()) + "_assembling"))
            VehicleAssemblingRecipeBuilder.item(
                ModItems.VEHICLE_RESET_KIT.get(),
                1,
                VehicleAssemblingRecipe.Category.MISC
            )
                .require(ModTags.Items.INGOTS_STEEL)
                .require(Items.PAPER, 4)
                .unlockedBy(getHasName(Items.PAPER), has(Items.PAPER))
                .save(writer, loc(getItemName(ModItems.VEHICLE_RESET_KIT.get()) + "_assembling"))
        }

        private fun buildGunRecipes(writer: RecipeOutput) {
            gunSmithing(
                writer,
                ModItems.TRACHELIUM_BLUEPRINT.get(),
                GunRarity.VIRTUAL,
                ModTags.Items.INGOTS_CEMENTED_CARBIDE,
                ModItems.TRACHELIUM.get()
            )
            gunSmithing(
                writer,
                ModItems.GLOCK_17_BLUEPRINT.get(),
                GunRarity.COMMON,
                Items.IRON_INGOT,
                ModItems.GLOCK_17.get()
            )
            gunSmithing(
                writer,
                ModItems.MP_443_BLUEPRINT.get(),
                GunRarity.COMMON,
                Items.IRON_INGOT,
                ModItems.MP_443.get()
            )
            gunSmithing(
                writer,
                ModItems.GLOCK_18_BLUEPRINT.get(),
                GunRarity.RARE,
                Items.GOLD_INGOT,
                ModItems.GLOCK_18.get()
            )
            gunSmithing(
                writer,
                ModItems.HUNTING_RIFLE_BLUEPRINT.get(),
                GunRarity.RARE,
                ItemTags.LOGS,
                ModItems.HUNTING_RIFLE.get()
            )
            gunSmithing(writer, ModItems.M_79_BLUEPRINT.get(), GunRarity.RARE, Items.DISPENSER, ModItems.M_79.get())
            gunSmithing(writer, ModItems.RPG_BLUEPRINT.get(), GunRarity.RARE, Items.DISPENSER, ModItems.RPG.get())
            gunSmithing(writer, ModItems.BOCEK_BLUEPRINT.get(), GunRarity.EPIC, Items.BOW, ModItems.BOCEK.get())
            gunSmithing(
                writer,
                ModItems.M_4_BLUEPRINT.get(),
                GunRarity.RARE,
                ModTags.Items.INGOTS_STEEL,
                ModItems.M_4.get()
            )
            gunSmithing(
                writer,
                ModItems.AA_12_BLUEPRINT.get(),
                GunRarity.LEGENDARY,
                Items.NETHERITE_INGOT,
                ModItems.AA_12.get()
            )
            gunSmithing(
                writer,
                ModItems.HK_416_BLUEPRINT.get(),
                GunRarity.RARE,
                ModTags.Items.INGOTS_STEEL,
                ModItems.HK_416.get()
            )
            gunSmithing(writer, ModItems.RPK_BLUEPRINT.get(), GunRarity.EPIC, ItemTags.LOGS, ModItems.RPK.get())
            gunSmithing(writer, ModItems.SKS_BLUEPRINT.get(), GunRarity.RARE, ItemTags.LOGS, ModItems.SKS.get())
            gunSmithing(
                writer,
                ModItems.NTW_20_BLUEPRINT.get(),
                GunRarity.LEGENDARY,
                Items.SPYGLASS,
                ModItems.NTW_20.get()
            )
            gunSmithing(writer, ModItems.MP_5_BLUEPRINT.get(), GunRarity.RARE, Items.IRON_INGOT, ModItems.MP_5.get())
            gunSmithing(
                writer,
                ModItems.VECTOR_BLUEPRINT.get(),
                GunRarity.EPIC,
                ModTags.Items.INGOTS_CEMENTED_CARBIDE,
                ModItems.VECTOR.get()
            )
            gunSmithing(
                writer,
                ModItems.MINIGUN_BLUEPRINT.get(),
                GunRarity.LEGENDARY,
                ModItems.MOTOR.get(),
                ModItems.MINIGUN.get()
            )
            gunSmithing(
                writer,
                ModItems.MK_14_BLUEPRINT.get(),
                GunRarity.EPIC,
                ModTags.Items.INGOTS_CEMENTED_CARBIDE,
                ModItems.MK_14.get()
            )
            gunSmithing(
                writer,
                ModItems.SENTINEL_BLUEPRINT.get(),
                GunRarity.EPIC,
                ModItems.CELL.get(),
                ModItems.SENTINEL.get()
            )
            gunSmithing(
                writer,
                ModItems.M_60_BLUEPRINT.get(),
                GunRarity.EPIC,
                ModTags.Items.INGOTS_CEMENTED_CARBIDE,
                ModItems.M_60.get()
            )
            gunSmithing(
                writer,
                ModItems.SVD_BLUEPRINT.get(),
                GunRarity.EPIC,
                ModTags.Items.INGOTS_CEMENTED_CARBIDE,
                ModItems.SVD.get()
            )
            gunSmithing(writer, ModItems.MARLIN_BLUEPRINT.get(), GunRarity.COMMON, ItemTags.LOGS, ModItems.MARLIN.get())
            gunSmithing(
                writer,
                ModItems.M_870_BLUEPRINT.get(),
                GunRarity.RARE,
                ModTags.Items.INGOTS_STEEL,
                ModItems.M_870.get()
            )
            gunSmithing(writer, ModItems.M_98B_BLUEPRINT.get(), GunRarity.EPIC, Items.SPYGLASS, ModItems.M_98B.get())
            gunSmithing(writer, ModItems.AK_47_BLUEPRINT.get(), GunRarity.RARE, ItemTags.LOGS, ModItems.AK_47.get())
            gunSmithing(
                writer,
                ModItems.AK_12_BLUEPRINT.get(),
                GunRarity.RARE,
                ModTags.Items.INGOTS_STEEL,
                ModItems.AK_12.get()
            )
            gunSmithing(
                writer,
                ModItems.DEVOTION_BLUEPRINT.get(),
                GunRarity.EPIC,
                ModTags.Items.INGOTS_CEMENTED_CARBIDE,
                ModItems.DEVOTION.get()
            )
            gunSmithing(
                writer,
                ModItems.TASER_BLUEPRINT.get(),
                GunRarity.COMMON,
                Items.YELLOW_CONCRETE,
                ModItems.TASER.get()
            )
            gunSmithing(
                writer,
                ModItems.M_1911_BLUEPRINT.get(),
                GunRarity.COMMON,
                ModTags.Items.INGOTS_STEEL,
                ModItems.M_1911.get()
            )
            gunSmithing(
                writer,
                ModItems.QBZ_95_BLUEPRINT.get(),
                GunRarity.RARE,
                ModTags.Items.INGOTS_STEEL,
                ModItems.QBZ_95.get()
            )
            gunSmithing(
                writer,
                ModItems.QBZ_191_BLUEPRINT.get(),
                GunRarity.EPIC,
                ModTags.Items.INGOTS_CEMENTED_CARBIDE,
                ModItems.QBZ_191.get()
            )
            gunSmithing(writer, ModItems.AWM_BLUEPRINT.get(), GunRarity.EPIC, Items.SPYGLASS, ModItems.AWM.get())
            gunSmithing(writer, ModItems.K_98_BLUEPRINT.get(), GunRarity.RARE, ItemTags.LOGS, ModItems.K_98.get())
            gunSmithing(
                writer,
                ModItems.MOSIN_NAGANT_BLUEPRINT.get(),
                GunRarity.RARE,
                ItemTags.LOGS,
                ModItems.MOSIN_NAGANT.get()
            )
            gunSmithing(
                writer,
                ModItems.JAVELIN_BLUEPRINT.get(),
                GunRarity.LEGENDARY,
                ModItems.ANCIENT_CPU.get(),
                ModItems.JAVELIN.get()
            )
            gunSmithing(
                writer,
                ModItems.IGLA_BLUEPRINT.get(),
                GunRarity.EPIC,
                ModItems.ANCIENT_CPU.get(),
                ModItems.IGLA_9K38.get()
            )
            gunSmithing(
                writer,
                ModItems.M_2_HB_BLUEPRINT.get(),
                GunRarity.RARE,
                ModTags.Items.STORAGE_BLOCK_STEEL,
                ModItems.M_2_HB.get()
            )
            gunSmithing(
                writer,
                ModItems.SECONDARY_CATACLYSM_BLUEPRINT.get(),
                GunRarity.VIRTUAL,
                ModItems.KNIFE.get(),
                ModItems.SECONDARY_CATACLYSM.get()
            )
            gunSmithing(
                writer,
                ModItems.INSIDIOUS_BLUEPRINT.get(),
                GunRarity.EPIC,
                ModTags.Items.INGOTS_CEMENTED_CARBIDE,
                ModItems.INSIDIOUS.get()
            )
            gunSmithing(
                writer,
                ModItems.QL_1031_BLUEPRINT.get(),
                GunRarity.VIRTUAL,
                ModItems.BATTERY.get(),
                ModItems.QL_1031.get()
            )
            gunSmithing(
                writer,
                ModItems.SUPER_STAR_SHOOTER_BLUEPRINT.get(),
                GunRarity.SUPERB,
                ModItems.MEDIUM_ARMAMENT_MODULE.get(),
                ModItems.SUPER_STAR_SHOOTER.get()
            )

            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.HOMEMADE_SHOTGUN.get())
                .pattern("aab")
                .pattern("ccc")
                .pattern(" dc")
                .define('a', ModItems.IRON_MATERIALS.barrel.get())
                .define('b', Items.FLINT_AND_STEEL)
                .define('c', ItemTags.PLANKS)
                .define('d', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy(getHasName(ModItems.IRON_MATERIALS.barrel.get()), has(ModItems.IRON_MATERIALS.barrel.get()))
                .save(writer, loc(getItemName(ModItems.HOMEMADE_SHOTGUN.get())))
        }

        private fun buildBlueprintRecipes(writer: RecipeOutput) {
            copyBlueprint(writer, ModItems.TRACHELIUM_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.GLOCK_17_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.MP_443_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.GLOCK_18_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.HUNTING_RIFLE_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.M_79_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.RPG_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.BOCEK_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.M_4_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.AA_12_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.HK_416_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.RPK_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.SKS_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.NTW_20_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.MP_5_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.VECTOR_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.MINIGUN_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.MK_14_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.SENTINEL_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.M_60_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.SVD_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.MARLIN_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.M_870_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.AWM_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.M_98B_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.AK_47_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.AK_12_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.DEVOTION_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.TASER_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.M_1911_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.QBZ_95_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.QBZ_191_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.K_98_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.MOSIN_NAGANT_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.JAVELIN_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.IGLA_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.M_2_HB_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.SECONDARY_CATACLYSM_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.INSIDIOUS_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.MK_42_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.MLE_1934_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.BL_132_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.HPJ_11_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.ANNIHILATOR_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.QL_1031_BLUEPRINT.get())
            copyBlueprint(writer, ModItems.SUPER_STAR_SHOOTER_BLUEPRINT.get())
        }

        private fun buildPerkRecipes(writer: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EMPTY_PERK.get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', Items.PAPER)
                .define('b', Items.LAPIS_LAZULI)
                .define('c', Tags.Items.INGOTS_IRON)
                .unlockedBy(getHasName(Items.PAPER), has(Items.PAPER))
                .save(writer, loc(getItemName(ModItems.EMPTY_PERK.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS[ModPerks.AP_BULLET]!!.get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', commonItemTag("storage_blocks/tungsten"))
                .define('c', INGOTS_TUNGSTEN)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.AP_BULLET))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS[ModPerks.CUPID_ARROW]!!.get())
                .pattern("cbc")
                .pattern("dad")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Items.BOW)
                .define('c', ItemTags.ARROWS)
                .define('d', getPotionIngredient(Potions.HEALING))
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.CUPID_ARROW))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS[ModPerks.FIREFLY]!!.get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Ingredient.of(Items.OCHRE_FROGLIGHT, Items.VERDANT_FROGLIGHT, Items.PEARLESCENT_FROGLIGHT))
                .define('c', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.FIREFLY))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS[ModPerks.HE_BULLET]!!.get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Items.TNT)
                .define('c', ModItems.HIGH_ENERGY_EXPLOSIVES.get())
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.HE_BULLET))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS[ModPerks.INCENDIARY_BULLET]!!.get())
                .pattern("bbb")
                .pattern("cac")
                .pattern("bbb")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Items.BLAZE_POWDER)
                .define('c', Items.DRAGON_BREATH)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.INCENDIARY_BULLET))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS[ModPerks.INTELLIGENT_CHIP]!!.get())
                .pattern("bbb")
                .pattern("bab")
                .pattern("bbb")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', ModItems.ANCIENT_CPU.get())
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.INTELLIGENT_CHIP))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS[ModPerks.JHP_BULLET]!!.get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Tags.Items.STORAGE_BLOCKS_COPPER)
                .define('c', Tags.Items.INGOTS_COPPER)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.JHP_BULLET))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS[ModPerks.LONGER_WIRE]!!.get())
                .pattern("bbb")
                .pattern("bab")
                .pattern("bbb")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Items.STRING)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.LONGER_WIRE))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS[ModPerks.MICRO_MISSILE]!!.get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', ModItems.GRAIN.get())
                .define('c', Items.FIREWORK_ROCKET)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.MICRO_MISSILE))
            ShapedRecipeBuilder.shaped(
                RecipeCategory.MISC,
                ModItems.PERK_ITEMS[ModPerks.PHASE_PENETRATING_BULLET]!!.get()
            )
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Tags.Items.INGOTS_NETHERITE)
                .define('c', ModItems.AP_HEAD.get())
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.PHASE_PENETRATING_BULLET))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS[ModPerks.POISONOUS_BULLET]!!.get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', commonItemTag("storage_blocks/lead"))
                .define('c', Items.SPIDER_EYE)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.POISONOUS_BULLET))
            ShapedRecipeBuilder.shaped(
                RecipeCategory.MISC,
                ModItems.PERK_ITEMS[ModPerks.POWERFUL_ATTRACTION]!!.get()
            )
                .pattern("dbe")
                .pattern("cac")
                .pattern(" c ")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Tags.Items.ENDER_PEARLS)
                .define('c', Tags.Items.INGOTS_IRON)
                .define('d', Tags.Items.DUSTS_REDSTONE)
                .define('e', Tags.Items.GEMS_LAPIS)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.POWERFUL_ATTRACTION))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS[ModPerks.REGENERATION]!!.get())
                .pattern("ccc")
                .pattern("bab")
                .pattern("ddd")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', ModItems.CELL.get())
                .define('c', Items.DAYLIGHT_DETECTOR)
                .define('d', Tags.Items.INGOTS_GOLD)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.REGENERATION))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS[ModPerks.RIOT_BULLET]!!.get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Items.SLIME_BLOCK)
                .define('c', Items.COBWEB)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.RIOT_BULLET))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS[ModPerks.SILVER_BULLET]!!.get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', commonItemTag("storage_blocks/silver"))
                .define('c', INGOTS_SILVER)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.SILVER_BULLET))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS[ModPerks.TURBO_CHARGER]!!.get())
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Items.PISTON)
                .define('c', ModTags.Items.INGOTS_STEEL)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.TURBO_CHARGER))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS[ModPerks.VOLT_OVERLOAD]!!.get())
                .pattern("cec")
                .pattern("bab")
                .pattern("bdb")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', ModItems.CELL.get())
                .define('c', Items.LIGHTNING_ROD)
                .define('d', commonItemTag("dusts/coal_coke"))
                .define('e', Tags.Items.INGOTS_IRON)
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .save(writer, perkLoc(ModPerks.VOLT_OVERLOAD))
            ShapedRecipeBuilder.shaped(
                RecipeCategory.MISC,
                ModItems.PERK_ITEMS[ModPerks.BACKPACK_LINKED_MAGAZINE]!!.get()
            )
                .pattern("cbc")
                .pattern("bab")
                .pattern("cbc")
                .define('a', ModItems.PERK_ITEMS[ModPerks.SUBSISTENCE]!!.get())
                .define('b', Tags.Items.CHESTS_ENDER)
                .define('c', Tags.Items.CHESTS_WOODEN)
                .unlockedBy(
                    getHasName(ModItems.PERK_ITEMS[ModPerks.SUBSISTENCE]!!.get()),
                    has(ModItems.PERK_ITEMS[ModPerks.SUBSISTENCE]!!.get())
                )
                .save(writer, perkLoc(ModPerks.BACKPACK_LINKED_MAGAZINE))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS[ModPerks.POWERFUL_COOLER]!!.get())
                .pattern("cdc")
                .pattern("bab")
                .pattern("cdc")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', Items.POWDER_SNOW_BUCKET)
                .define('c', Items.BLUE_ICE)
                .define('d', commonItemTag("storage_blocks/silver"))
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .unlockedBy(getHasName(Items.POWDER_SNOW_BUCKET), has(Items.POWDER_SNOW_BUCKET))
                .save(writer, perkLoc(ModPerks.POWERFUL_COOLER))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PERK_ITEMS[ModPerks.BLADE_BULLET]!!.get())
                .pattern("dbd")
                .pattern("cac")
                .pattern("ebe")
                .define('a', ModItems.EMPTY_PERK.get())
                .define('b', ModTags.Items.STORAGE_BLOCK_STEEL)
                .define('c', ModItems.BARBED_WIRE.get())
                .define('d', ModItems.KNIFE.get())
                .define('e', ModItems.CLAYMORE_MINE.get())
                .unlockedBy(getHasName(ModItems.EMPTY_PERK.get()), has(ModItems.EMPTY_PERK.get()))
                .unlockedBy(getHasName(ModItems.CLAYMORE_MINE.get()), has(ModItems.CLAYMORE_MINE.get()))
                .save(writer, perkLoc(ModPerks.BLADE_BULLET))
        }

        private fun buildMiscRecipes(writer: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DOG_TAG.get())
                .pattern("a")
                .pattern("b")
                .define('a', Items.CHAIN)
                .define('b', Items.NAME_TAG)
                .unlockedBy(getHasName(Items.NAME_TAG), has(Items.NAME_TAG))
                .save(writer, loc(getItemName(ModItems.DOG_TAG.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DRONE.get(), 4)
                .pattern("a a")
                .pattern("bcb")
                .pattern("ded")
                .define('a', ModItems.PROPELLER.get())
                .define('b', ModItems.MOTOR.get())
                .define('c', Items.COMPASS)
                .define('d', Tags.Items.NUGGETS_IRON)
                .define('e', ModItems.CELL.get())
                .unlockedBy(getHasName(ModItems.PROPELLER.get()), has(ModItems.PROPELLER.get()))
                .unlockedBy(getHasName(ModItems.MOTOR.get()), has(ModItems.MOTOR.get()))
                .save(writer, loc(getItemName(ModItems.DRONE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FIRING_PARAMETERS.get())
                .pattern("a")
                .pattern("b")
                .pattern("c")
                .define('a', Items.TARGET)
                .define('b', Items.PAPER)
                .define('c', ItemTags.PLANKS)
                .unlockedBy(getHasName(Items.TARGET), has(Items.TARGET))
                .save(writer, loc(getItemName(ModItems.FIRING_PARAMETERS.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.IFF.get())
                .pattern("ab")
                .pattern("c ")
                .define('a', Tags.Items.DUSTS_REDSTONE)
                .define('b', Tags.Items.GEMS_LAPIS)
                .define('c', PLATES_COPPER)
                .unlockedBy(getHasName(Items.LAPIS_LAZULI), has(Items.LAPIS_LAZULI))
                .save(writer, loc(getItemName(ModItems.IFF.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.THERMAL_IMAGING_GOGGLES.get())
                .pattern("aba")
                .pattern("cfc")
                .pattern("ede")
                .define('a', Items.EMERALD)
                .define('b', Tags.Items.GLASS_PANES)
                .define('c', Items.SPIDER_EYE)
                .define('d', ModItems.CELL.get())
                .define('e', Items.OBSERVER)
                .define('f', Items.DAYLIGHT_DETECTOR)
                .unlockedBy(getHasName(Items.SPIDER_EYE), has(Items.SPIDER_EYE))
                .save(writer, loc(getItemName(ModItems.THERMAL_IMAGING_GOGGLES.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PARACHUTE.get())
                .pattern("aaa")
                .pattern("b b")
                .pattern("bcb")
                .define('a', Items.PHANTOM_MEMBRANE)
                .define('b', Items.STRING)
                .define('c', Items.LEATHER)
                .unlockedBy(getHasName(Items.PHANTOM_MEMBRANE), has(Items.PHANTOM_MEMBRANE))
                .save(writer, loc(getItemName(ModItems.PARACHUTE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TRANSCRIPT.get())
                .pattern("a")
                .pattern("b")
                .pattern("c")
                .define('a', Tags.Items.NUGGETS_IRON)
                .define('b', Items.PAPER)
                .define('c', ItemTags.PLANKS)
                .unlockedBy(getHasName(Items.PAPER), has(Items.PAPER))
                .save(writer, loc(getItemName(ModItems.TRANSCRIPT.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DATA_CHIP_SUBSTRATE.get(), 4)
                .pattern("dad")
                .pattern("aba")
                .pattern("dad")
                .define('a', PLATES_COPPER)
                .define('b', Items.AMETHYST_SHARD)
                .define('d', Items.IRON_INGOT)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, loc(getItemName(ModItems.DATA_CHIP_SUBSTRATE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMMON_BLUEPRINT_DATA_CHIP.get())
                .pattern(" a ")
                .pattern("cbc")
                .pattern(" d ")
                .define('a', Tags.Items.GLASS_BLOCKS)
                .define('b', ModItems.DATA_CHIP_SUBSTRATE.get())
                .define('c', Items.IRON_INGOT)
                .define('d', Tags.Items.NUGGETS_GOLD)
                .unlockedBy(getHasName(ModItems.DATA_CHIP_SUBSTRATE.get()), has(ModItems.DATA_CHIP_SUBSTRATE.get()))
                .save(writer, loc(getItemName(ModItems.COMMON_BLUEPRINT_DATA_CHIP.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RARE_BLUEPRINT_DATA_CHIP.get())
                .pattern(" a ")
                .pattern("cbc")
                .pattern(" d ")
                .define('a', Tags.Items.GLASS_BLOCKS)
                .define('b', ModItems.DATA_CHIP_SUBSTRATE.get())
                .define('c', ModTags.Items.INGOTS_STEEL)
                .define('d', Tags.Items.NUGGETS_GOLD)
                .unlockedBy(getHasName(ModItems.DATA_CHIP_SUBSTRATE.get()), has(ModItems.DATA_CHIP_SUBSTRATE.get()))
                .save(writer, loc(getItemName(ModItems.RARE_BLUEPRINT_DATA_CHIP.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EPIC_BLUEPRINT_DATA_CHIP.get())
                .pattern(" a ")
                .pattern("cbc")
                .pattern(" d ")
                .define('a', Tags.Items.GLASS_BLOCKS)
                .define('b', ModItems.DATA_CHIP_SUBSTRATE.get())
                .define('c', ModTags.Items.INGOTS_CEMENTED_CARBIDE)
                .define('d', Tags.Items.NUGGETS_GOLD)
                .unlockedBy(getHasName(ModItems.DATA_CHIP_SUBSTRATE.get()), has(ModItems.DATA_CHIP_SUBSTRATE.get()))
                .save(writer, loc(getItemName(ModItems.EPIC_BLUEPRINT_DATA_CHIP.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.LEGENDARY_BLUEPRINT_DATA_CHIP.get())
                .pattern(" a ")
                .pattern("cbc")
                .pattern(" d ")
                .define('a', Tags.Items.GLASS_BLOCKS)
                .define('b', ModItems.DATA_CHIP_SUBSTRATE.get())
                .define('c', Items.NETHERITE_SCRAP)
                .define('d', Tags.Items.NUGGETS_GOLD)
                .unlockedBy(getHasName(ModItems.DATA_CHIP_SUBSTRATE.get()), has(ModItems.DATA_CHIP_SUBSTRATE.get()))
                .save(writer, loc(getItemName(ModItems.LEGENDARY_BLUEPRINT_DATA_CHIP.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.VIRTUAL_BLUEPRINT_DATA_CHIP.get())
                .pattern("eae")
                .pattern("cbc")
                .pattern("fdf")
                .define('a', Tags.Items.GLASS_BLOCKS)
                .define('b', ModItems.DATA_CHIP_SUBSTRATE.get())
                .define('c', ModTags.Items.INGOTS_CEMENTED_CARBIDE)
                .define('d', Tags.Items.NUGGETS_GOLD)
                .define('e', Tags.Items.GEMS_AMETHYST)
                .define('f', Tags.Items.GEMS_DIAMOND)
                .unlockedBy(getHasName(ModItems.DATA_CHIP_SUBSTRATE.get()), has(ModItems.DATA_CHIP_SUBSTRATE.get()))
                .save(writer, loc(getItemName(ModItems.VIRTUAL_BLUEPRINT_DATA_CHIP.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BOOST_RESEARCH_MODULE.get())
                .pattern("ada")
                .pattern("bcb")
                .pattern("ada")
                .define('a', ModTags.Items.INGOTS_CEMENTED_CARBIDE)
                .define('b', Tags.Items.INGOTS_GOLD)
                .define('c', ModItems.DATA_CHIP_SUBSTRATE.get())
                .define('d', Tags.Items.GEMS_EMERALD)
                .unlockedBy(getHasName(ModItems.DATA_CHIP_SUBSTRATE.get()), has(ModItems.DATA_CHIP_SUBSTRATE.get()))
                .save(writer, loc(getItemName(ModItems.BOOST_RESEARCH_MODULE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EFFECTIVE_RESEARCH_MODULE.get())
                .pattern("ada")
                .pattern("bcb")
                .pattern("ada")
                .define('a', ModTags.Items.INGOTS_CEMENTED_CARBIDE)
                .define('b', Tags.Items.INGOTS_GOLD)
                .define('c', ModItems.DATA_CHIP_SUBSTRATE.get())
                .define('d', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .unlockedBy(getHasName(ModItems.DATA_CHIP_SUBSTRATE.get()), has(ModItems.DATA_CHIP_SUBSTRATE.get()))
                .save(writer, loc(getItemName(ModItems.EFFECTIVE_RESEARCH_MODULE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DIRECTIONAL_RESEARCH_MODULE.get())
                .pattern("ada")
                .pattern("bcb")
                .pattern("ada")
                .define('a', ModTags.Items.INGOTS_CEMENTED_CARBIDE)
                .define('b', Tags.Items.INGOTS_GOLD)
                .define('c', ModItems.ANCIENT_CPU.get())
                .define('d', Tags.Items.GEMS_DIAMOND)
                .unlockedBy(getHasName(ModItems.ANCIENT_CPU.get()), has(ModItems.ANCIENT_CPU.get()))
                .save(writer, loc(getItemName(ModItems.DIRECTIONAL_RESEARCH_MODULE.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ENLARGEMENT_RESEARCH_MODULE.get())
                .pattern("ada")
                .pattern("bcb")
                .pattern("ada")
                .define('a', ModTags.Items.INGOTS_CEMENTED_CARBIDE)
                .define('b', Tags.Items.INGOTS_GOLD)
                .define('c', ModItems.ANCIENT_CPU.get())
                .define('d', Tags.Items.STORAGE_BLOCKS_IRON)
                .unlockedBy(getHasName(ModItems.ANCIENT_CPU.get()), has(ModItems.ANCIENT_CPU.get()))
                .save(writer, loc(getItemName(ModItems.ENLARGEMENT_RESEARCH_MODULE.get())))

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.AMMO_PERK_DATA_CHIP.get())
                .pattern(" a ")
                .pattern("cbc")
                .pattern(" d ")
                .define('a', Tags.Items.GLASS_BLOCKS)
                .define('b', ModItems.DATA_CHIP_SUBSTRATE.get())
                .define('c', INGOTS_LEAD)
                .define('d', Tags.Items.NUGGETS_GOLD)
                .unlockedBy(getHasName(ModItems.DATA_CHIP_SUBSTRATE.get()), has(ModItems.DATA_CHIP_SUBSTRATE.get()))
                .save(writer, loc(getItemName(ModItems.AMMO_PERK_DATA_CHIP.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FUNCTIONAL_PERK_DATA_CHIP.get())
                .pattern(" a ")
                .pattern("cbc")
                .pattern(" d ")
                .define('a', Tags.Items.GLASS_BLOCKS)
                .define('b', ModItems.DATA_CHIP_SUBSTRATE.get())
                .define('c', INGOTS_SILVER)
                .define('d', Tags.Items.NUGGETS_GOLD)
                .unlockedBy(getHasName(ModItems.DATA_CHIP_SUBSTRATE.get()), has(ModItems.DATA_CHIP_SUBSTRATE.get()))
                .save(writer, loc(getItemName(ModItems.FUNCTIONAL_PERK_DATA_CHIP.get())))
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DAMAGE_PERK_DATA_CHIP.get())
                .pattern(" a ")
                .pattern("cbc")
                .pattern(" d ")
                .define('a', Tags.Items.GLASS_BLOCKS)
                .define('b', ModItems.DATA_CHIP_SUBSTRATE.get())
                .define('c', INGOTS_TUNGSTEN)
                .define('d', Tags.Items.NUGGETS_GOLD)
                .unlockedBy(getHasName(ModItems.DATA_CHIP_SUBSTRATE.get()), has(ModItems.DATA_CHIP_SUBSTRATE.get()))
                .save(writer, loc(getItemName(ModItems.DAMAGE_PERK_DATA_CHIP.get())))
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.GALENA.get(), 9)
                .requires(commonItemTag("storage_blocks/raw_lead"))
                .unlockedBy(getHasName(ModItems.RAW_GALENA_BLOCK.get()), has(commonItemTag("storage_blocks/raw_lead")))
                .save(writer, loc("${getItemName(ModItems.GALENA.get())}_from_raw_block"))
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.SCHEELITE.get(), 9)
                .requires(commonItemTag("storage_blocks/raw_tungsten"))
                .unlockedBy(getHasName(ModItems.RAW_SCHEELITE_BLOCK.get()), has(commonItemTag("storage_blocks/raw_tungsten")))
                .save(writer, loc("${getItemName(ModItems.SCHEELITE.get())}_from_raw_block"))
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.RAW_SILVER.get(), 9)
                .requires(commonItemTag("storage_blocks/raw_silver"))
                .unlockedBy(getHasName(ModItems.RAW_SILVER_BLOCK.get()), has(commonItemTag("storage_blocks/raw_silver")))
                .save(writer, loc("${getItemName(ModItems.RAW_SILVER.get())}_from_raw_block"))
        }

        private fun buildSpecialRecipes(writer: RecipeOutput) {
            SpecialRecipeBuilder.special(::PotionMortarShellRecipe)
                .save(writer, "superbwarfare:potion_mortar_shell")
            SpecialRecipeBuilder.special(::SmokeDyeRecipe).save(writer, "superbwarfare:smoke_dye")
            SpecialRecipeBuilder.special(::VehicleResetRecipe).save(writer, "superbwarfare:vehicle_reset")
        }

        private fun buildResearchRecipes(writer: RecipeOutput) {
            this.generateBlueprintResearchingRecipe(writer, Rarity.COMMON)
            this.generateBlueprintResearchingRecipe(writer, Rarity.RARE)
            this.generateBlueprintResearchingRecipe(writer, Rarity.EPIC)
            this.generateBlueprintResearchingRecipe(writer, ModRarities.LEGENDARY)
            this.generateBlueprintResearchingRecipe(writer, ModRarities.SUPERB)
            this.generateBlueprintResearchingRecipe(writer, ModRarities.VIRTUAL)

            Perk.Type.entries.forEach { this.generatePerkResearchingRecipe(writer, it) }
        }

        fun copyBlueprint(writer: RecipeOutput, result: ItemLike) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result, 2)
                .pattern("ABA")
                .pattern("ACA")
                .pattern("AAA")
                .define('A', Items.LAPIS_LAZULI)
                .define('B', Items.PAPER)
                .define('C', result)
                .unlockedBy(getHasName(result), has(result))
                .save(writer, loc("${getItemName(result)}_copy"))
            ResearchingRecipeBuilder.item(result.asItem(), 2, result)
                .base(Items.PAPER)
                .addition(Items.LAPIS_LAZULI)
                .time(600)
                .unlockedBy(getHasName(result), has(result))
                .save(writer, loc("${getItemName(result)}_copy_researching"))
        }

        fun gunSmithing(
            writer: RecipeOutput,
            blueprint: ItemLike,
            rarity: GunRarity,
            tagKey: TagKey<Item>,
            pResultItem: Item
        ) {
            gunSmithing(writer, blueprint, rarity, Ingredient.of(tagKey), pResultItem)
        }

        fun gunSmithing(
            writer: RecipeOutput,
            blueprint: ItemLike,
            rarity: GunRarity,
            ingredient: ItemLike,
            pResultItem: Item
        ) {
            gunSmithing(writer, blueprint, rarity, Ingredient.of(ingredient), pResultItem)
        }

        fun gunSmithing(
            writer: RecipeOutput,
            blueprint: ItemLike,
            rarity: GunRarity,
            ingredient: Ingredient,
            pResultItem: Item
        ) {
            val pack: ItemLike = when (rarity) {
                GunRarity.COMMON -> ModItems.COMMON_MATERIAL_PACK.get()
                GunRarity.RARE -> ModItems.RARE_MATERIAL_PACK.get()
                GunRarity.EPIC -> ModItems.EPIC_MATERIAL_PACK.get()
                GunRarity.LEGENDARY -> ModItems.LEGENDARY_MATERIAL_PACK.get()
                GunRarity.SUPERB -> ModItems.SUPERB_MATERIAL_PACK.get()
                GunRarity.VIRTUAL -> ModItems.VIRTUAL_MATERIAL_PACK.get()
            }

            SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(blueprint),
                Ingredient.of(pack),
                ingredient,
                RecipeCategory.COMBAT,
                pResultItem
            )
                .unlocks(getHasName(blueprint), has(blueprint))
                .save(writer, loc(getItemName(pResultItem) + "_smithing"))
        }

        fun perkLoc(perk: DeferredHolder<Perk, out Perk>): ResourceLocation {
            return loc("perk/" + getItemName(ModItems.PERK_ITEMS[perk]!!.get()))
        }

        fun getEntityTypeName(entityType: EntityType<*>): String {
            return BuiltInRegistries.ENTITY_TYPE.getKey(entityType).path
        }

        // 生成材料包所有材料的配方
        fun generateMaterialRecipes(writer: RecipeOutput, material: Materials, ingredient: ItemLike) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, material.barrel.get())
                .pattern("AAA")
                .define('A', ingredient)
                .unlockedBy(getHasName(ingredient), has(ingredient))
                .save(writer, loc(getItemName(material.barrel.get())))

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, material.action.get())
                .pattern("AAA")
                .pattern("  A")
                .define('A', ingredient)
                .unlockedBy(getHasName(ingredient), has(ingredient))
                .save(writer, loc(getItemName(material.action.get())))

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, material.spring.get())
                .pattern("A")
                .pattern("A")
                .pattern("A")
                .define('A', ingredient)
                .unlockedBy(getHasName(ingredient), has(ingredient))
                .save(writer, loc(getItemName(material.spring.get())))

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, material.trigger.get())
                .pattern("BA")
                .pattern(" A")
                .define('A', ingredient)
                .define('B', Items.TRIPWIRE_HOOK)
                .unlockedBy(getHasName(ingredient), has(ingredient))
                .save(writer, loc(getItemName(material.trigger.get())))
        }

        fun generateMaterialRecipes(writer: RecipeOutput, material: Materials, tagKey: TagKey<Item>, name: Item) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, material.barrel.get())
                .pattern("AAA")
                .define('A', tagKey)
                .unlockedBy(getHasName(name), has(tagKey))
                .save(writer, loc(getItemName(material.barrel.get())))

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, material.action.get())
                .pattern("AAA")
                .pattern("  A")
                .define('A', tagKey)
                .unlockedBy(getHasName(name), has(tagKey))
                .save(writer, loc(getItemName(material.action.get())))

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, material.spring.get())
                .pattern("A")
                .pattern("A")
                .pattern("A")
                .define('A', tagKey)
                .unlockedBy(getHasName(name), has(tagKey))
                .save(writer, loc(getItemName(material.spring.get())))

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, material.trigger.get())
                .pattern("BA")
                .pattern(" A")
                .define('A', tagKey)
                .define('B', Items.TRIPWIRE_HOOK)
                .unlockedBy(getHasName(name), has(tagKey))
                .save(writer, loc(getItemName(material.trigger.get())))
        }

        fun generateSmithingMaterialRecipe(
            writer: RecipeOutput,
            material: Materials,
            result: Materials,
            template: Item,
            ingredient: Item
        ) {
            SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(template),
                Ingredient.of(material.barrel.get()),
                Ingredient.of(ingredient),
                RecipeCategory.MISC,
                result.barrel.get()
            )
                .unlocks(getHasName(template), has(template))
                .unlocks(getHasName(material.barrel.get()), has(material.barrel.get()))
                .save(writer, loc(getItemName(result.barrel.get())))

            SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(template),
                Ingredient.of(material.action.get()),
                Ingredient.of(ingredient),
                RecipeCategory.MISC,
                result.action.get()
            )
                .unlocks(getHasName(template), has(template))
                .unlocks(getHasName(material.action.get()), has(material.action.get()))
                .save(writer, loc(getItemName(result.action.get())))

            SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(template),
                Ingredient.of(material.spring.get()),
                Ingredient.of(ingredient),
                RecipeCategory.MISC,
                result.spring.get()
            )
                .unlocks(getHasName(template), has(template))
                .unlocks(getHasName(material.spring.get()), has(material.spring.get()))
                .save(writer, loc(getItemName(result.spring.get())))

            SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(template),
                Ingredient.of(material.trigger.get()),
                Ingredient.of(ingredient),
                RecipeCategory.MISC,
                result.trigger.get()
            )
                .unlocks(getHasName(template), has(template))
                .unlocks(getHasName(material.trigger.get()), has(material.trigger.get()))
                .save(writer, loc(getItemName(result.trigger.get())))
        }

        fun generateMaterialPackRecipe(writer: RecipeOutput, material: Materials, pack: Item) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, pack)
                .requires(material.barrel.get())
                .requires(material.action.get())
                .requires(material.spring.get())
                .requires(material.trigger.get())
                .unlockedBy(getHasName(material.barrel.get()), has(material.barrel.get()))
                .unlockedBy(getHasName(material.action.get()), has(material.action.get()))
                .unlockedBy(getHasName(material.spring.get()), has(material.spring.get()))
                .unlockedBy(getHasName(material.trigger.get()), has(material.trigger.get()))
                .save(writer, loc(getItemName(pack)))
        }

        fun getPotionIngredient(potion: Holder<Potion>): Ingredient {
            return DataComponentIngredient.of(
                false,
                DataComponentMap.builder().set(DataComponents.POTION_CONTENTS, PotionContents(potion)).build(),
                Items.POTION
            )
        }

        fun generateBlueprintResearchingRecipe(writer: RecipeOutput, rarity: Rarity) {
            val tag: TagKey<Item>
            val enlargedTag: TagKey<Item>?
            val input: Item
            val time: Int
            when (rarity) {
                Rarity.RARE -> {
                    tag = ModTags.Items.RARE_BLUEPRINT
                    enlargedTag = ModTags.Items.ENLARGED_RARE_BLUEPRINT
                    input = ModItems.RARE_BLUEPRINT_DATA_CHIP.get()
                    time = 600
                }

                Rarity.EPIC -> {
                    tag = ModTags.Items.EPIC_BLUEPRINT
                    enlargedTag = ModTags.Items.ENLARGED_EPIC_BLUEPRINT
                    input = ModItems.EPIC_BLUEPRINT_DATA_CHIP.get()
                    time = 1500
                }

                ModRarities.LEGENDARY -> {
                    tag = ModTags.Items.LEGENDARY_BLUEPRINT
                    enlargedTag = ModTags.Items.ENLARGED_LEGENDARY_BLUEPRINT
                    input = ModItems.LEGENDARY_BLUEPRINT_DATA_CHIP.get()
                    time = 3000
                }

                ModRarities.SUPERB -> {
                    tag = ModTags.Items.SUPERB_BLUEPRINT
                    enlargedTag = null
                    input = ModItems.SUPERB_BLUEPRINT_DATA_CHIP.get()
                    time = 6000
                }

                ModRarities.VIRTUAL -> {
                    tag = ModTags.Items.VIRTUAL_BLUEPRINT
                    enlargedTag = null
                    input = ModItems.VIRTUAL_BLUEPRINT_DATA_CHIP.get()
                    time = 2400
                }

                else -> {
                    tag = ModTags.Items.COMMON_BLUEPRINT
                    enlargedTag = ModTags.Items.ENLARGED_COMMON_BLUEPRINT
                    input = ModItems.COMMON_BLUEPRINT_DATA_CHIP.get()
                    time = 300
                }
            }

            ResearchingRecipeBuilder.tag(tag, input = input)
                .base(Items.PAPER)
                .addition(Items.LAPIS_LAZULI)
                .time(time)
                .unlockedBy(getHasName(input), has(input))
                .save(writer, loc(getItemName(input) + "_researching"))
            ResearchingRecipeBuilder.tag(tag, 2, input)
                .base(Items.PAPER)
                .addition(Items.LAPIS_LAZULI)
                .special(ModItems.BOOST_RESEARCH_MODULE.get())
                .time(time)
                .color(1)
                .unlockedBy(getHasName(input), has(input))
                .unlockedBy(getHasName(ModItems.BOOST_RESEARCH_MODULE.get()), has(ModItems.BOOST_RESEARCH_MODULE.get()))
                .save(writer, loc(getItemName(input) + "_researching_boost"))
            ResearchingRecipeBuilder.tag(tag, input = input)
                .base(Items.PAPER)
                .addition(Items.LAPIS_LAZULI)
                .special(ModItems.DIRECTIONAL_RESEARCH_MODULE.get())
                .time(time)
                .color(2)
                .selectable()
                .unlockedBy(getHasName(input), has(input))
                .unlockedBy(
                    getHasName(ModItems.DIRECTIONAL_RESEARCH_MODULE.get()),
                    has(ModItems.DIRECTIONAL_RESEARCH_MODULE.get())
                )
                .save(writer, loc(getItemName(input) + "_researching_directional"))
            ResearchingRecipeBuilder.tag(tag, input = input)
                .base(Items.PAPER)
                .addition(Items.LAPIS_LAZULI)
                .special(ModItems.EFFECTIVE_RESEARCH_MODULE.get())
                .time(time / 5)
                .color(3)
                .unlockedBy(getHasName(input), has(input))
                .unlockedBy(
                    getHasName(ModItems.EFFECTIVE_RESEARCH_MODULE.get()),
                    has(ModItems.EFFECTIVE_RESEARCH_MODULE.get())
                )
                .save(writer, loc(getItemName(input) + "_researching_effective"))
            if (enlargedTag != null) {
                ResearchingRecipeBuilder.tag(enlargedTag, input = input)
                    .base(Items.PAPER)
                    .addition(Items.LAPIS_LAZULI)
                    .special(ModItems.ENLARGEMENT_RESEARCH_MODULE.get())
                    .time(time * 2)
                    .color(4)
                    .unlockedBy(getHasName(input), has(input))
                    .unlockedBy(
                        getHasName(ModItems.ENLARGEMENT_RESEARCH_MODULE.get()),
                        has(ModItems.ENLARGEMENT_RESEARCH_MODULE.get())
                    )
                    .save(writer, loc(getItemName(input) + "_researching_enlargement"))
            }

            ResearchingRecipeBuilder.item(input, input = tag)
                .base(ModItems.DATA_CHIP_SUBSTRATE.get())
                .addition(Items.AMETHYST_SHARD)
                .time(200)
                .unlockedBy("has_${tag.location.path}", has(tag))
                .save(writer, loc(getItemName(input) + "_from_blueprint"))
            ResearchingRecipeBuilder.item(input, 2, tag)
                .base(ModItems.DATA_CHIP_SUBSTRATE.get())
                .addition(Items.AMETHYST_SHARD)
                .time(200)
                .special(ModItems.BOOST_RESEARCH_MODULE.get())
                .color(1)
                .unlockedBy("has_${tag.location.path}", has(tag))
                .unlockedBy(getHasName(ModItems.BOOST_RESEARCH_MODULE.get()), has(ModItems.BOOST_RESEARCH_MODULE.get()))
                .save(writer, loc(getItemName(input) + "_from_blueprint_boost"))
        }

        fun generatePerkResearchingRecipe(writer: RecipeOutput, type: Perk.Type) {
            val inputPerk: Item
            val resTag: TagKey<Item>
            when (type) {
                Perk.Type.AMMO -> {
                    inputPerk = ModItems.AMMO_PERK_DATA_CHIP.get()
                    resTag = ModTags.Items.RESEARCHABLE_AMMO_PERK
                }

                Perk.Type.FUNCTIONAL -> {
                    inputPerk = ModItems.FUNCTIONAL_PERK_DATA_CHIP.get()
                    resTag = ModTags.Items.RESEARCHABLE_FUNCTIONAL_PERK
                }

                Perk.Type.DAMAGE -> {
                    inputPerk = ModItems.DAMAGE_PERK_DATA_CHIP.get()
                    resTag = ModTags.Items.RESEARCHABLE_DAMAGE_PERK
                }
            }

            ResearchingRecipeBuilder.tag(resTag, 1, inputPerk)
                .base(ModItems.EMPTY_PERK.get())
                .time(600)
                .unlockedBy(getHasName(inputPerk), has(inputPerk))
                .save(writer, loc(getItemName(inputPerk) + "_researching"))
            ResearchingRecipeBuilder.tag(resTag, 2, inputPerk)
                .base(ModItems.EMPTY_PERK.get())
                .special(ModItems.BOOST_RESEARCH_MODULE.get())
                .time(600)
                .color(1)
                .unlockedBy(getHasName(inputPerk), has(inputPerk))
                .unlockedBy(getHasName(ModItems.BOOST_RESEARCH_MODULE.get()), has(ModItems.BOOST_RESEARCH_MODULE.get()))
                .save(writer, loc(getItemName(inputPerk) + "_researching_boost"))
            ResearchingRecipeBuilder.tag(resTag, 1, inputPerk)
                .base(ModItems.EMPTY_PERK.get())
                .special(ModItems.DIRECTIONAL_RESEARCH_MODULE.get())
                .time(600)
                .color(2)
                .selectable()
                .unlockedBy(getHasName(inputPerk), has(inputPerk))
                .unlockedBy(
                    getHasName(ModItems.DIRECTIONAL_RESEARCH_MODULE.get()),
                    has(ModItems.DIRECTIONAL_RESEARCH_MODULE.get())
                )
                .save(writer, loc(getItemName(inputPerk) + "_researching_directional"))
            ResearchingRecipeBuilder.tag(resTag, 1, inputPerk)
                .base(ModItems.EMPTY_PERK.get())
                .special(ModItems.EFFECTIVE_RESEARCH_MODULE.get())
                .time(120)
                .color(3)
                .unlockedBy(getHasName(inputPerk), has(inputPerk))
                .unlockedBy(
                    getHasName(ModItems.EFFECTIVE_RESEARCH_MODULE.get()),
                    has(ModItems.EFFECTIVE_RESEARCH_MODULE.get())
                )
                .save(writer, loc(getItemName(inputPerk) + "_researching_effective"))

            ResearchingRecipeBuilder.item(inputPerk, 1, resTag)
                .base(ModItems.DATA_CHIP_SUBSTRATE.get())
                .addition(Items.AMETHYST_SHARD)
                .time(200)
                .unlockedBy("has_${resTag.location.path}", has(resTag))
                .save(writer, loc(getItemName(inputPerk) + "_from_blueprint"))
            ResearchingRecipeBuilder.item(inputPerk, 2, resTag)
                .base(ModItems.DATA_CHIP_SUBSTRATE.get())
                .addition(Items.AMETHYST_SHARD)
                .time(200)
                .special(ModItems.BOOST_RESEARCH_MODULE.get())
                .color(1)
                .unlockedBy("has_${resTag.location.path}", has(resTag))
                .unlockedBy(getHasName(ModItems.BOOST_RESEARCH_MODULE.get()), has(ModItems.BOOST_RESEARCH_MODULE.get()))
                .save(writer, loc(getItemName(inputPerk) + "_from_blueprint_boost"))
        }
    }
}
