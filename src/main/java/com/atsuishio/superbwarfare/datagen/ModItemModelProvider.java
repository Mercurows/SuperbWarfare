package com.atsuishio.superbwarfare.datagen;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModBlocks;
import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings({"ConstantConditions", "UnusedReturnValue", "SameParameterValue", "unused"})
public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Mod.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // gun
        gunItem(ModItems.AA_12);
        gunItem(ModItems.AK_12);
        gunItem(ModItems.AK_47);
        gunItem(ModItems.AURELIA_SCEPTRE);
        gunItem(ModItems.BOCEK);
        gunItem(ModItems.DEVOTION);
        gunItem(ModItems.GLOCK_17);
        gunItem(ModItems.GLOCK_18, "glock_17");
        gunItem(ModItems.HK_416);
        gunItem(ModItems.HOMEMADE_SHOTGUN);
        gunItem(ModItems.HUNTING_RIFLE);
        gunItem(ModItems.INSIDIOUS);
        gunItem(ModItems.JAVELIN);
        gunItem(ModItems.K_98);
        gunItem(ModItems.M_4);
        gunItem(ModItems.M_60);
        gunItem(ModItems.M_79);
        gunItem(ModItems.M_1911);
        gunItem(ModItems.M_870);
        gunItem(ModItems.M_98B);
        gunItem(ModItems.MARLIN);
        gunItem(ModItems.MINIGUN);
        gunItem(ModItems.MK_14);
        gunItem(ModItems.MOSIN_NAGANT);
        gunItem(ModItems.MP_443);
        gunItem(ModItems.NTW_20);
        gunItem(ModItems.QBZ_95);
        gunItem(ModItems.RPG);
        gunItem(ModItems.RPK);
        gunItem(ModItems.SECONDARY_CATACLYSM);
        gunItem(ModItems.SENTINEL);
        gunItem(ModItems.SKS);
        gunItem(ModItems.SVD);
        gunItem(ModItems.TASER);
        gunItem(ModItems.TRACHELIUM);
        gunItem(ModItems.VECTOR);
        gunItem(ModItems.MP_5);

        simpleItem(ModItems.EMPTY_PERK, "perk/");

        simpleItem(ModItems.MORTAR_SHELL);

        // misc
        simpleItem(ModItems.ANCIENT_CPU);
        simpleItem(ModItems.PROPELLER);
        simpleItem(ModItems.LARGE_PROPELLER);
        simpleItem(ModItems.MOTOR);
        simpleItem(ModItems.LARGE_MOTOR);
        simpleItem(ModItems.WHEEL);
        simpleItem(ModItems.TRACK);
        simpleItem(ModItems.DRONE);
        simpleItem(ModItems.LIGHT_ARMAMENT_MODULE);
        simpleItem(ModItems.MEDIUM_ARMAMENT_MODULE);
        simpleItem(ModItems.HEAVY_ARMAMENT_MODULE);

        simpleItem(ModItems.TARGET_DEPLOYER);
        simpleItem(ModItems.DPS_GENERATOR_DEPLOYER);
        simpleItem(ModItems.MORTAR_DEPLOYER);
        simpleItem(ModItems.MORTAR_BARREL);
        simpleItem(ModItems.MORTAR_BASE_PLATE);
        simpleItem(ModItems.MORTAR_BIPOD);
        simpleItem(ModItems.SEEKER);
        simpleItem(ModItems.MISSILE_ENGINE);
        simpleItem(ModItems.FUSEE);
        simpleItem(ModItems.PRIMER);
        simpleItem(ModItems.BLU_43_MINE);
        simpleItem(ModItems.AP_HEAD);
        simpleItem(ModItems.HE_HEAD);
        simpleItem(ModItems.CANNON_CORE);
        simpleItem(ModItems.COPPER_PLATE);
        simpleItem(ModItems.STEEL_INGOT);
        simpleItem(ModItems.LEAD_INGOT);
        simpleItem(ModItems.TUNGSTEN_INGOT);
        simpleItem(ModItems.CEMENTED_CARBIDE_INGOT);
        simpleItem(ModItems.HIGH_ENERGY_EXPLOSIVES);
        simpleItem(ModItems.GRAIN);
        simpleItem(ModItems.IRON_POWDER);
        simpleItem(ModItems.TUNGSTEN_POWDER);
        simpleItem(ModItems.COAL_POWDER);
        simpleItem(ModItems.COAL_IRON_POWDER);
        simpleItem(ModItems.RAW_CEMENTED_CARBIDE_POWDER);
        simpleItem(ModItems.GALENA);
        simpleItem(ModItems.SCHEELITE);
        simpleItem(ModItems.DOG_TAG);
        simpleItem(ModItems.TRANSCRIPT);
        simpleItem(ModItems.RAW_SILVER);
        simpleItem(ModItems.SILVER_INGOT);
        handheldItem(ModItems.BEAST);
        handheldItem(ModItems.CROWBAR);
        handheldItem(ModItems.DEFUSER);
        simpleItem(ModItems.FIRING_PARAMETERS);
        simpleItem(ModItems.BEAM_TEST);
        simpleItem(ModItems.HANDGUN_AMMO);
        simpleItem(ModItems.RIFLE_AMMO);
        simpleItem(ModItems.SNIPER_AMMO);
        simpleItem(ModItems.SHOTGUN_AMMO);
        simpleItem(ModItems.HEAVY_AMMO);
        simpleItem(ModItems.ROCKET_70);
        simpleItem(ModItems.WIRE_GUIDE_MISSILE);
        simpleItem(ModItems.AGM);
        simpleItem(ModItems.SMALL_SHELL);
        simpleItem(ModItems.SWARM_DRONE);
        simpleItem(ModItems.MEDIUM_AERIAL_BOMB);
        simpleItem(ModItems.SMALL_BATTERY_PACK);
        simpleItem(ModItems.MEDIUM_BATTERY_PACK);
        simpleItem(ModItems.LARGE_BATTERY_PACK);

        simpleItem(ModItems.TUNGSTEN_ROD);
        simpleItem(ModItems.IRON_BARREL);
        simpleItem(ModItems.IRON_ACTION);
        simpleItem(ModItems.IRON_TRIGGER);
        simpleItem(ModItems.IRON_SPRING);
        simpleItem(ModItems.STEEL_BARREL);
        simpleItem(ModItems.STEEL_ACTION);
        simpleItem(ModItems.STEEL_TRIGGER);
        simpleItem(ModItems.STEEL_SPRING);
        simpleItem(ModItems.CEMENTED_CARBIDE_BARREL);
        simpleItem(ModItems.CEMENTED_CARBIDE_ACTION);
        simpleItem(ModItems.CEMENTED_CARBIDE_TRIGGER);
        simpleItem(ModItems.CEMENTED_CARBIDE_SPRING);
        simpleItem(ModItems.NETHERITE_BARREL);
        simpleItem(ModItems.NETHERITE_ACTION);
        simpleItem(ModItems.NETHERITE_TRIGGER);
        simpleItem(ModItems.NETHERITE_SPRING);

        simpleItem(ModItems.COMMON_MATERIAL_PACK);
        simpleItem(ModItems.RARE_MATERIAL_PACK);
        simpleItem(ModItems.EPIC_MATERIAL_PACK);
        simpleItem(ModItems.LEGENDARY_MATERIAL_PACK);

        // armor
        simpleItem(ModItems.RU_HELMET_6B47);
        simpleItem(ModItems.RU_CHEST_6B43);
        simpleItem(ModItems.US_HELMET_PASTG);
        simpleItem(ModItems.US_CHEST_IOTV);
        simpleItem(ModItems.GE_HELMET_M_35);

        // blueprints
        gunBlueprintItem(ModItems.TRACHELIUM_BLUEPRINT);
        gunBlueprintItem(ModItems.GLOCK_17_BLUEPRINT);
        gunBlueprintItem(ModItems.GLOCK_18_BLUEPRINT);
        gunBlueprintItem(ModItems.MP_443_BLUEPRINT);
        gunBlueprintItem(ModItems.HUNTING_RIFLE_BLUEPRINT);
        gunBlueprintItem(ModItems.M_79_BLUEPRINT);
        gunBlueprintItem(ModItems.RPG_BLUEPRINT);
        gunBlueprintItem(ModItems.BOCEK_BLUEPRINT);
        gunBlueprintItem(ModItems.M_4_BLUEPRINT);
        gunBlueprintItem(ModItems.AA_12_BLUEPRINT);
        gunBlueprintItem(ModItems.HK_416_BLUEPRINT);
        gunBlueprintItem(ModItems.RPK_BLUEPRINT);
        gunBlueprintItem(ModItems.SKS_BLUEPRINT);
        gunBlueprintItem(ModItems.NTW_20_BLUEPRINT);
        gunBlueprintItem(ModItems.VECTOR_BLUEPRINT);
        gunBlueprintItem(ModItems.MINIGUN_BLUEPRINT);
        gunBlueprintItem(ModItems.MK_14_BLUEPRINT);
        gunBlueprintItem(ModItems.SENTINEL_BLUEPRINT);
        gunBlueprintItem(ModItems.M_60_BLUEPRINT);
        gunBlueprintItem(ModItems.SVD_BLUEPRINT);
        gunBlueprintItem(ModItems.MARLIN_BLUEPRINT);
        gunBlueprintItem(ModItems.M_870_BLUEPRINT);
        gunBlueprintItem(ModItems.M_98B_BLUEPRINT);
        gunBlueprintItem(ModItems.AK_12_BLUEPRINT);
        gunBlueprintItem(ModItems.AK_47_BLUEPRINT);
        gunBlueprintItem(ModItems.DEVOTION_BLUEPRINT);
        gunBlueprintItem(ModItems.TASER_BLUEPRINT);
        gunBlueprintItem(ModItems.M_1911_BLUEPRINT);
        gunBlueprintItem(ModItems.QBZ_95_BLUEPRINT);
        gunBlueprintItem(ModItems.K_98_BLUEPRINT);
        gunBlueprintItem(ModItems.MOSIN_NAGANT_BLUEPRINT);
        gunBlueprintItem(ModItems.JAVELIN_BLUEPRINT);
        gunBlueprintItem(ModItems.AURELIA_SCEPTRE_BLUEPRINT);
        cannonBlueprintItem(ModItems.MK_42_BLUEPRINT);
        cannonBlueprintItem(ModItems.MLE_1934_BLUEPRINT);
        cannonBlueprintItem(ModItems.ANNIHILATOR_BLUEPRINT);
        cannonBlueprintItem(ModItems.HPJ_11_BLUEPRINT);
        gunBlueprintItem(ModItems.M_2_HB_BLUEPRINT);
        gunBlueprintItem(ModItems.SECONDARY_CATACLYSM_BLUEPRINT);
        gunBlueprintItem(ModItems.INSIDIOUS_BLUEPRINT);
        gunBlueprintItem(ModItems.MP_5_BLUEPRINT);

        // blocks
        evenSimplerBlockItem(ModBlocks.BARBED_WIRE);
        evenSimplerBlockItem(ModBlocks.JUMP_PAD);
        evenSimplerBlockItem(ModBlocks.REFORGING_TABLE);
        evenSimplerBlockItem(ModBlocks.CHARGING_STATION);
        evenSimplerBlockItem(ModBlocks.CREATIVE_CHARGING_STATION);
        evenSimplerBlockItem(ModBlocks.VEHICLE_DEPLOYER);
        evenSimplerBlockItem(ModBlocks.AIRCRAFT_CATAPULT);
    }

    private ItemModelBuilder simpleItem(RegistryObject<Item> item) {
        return simpleItem(item, "");
    }

    private ItemModelBuilder simpleItem(RegistryObject<Item> item, String location) {
        return withExistingParent(item.getId().getPath(), new ResourceLocation("item/generated"))
                .texture("layer0", Mod.loc("item/" + location + item.getId().getPath()));
    }

    private ItemModelBuilder simpleItem(RegistryObject<Item> item, String location, String renderType) {
        return withExistingParent(item.getId().getPath(), new ResourceLocation("item/generated"))
                .texture("layer0", Mod.loc("item/" + location + item.getId().getPath())).renderType(renderType);
    }

    public void evenSimplerBlockItem(RegistryObject<Block> block) {
        this.withExistingParent(Mod.MODID + ":" + ForgeRegistries.BLOCKS.getKey(block.get()).getPath(),
                modLoc("block/" + ForgeRegistries.BLOCKS.getKey(block.get()).getPath()));
    }

    private ItemModelBuilder gunBlueprintItem(RegistryObject<Item> item) {
        return withExistingParent(item.getId().getPath(), new ResourceLocation("item/generated"))
                .texture("layer0", Mod.loc("item/gun_blueprint"));
    }

    private ItemModelBuilder cannonBlueprintItem(RegistryObject<Item> item) {
        return withExistingParent(item.getId().getPath(), new ResourceLocation("item/generated"))
                .texture("layer0", Mod.loc("item/cannon_blueprint"));
    }

    private ItemModelBuilder handheldItem(RegistryObject<Item> item) {
        return withExistingParent(item.getId().getPath(), new ResourceLocation("item/handheld"))
                .texture("layer0", Mod.loc("item/" + item.getId().getPath()));
    }

    private ItemModelBuilder gunIcon(RegistryObject<Item> item, String name) {
        return withExistingParent(item.getId().getPath() + "_icon", new ResourceLocation("item/generated"))
                .texture("layer0", Mod.loc("item/" + name + "_icon"));
    }

    private ItemModelBuilder gunBase(RegistryObject<Item> item, String name) {
        return getBuilder(item.getId().getPath() + "_base")
                .parent(new ModelFile.UncheckedModelFile(modLoc("displaysettings/" + name + ".item")))
                .texture("layer0", Mod.loc("item/" + name));
    }

    private ItemModelBuilder customSeparatedGunModel(RegistryObject<Item> item, String name) {
        String lod = modLoc("lod/" + name).toString();
        String base = modLoc("item/" + name + "_base").toString();
        String icon = modLoc("item/" + name + "_icon").toString();

        return getBuilder(item.getId().getPath())
                .guiLight(BlockModel.GuiLight.FRONT)
                .customLoader(CustomSeparateModelBuilder::begin)
                .base(base)
//                .perspective(ItemDisplayContext.FIXED, lod)
//                .perspective(ItemDisplayContext.HEAD, lod)
//                .perspective(ItemDisplayContext.GROUND, lod)
//                .perspective(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, lod)
//                .perspective(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, lod)
                .perspective(ItemDisplayContext.GUI, icon)
                .end();
    }

    public void gunItem(RegistryObject<Item> item) {
        this.gunItem(item, item.getId().getPath());
    }

    public void gunItem(RegistryObject<Item> item, String name) {
        this.gunIcon(item, name);
        this.gunBase(item, name);
        this.customSeparatedGunModel(item, name);
    }
}
