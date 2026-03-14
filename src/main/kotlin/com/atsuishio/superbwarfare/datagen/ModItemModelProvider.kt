package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.datagen.builder.CustomSeparateModelBuilder
import com.atsuishio.superbwarfare.init.ModBlocks
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModItems.Materials
import com.atsuishio.superbwarfare.init.ModPerks
import net.minecraft.client.renderer.block.model.BlockModel
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.level.block.Block
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder
import net.neoforged.neoforge.client.model.generators.ItemModelProvider
import net.neoforged.neoforge.client.model.generators.ModelFile
import net.neoforged.neoforge.common.data.ExistingFileHelper
import net.neoforged.neoforge.registries.DeferredHolder

@Suppress("unused")
class ModItemModelProvider(output: PackOutput, existingFileHelper: ExistingFileHelper) :
    ItemModelProvider(output, Mod.MODID, existingFileHelper) {
    override fun registerModels() {
        // gun
        gunItem(ModItems.AA_12)
        gunItem(ModItems.AK_12)
        gunItem(ModItems.AK_47)
        gunItem(ModItems.BOCEK)
        gunItem(ModItems.DEVOTION)
        gunItem(ModItems.GLOCK_17)
        gunItem(ModItems.GLOCK_18, "glock_17")
        gunItem(ModItems.HK_416)
        gunItem(ModItems.HOMEMADE_SHOTGUN)
        gunItem(ModItems.HUNTING_RIFLE)
        gunItem(ModItems.INSIDIOUS)
        gunItem(ModItems.JAVELIN)
        gunItem(ModItems.K_98)
        gunItem(ModItems.M_4)
        gunItem(ModItems.M_60)
        gunItem(ModItems.M_79)
        gunItem(ModItems.M_1911)
        gunItem(ModItems.M_870)
        gunItem(ModItems.M_98B)
        gunItem(ModItems.MARLIN)
        gunItem(ModItems.MINIGUN)
        gunItem(ModItems.MK_14)
        gunItem(ModItems.MOSIN_NAGANT)
        gunItem(ModItems.MP_443)
        gunItem(ModItems.NTW_20)
        gunItem(ModItems.QBZ_95)
        gunItem(ModItems.RPG)
        gunItem(ModItems.RPK)
        gunItem(ModItems.SECONDARY_CATACLYSM)
        gunItem(ModItems.SENTINEL)
        gunItem(ModItems.SKS)
        gunItem(ModItems.SVD)
        gunItem(ModItems.TASER)
        gunItem(ModItems.TRACHELIUM)
        gunItem(ModItems.VECTOR)
        gunItem(ModItems.MP_5)
        gunItem(ModItems.M_2_HB)
        gunItem(ModItems.QBZ_191)
        gunItem(ModItems.AWM)
        gunItem(ModItems.IGLA_9K38)
        gunItem(ModItems.REPAIR_TOOL)
        gunItem(ModItems.QL_1031)
        gunItem(ModItems.SUPER_STAR_SHOOTER)

        simpleItem(ModItems.VEHICLE_GUN)
        simpleItem(ModItems.MORTAR_SHELL)
        simpleItem(ModItems.LARGE_SHELL_AP)
        simpleItem(ModItems.LARGE_SHELL_HE)
        simpleItem(ModItems.LARGE_SHELL_CM)
        simpleItem(ModItems.LARGE_SHELL_WP)
        simpleItem(ModItems.LARGE_SHELL_GS)
        simpleItem(ModItems.JAVELIN_MISSILE)

        // misc
        simpleItem(ModItems.ANCIENT_CPU)
        simpleItem(ModItems.PROPELLER)
        simpleItem(ModItems.LARGE_PROPELLER)
        simpleItem(ModItems.MOTOR)
        simpleItem(ModItems.LARGE_MOTOR)
        simpleItem(ModItems.WHEEL)
        simpleItem(ModItems.TRACK)
        simpleItem(ModItems.DRONE)
        simpleItem(ModItems.LIGHT_ARMAMENT_MODULE)
        simpleItem(ModItems.MEDIUM_ARMAMENT_MODULE)
        simpleItem(ModItems.HEAVY_ARMAMENT_MODULE)

        simpleItem(ModItems.TARGET_DEPLOYER)
        simpleItem(ModItems.DPS_GENERATOR_DEPLOYER)
        simpleItem(ModItems.MORTAR_DEPLOYER)
        simpleItem(ModItems.MORTAR_BARREL)
        simpleItem(ModItems.MORTAR_BASE_PLATE)
        simpleItem(ModItems.MORTAR_BIPOD)
        simpleItem(ModItems.SEEKER)
        simpleItem(ModItems.MISSILE_ENGINE)
        simpleItem(ModItems.FUSEE)
        simpleItem(ModItems.PRIMER)
        simpleItem(ModItems.BLU_43_MINE)
        simpleItem(ModItems.AP_HEAD)
        simpleItem(ModItems.HE_HEAD)
        simpleItem(ModItems.CM_HEAD)
        simpleItem(ModItems.GS_HEAD)
        simpleItem(ModItems.CANNON_CORE)
        simpleItem(ModItems.COPPER_PLATE)
        simpleItem(ModItems.STEEL_INGOT)
        simpleItem(ModItems.LEAD_INGOT)
        simpleItem(ModItems.TUNGSTEN_INGOT)
        simpleItem(ModItems.CEMENTED_CARBIDE_INGOT)
        simpleItem(ModItems.HIGH_ENERGY_EXPLOSIVES)
        simpleItem(ModItems.GRAIN)
        simpleItem(ModItems.IRON_POWDER)
        simpleItem(ModItems.TUNGSTEN_POWDER)
        simpleItem(ModItems.COAL_POWDER)
        simpleItem(ModItems.COAL_IRON_POWDER)
        simpleItem(ModItems.RAW_CEMENTED_CARBIDE_POWDER)
        simpleItem(ModItems.GALENA)
        simpleItem(ModItems.SCHEELITE)
        simpleItem(ModItems.DOG_TAG)
        simpleItem(ModItems.IFF)
        simpleItem(ModItems.TRANSCRIPT)
        simpleItem(ModItems.RAW_SILVER)
        simpleItem(ModItems.SILVER_INGOT)
        handheldItem(ModItems.BEAST)
        handheldItem(ModItems.CROWBAR)
        handheldItem(ModItems.DEFUSER)
        simpleItem(ModItems.FIRING_PARAMETERS)
        simpleItem(ModItems.HANDGUN_AMMO)
        simpleItem(ModItems.RIFLE_AMMO)
        simpleItem(ModItems.SNIPER_AMMO)
        simpleItem(ModItems.SHOTGUN_AMMO)
        simpleItem(ModItems.HEAVY_AMMO)
        simpleItem(ModItems.SMALL_ROCKET)
        simpleItem(ModItems.MEDIUM_ROCKET_AP)
        simpleItem(ModItems.MEDIUM_ROCKET_HE)
        simpleItem(ModItems.MEDIUM_ROCKET_CM)
        simpleItem(ModItems.MEDIUM_ANTI_GROUND_MISSILE)
        simpleItem(ModItems.LARGE_ANTI_GROUND_MISSILE)
        simpleItem(ModItems.SMALL_SHELL_AP)
        simpleItem(ModItems.SMALL_SHELL_HE)
        simpleItem(ModItems.SMALL_SHELL_GS)
        simpleItem(ModItems.SMALL_SHELL_AA)
        simpleItem(ModItems.SWARM_DRONE)
        simpleItem(ModItems.SMALL_AERIAL_BOMB)
        simpleItem(ModItems.MEDIUM_AERIAL_BOMB)
        simpleItem(ModItems.SMALL_BATTERY_PACK)
        simpleItem(ModItems.MEDIUM_BATTERY_PACK)
        simpleItem(ModItems.LARGE_BATTERY_PACK)
        simpleItem(ModItems.MEDICAL_KIT)
        simpleItem(ModItems.PARACHUTE)
        simpleItem(ModItems.THERMAL_IMAGING_GOGGLES)
        simpleItem(ModItems.VEHICLE_DAMAGE_ANALYZER)
        simpleItem(ModItems.MEDIUM_ANTI_AIR_MISSILE)
        simpleItem(ModItems.LASER_UNIT)
        simpleItem(ModItems.TOW_DEPLOYER)
        simpleItem(ModItems.VEHICLE_RESET_KIT)
        handheldItem(ModItems.RPG_ROCKET_STANDARD)
        handheldItem(ModItems.RPG_ROCKET_TBG)
        simpleItem(ModItems.MORTAR_SHELL_WP)
        simpleItem(ModItems.WP_HEAD)
        simpleItem(ModItems.TUNGSTEN_ROD)
        simpleItem(ModItems.GRENADE_40MM)

        simpleMaterials(ModItems.IRON_MATERIALS)
        simpleMaterials(ModItems.STEEL_MATERIALS)
        simpleMaterials(ModItems.CEMENTED_CARBIDE_MATERIALS)
        simpleMaterials(ModItems.NETHERITE_MATERIALS)
        simpleMaterials(ModItems.CRYSTAL_MATERIALS)

        simpleItem(ModItems.COMMON_MATERIAL_PACK)
        simpleItem(ModItems.RARE_MATERIAL_PACK)
        simpleItem(ModItems.EPIC_MATERIAL_PACK)
        simpleItem(ModItems.LEGENDARY_MATERIAL_PACK)
        simpleItem(ModItems.SUPERB_MATERIAL_PACK)
        simpleItem(ModItems.VIRTUAL_MATERIAL_PACK)

        simpleItem(ModItems.DATA_CHIP_SUBSTRATE)
        simpleItem(ModItems.COMMON_BLUEPRINT_DATA_CHIP)
        simpleItem(ModItems.RARE_BLUEPRINT_DATA_CHIP)
        simpleItem(ModItems.EPIC_BLUEPRINT_DATA_CHIP)
        simpleItem(ModItems.LEGENDARY_BLUEPRINT_DATA_CHIP)
        simpleItem(ModItems.SUPERB_BLUEPRINT_DATA_CHIP)
        simpleItem(ModItems.VIRTUAL_BLUEPRINT_DATA_CHIP)

        simpleItem(ModItems.AMMO_PERK_DATA_CHIP)
        simpleItem(ModItems.FUNCTIONAL_PERK_DATA_CHIP)
        simpleItem(ModItems.DAMAGE_PERK_DATA_CHIP)

        simpleItem(ModItems.DIRECTIONAL_RESEARCH_MODULE)
        simpleItem(ModItems.ENLARGEMENT_RESEARCH_MODULE)
        simpleItem(ModItems.EFFECTIVE_RESEARCH_MODULE)
        simpleItem(ModItems.BOOST_RESEARCH_MODULE)

        // perk
        simpleItem(ModItems.SHORTCUT_PACK)
        simpleItem(ModItems.EMPTY_PERK)
        ModPerks.AMMO_PERKS.entries.forEach {
            simpleItem(ModItems.PERK_ITEMS[it]!!)
        }
        ModPerks.FUNC_PERKS.entries.forEach {
            simpleItem(ModItems.PERK_ITEMS[it]!!)
        }
        ModPerks.DAMAGE_PERKS.entries.forEach {
            simpleItem(ModItems.PERK_ITEMS[it]!!)
        }

        // armor
        simpleItem(ModItems.RU_HELMET_6B47)
        simpleItem(ModItems.RU_CHEST_6B43)
        simpleItem(ModItems.US_HELMET_PASGT)
        simpleItem(ModItems.US_CHEST_IOTV)
        simpleItem(ModItems.GE_HELMET_M_35)

        // blueprints
        gunBlueprintItem(ModItems.TRACHELIUM_BLUEPRINT)
        gunBlueprintItem(ModItems.GLOCK_17_BLUEPRINT)
        gunBlueprintItem(ModItems.GLOCK_18_BLUEPRINT)
        gunBlueprintItem(ModItems.MP_443_BLUEPRINT)
        gunBlueprintItem(ModItems.HUNTING_RIFLE_BLUEPRINT)
        gunBlueprintItem(ModItems.M_79_BLUEPRINT)
        gunBlueprintItem(ModItems.RPG_BLUEPRINT)
        gunBlueprintItem(ModItems.BOCEK_BLUEPRINT)
        gunBlueprintItem(ModItems.M_4_BLUEPRINT)
        gunBlueprintItem(ModItems.AA_12_BLUEPRINT)
        gunBlueprintItem(ModItems.HK_416_BLUEPRINT)
        gunBlueprintItem(ModItems.RPK_BLUEPRINT)
        gunBlueprintItem(ModItems.SKS_BLUEPRINT)
        gunBlueprintItem(ModItems.NTW_20_BLUEPRINT)
        gunBlueprintItem(ModItems.VECTOR_BLUEPRINT)
        gunBlueprintItem(ModItems.MINIGUN_BLUEPRINT)
        gunBlueprintItem(ModItems.MK_14_BLUEPRINT)
        gunBlueprintItem(ModItems.SENTINEL_BLUEPRINT)
        gunBlueprintItem(ModItems.M_60_BLUEPRINT)
        gunBlueprintItem(ModItems.SVD_BLUEPRINT)
        gunBlueprintItem(ModItems.MARLIN_BLUEPRINT)
        gunBlueprintItem(ModItems.M_870_BLUEPRINT)
        gunBlueprintItem(ModItems.AWM_BLUEPRINT)
        gunBlueprintItem(ModItems.M_98B_BLUEPRINT)
        gunBlueprintItem(ModItems.AK_12_BLUEPRINT)
        gunBlueprintItem(ModItems.AK_47_BLUEPRINT)
        gunBlueprintItem(ModItems.DEVOTION_BLUEPRINT)
        gunBlueprintItem(ModItems.TASER_BLUEPRINT)
        gunBlueprintItem(ModItems.M_1911_BLUEPRINT)
        gunBlueprintItem(ModItems.QBZ_95_BLUEPRINT)
        gunBlueprintItem(ModItems.K_98_BLUEPRINT)
        gunBlueprintItem(ModItems.MOSIN_NAGANT_BLUEPRINT)
        gunBlueprintItem(ModItems.JAVELIN_BLUEPRINT)
        cannonBlueprintItem(ModItems.MK_42_BLUEPRINT)
        cannonBlueprintItem(ModItems.MLE_1934_BLUEPRINT)
        cannonBlueprintItem(ModItems.ANNIHILATOR_BLUEPRINT)
        cannonBlueprintItem(ModItems.HPJ_11_BLUEPRINT)
        cannonBlueprintItem(ModItems.BL_132_BLUEPRINT)
        gunBlueprintItem(ModItems.M_2_HB_BLUEPRINT)
        gunBlueprintItem(ModItems.SECONDARY_CATACLYSM_BLUEPRINT)
        gunBlueprintItem(ModItems.INSIDIOUS_BLUEPRINT)
        gunBlueprintItem(ModItems.MP_5_BLUEPRINT)
        gunBlueprintItem(ModItems.QBZ_191_BLUEPRINT)
        gunBlueprintItem(ModItems.IGLA_BLUEPRINT)
        gunBlueprintItem(ModItems.QL_1031_BLUEPRINT)
        gunBlueprintItem(ModItems.SUPER_STAR_SHOOTER_BLUEPRINT)

        // blocks
        evenSimplerBlockItem(ModBlocks.BARBED_WIRE)
        evenSimplerBlockItem(ModBlocks.JUMP_PAD)
        evenSimplerBlockItem(ModBlocks.REFORGING_TABLE)
        evenSimplerBlockItem(ModBlocks.CHARGING_STATION)
        evenSimplerBlockItem(ModBlocks.CREATIVE_CHARGING_STATION)
        evenSimplerBlockItem(ModBlocks.VEHICLE_DEPLOYER)
        evenSimplerBlockItem(ModBlocks.AIRCRAFT_CATAPULT)
        evenSimplerBlockItem(ModBlocks.SUPERB_ITEM_INTERFACE)
        evenSimplerBlockItem(ModBlocks.CREATIVE_SUPERB_ITEM_INTERFACE)
        evenSimplerBlockItem(ModBlocks.BIOGAS_GENERATOR)
        simpleItem(ModItems.FUMO_25)
    }

    private fun simpleMaterials(materials: Materials) {
        simpleItem(materials.action)
        simpleItem(materials.barrel)
        simpleItem(materials.trigger)
        simpleItem(materials.spring)
    }

    private fun simpleItem(item: DeferredHolder<Item, out Item>, location: String = ""): ItemModelBuilder {
        return withExistingParent(item.id.path, ResourceLocation.withDefaultNamespace("item/generated"))
            .texture("layer0", loc("item/" + location + item.id.path))
    }

    private fun simpleItem(
        item: DeferredHolder<Item, out Item>,
        location: String,
        renderType: String
    ): ItemModelBuilder {
        return withExistingParent(item.id.path, ResourceLocation.withDefaultNamespace("item/generated"))
            .texture("layer0", loc("item/" + location + item.id.path)).renderType(renderType)
    }

    fun <T : Block> evenSimplerBlockItem(block: DeferredHolder<Block, T>) {
        this.withExistingParent(
            Mod.MODID + ":" + BuiltInRegistries.BLOCK.getKey(block.get()).path,
            modLoc("block/" + BuiltInRegistries.BLOCK.getKey(block.get()).path)
        )
    }

    private fun gunBlueprintItem(item: DeferredHolder<Item, out Item>): ItemModelBuilder {
        return withExistingParent(item.id.path, ResourceLocation.withDefaultNamespace("item/generated"))
            .texture("layer0", loc("item/gun_blueprint"))
    }

    private fun cannonBlueprintItem(item: DeferredHolder<Item, out Item>): ItemModelBuilder {
        return withExistingParent(item.id.path, ResourceLocation.withDefaultNamespace("item/generated"))
            .texture("layer0", loc("item/cannon_blueprint"))
    }

    private fun handheldItem(item: DeferredHolder<Item, out Item>): ItemModelBuilder {
        return withExistingParent(item.id.path, ResourceLocation.withDefaultNamespace("item/handheld"))
            .texture("layer0", loc("item/" + item.id.path))
    }

    private fun gunIcon(item: DeferredHolder<Item, out Item>, name: String): ItemModelBuilder {
        return withExistingParent(item.id.path + "_icon", ResourceLocation.withDefaultNamespace("item/generated"))
            .texture("layer0", loc("item/" + name + "_icon"))
    }

    private fun gunBase(item: DeferredHolder<Item, out Item>, name: String): ItemModelBuilder {
        return getBuilder(item.id.path + "_base")
            .parent(ModelFile.UncheckedModelFile(modLoc("displaysettings/$name.item")))
            .texture("layer0", loc("item/$name"))
    }

    private fun customSeparatedGunModel(item: DeferredHolder<Item, out Item>, name: String): ItemModelBuilder {
        val base = modLoc("item/" + name + "_base").toString()
        val icon = modLoc("item/" + name + "_icon").toString()

        return getBuilder(item.id.path)
            .guiLight(BlockModel.GuiLight.FRONT)
            .customLoader { parent, existingFileHelper ->
                CustomSeparateModelBuilder.begin(parent, existingFileHelper)
            }
            .base(base)
            .perspective(ItemDisplayContext.GUI, icon)
            .texture("particle", modLoc("item/" + name + "_icon"))
            .end()
    }

    @JvmOverloads
    fun gunItem(item: DeferredHolder<Item, out Item>, name: String = item.id.path) {
        this.gunIcon(item, name)
        this.gunBase(item, name)
        this.customSeparatedGunModel(item, name)
    }
}
