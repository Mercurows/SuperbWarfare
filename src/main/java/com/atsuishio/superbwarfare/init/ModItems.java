package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity;
import com.atsuishio.superbwarfare.item.*;
import com.atsuishio.superbwarfare.item.armor.*;
import com.atsuishio.superbwarfare.item.common.BlueprintItem;
import com.atsuishio.superbwarfare.item.common.MaterialPack;
import com.atsuishio.superbwarfare.item.common.ammo.*;
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
import com.atsuishio.superbwarfare.item.common.container.LuckyContainerBlockItem;
import com.atsuishio.superbwarfare.item.common.container.SmallContainerBlockItem;
import com.atsuishio.superbwarfare.item.gun.handgun.*;
import com.atsuishio.superbwarfare.item.gun.heavy.Ntw20Item;
import com.atsuishio.superbwarfare.item.gun.launcher.JavelinItem;
import com.atsuishio.superbwarfare.item.gun.launcher.M79Item;
import com.atsuishio.superbwarfare.item.gun.launcher.RpgItem;
import com.atsuishio.superbwarfare.item.gun.launcher.SecondaryCataclysm;
import com.atsuishio.superbwarfare.item.gun.machinegun.*;
import com.atsuishio.superbwarfare.item.gun.rifle.*;
import com.atsuishio.superbwarfare.item.gun.shotgun.Aa12Item;
import com.atsuishio.superbwarfare.item.gun.shotgun.HomemadeShotgunItem;
import com.atsuishio.superbwarfare.item.gun.shotgun.M870Item;
import com.atsuishio.superbwarfare.item.gun.smg.Mp5Item;
import com.atsuishio.superbwarfare.item.gun.smg.VectorItem;
import com.atsuishio.superbwarfare.item.gun.sniper.*;
import com.atsuishio.superbwarfare.item.gun.special.BocekItem;
import com.atsuishio.superbwarfare.item.gun.special.TaserItem;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tiers.ModItemTier;
import com.atsuishio.superbwarfare.tools.Ammo;
import com.atsuishio.superbwarfare.tools.RarityTool;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class ModItems {
    /**
     * guns
     */
    public static final DeferredRegister<Item> GUNS = DeferredRegister.create(ForgeRegistries.ITEMS, Mod.MODID);

    public static final RegistryObject<Item> TASER = GUNS.register("taser", TaserItem::new);
    public static final RegistryObject<Item> GLOCK_17 = GUNS.register("glock_17", Glock17Item::new);
    public static final RegistryObject<Item> GLOCK_18 = GUNS.register("glock_18", Glock18Item::new);
    public static final RegistryObject<Item> MP_443 = GUNS.register("mp_443", Mp443Item::new);
    public static final RegistryObject<Item> M_1911 = GUNS.register("m_1911", M1911Item::new);
    public static final RegistryObject<Item> HOMEMADE_SHOTGUN = GUNS.register("homemade_shotgun", HomemadeShotgunItem::new);
    public static final RegistryObject<Item> TRACHELIUM = GUNS.register("trachelium", Trachelium::new);
    public static final RegistryObject<Item> MP_5 = GUNS.register("mp_5", Mp5Item::new);
    public static final RegistryObject<Item> VECTOR = GUNS.register("vector", VectorItem::new);
    public static final RegistryObject<Item> AK_47 = GUNS.register("ak_47", AK47Item::new);
    public static final RegistryObject<Item> AK_12 = GUNS.register("ak_12", AK12Item::new);
    public static final RegistryObject<Item> SKS = GUNS.register("sks", SksItem::new);
    public static final RegistryObject<Item> M_4 = GUNS.register("m_4", M4Item::new);
    public static final RegistryObject<Item> HK_416 = GUNS.register("hk_416", Hk416Item::new);
    public static final RegistryObject<Item> QBZ_95 = GUNS.register("qbz_95", Qbz95Item::new);
    public static final RegistryObject<Item> QBZ_191 = GUNS.register("qbz_191", Qbz191Item::new);
    public static final RegistryObject<Item> INSIDIOUS = GUNS.register("insidious", InsidiousItem::new);
    public static final RegistryObject<Item> MK_14 = GUNS.register("mk_14", Mk14Item::new);
    public static final RegistryObject<Item> MARLIN = GUNS.register("marlin", MarlinItem::new);
    public static final RegistryObject<Item> K_98 = GUNS.register("k_98", K98Item::new);
    public static final RegistryObject<Item> MOSIN_NAGANT = GUNS.register("mosin_nagant", MosinNagantItem::new);
    public static final RegistryObject<Item> SVD = GUNS.register("svd", SvdItem::new);
    public static final RegistryObject<Item> M_98B = GUNS.register("m_98b", M98bItem::new);
    public static final RegistryObject<Item> SENTINEL = GUNS.register("sentinel", SentinelItem::new);
    public static final RegistryObject<Item> HUNTING_RIFLE = GUNS.register("hunting_rifle", HuntingRifleItem::new);
    public static final RegistryObject<Item> NTW_20 = GUNS.register("ntw_20", Ntw20Item::new);
    public static final RegistryObject<Item> M_870 = GUNS.register("m_870", M870Item::new);
    public static final RegistryObject<Item> AA_12 = GUNS.register("aa_12", Aa12Item::new);
    public static final RegistryObject<Item> DEVOTION = GUNS.register("devotion", DevotionItem::new);
    public static final RegistryObject<Item> RPK = GUNS.register("rpk", RpkItem::new);
    public static final RegistryObject<Item> M_60 = GUNS.register("m_60", M60Item::new);
    public static final RegistryObject<Item> M_2_HB = GUNS.register("m_2_hb", M2HBItem::new);
    public static final RegistryObject<Item> MINIGUN = GUNS.register("minigun", MinigunItem::new);
    public static final RegistryObject<Item> M_79 = GUNS.register("m_79", M79Item::new);
    public static final RegistryObject<Item> SECONDARY_CATACLYSM = GUNS.register("secondary_cataclysm", SecondaryCataclysm::new);
    public static final RegistryObject<Item> RPG = GUNS.register("rpg", RpgItem::new);
    public static final RegistryObject<Item> JAVELIN = GUNS.register("javelin", JavelinItem::new);
    public static final RegistryObject<Item> AURELIA_SCEPTRE = GUNS.register("aurelia_sceptre", AureliaSceptre::new);
    public static final RegistryObject<Item> BOCEK = GUNS.register("bocek", BocekItem::new);

    /**
     * Ammo
     */
    public static final DeferredRegister<Item> AMMO = DeferredRegister.create(ForgeRegistries.ITEMS, Mod.MODID);

    public static final RegistryObject<Item> HANDGUN_AMMO = AMMO.register("handgun_ammo", () -> new AmmoSupplierItem(Ammo.HANDGUN, 1, new Item.Properties()));
    public static final RegistryObject<Item> RIFLE_AMMO = AMMO.register("rifle_ammo", () -> new AmmoSupplierItem(Ammo.RIFLE, 1, new Item.Properties()));
    public static final RegistryObject<Item> SNIPER_AMMO = AMMO.register("sniper_ammo", () -> new AmmoSupplierItem(Ammo.SNIPER, 1, new Item.Properties()));
    public static final RegistryObject<Item> SHOTGUN_AMMO = AMMO.register("shotgun_ammo", () -> new AmmoSupplierItem(Ammo.SHOTGUN, 1, new Item.Properties()));
    public static final RegistryObject<Item> HEAVY_AMMO = AMMO.register("heavy_ammo", () -> new AmmoSupplierItem(Ammo.HEAVY, 1, new Item.Properties()));
    public static final RegistryObject<Item> HANDGUN_AMMO_BOX = AMMO.register("handgun_ammo_box", HandgunAmmoBox::new);
    public static final RegistryObject<Item> RIFLE_AMMO_BOX = AMMO.register("rifle_ammo_box", RifleAmmoBox::new);
    public static final RegistryObject<Item> SNIPER_AMMO_BOX = AMMO.register("sniper_ammo_box", SniperAmmoBox::new);
    public static final RegistryObject<Item> SHOTGUN_AMMO_BOX = AMMO.register("shotgun_ammo_box", ShotgunAmmoBox::new);
    public static final RegistryObject<Item> CREATIVE_AMMO_BOX = AMMO.register("creative_ammo_box", CreativeAmmoBox::new);
    public static final RegistryObject<Item> AMMO_BOX = AMMO.register("ammo_box", AmmoBox::new);
    public static final RegistryObject<Item> TASER_ELECTRODE = AMMO.register("taser_electrode", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GRENADE_40MM = AMMO.register("grenade_40mm", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> JAVELIN_MISSILE = AMMO.register("javelin_missile", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MORTAR_SHELL = AMMO.register("mortar_shell", MortarShell::new);
    public static final RegistryObject<Item> POTION_MORTAR_SHELL = AMMO.register("potion_mortar_shell", PotionMortarShell::new);
    public static final RegistryObject<Item> ROCKET = AMMO.register("rocket", RocketItem::new);
    public static final RegistryObject<Item> LUNGE_MINE = AMMO.register("lunge_mine", LungeMine::new);
    public static final RegistryObject<Item> HE_5_INCHES = AMMO.register("he_5_inches", () -> new CannonShellItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> AP_5_INCHES = AMMO.register("ap_5_inches", () -> new CannonShellItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> CM_5_INCHES = AMMO.register("cm_5_inches", () -> new CannonShellItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> HAND_GRENADE = AMMO.register("hand_grenade", HandGrenade::new);
    public static final RegistryObject<Item> RGO_GRENADE = AMMO.register("rgo_grenade", RgoGrenade::new);
    public static final RegistryObject<Item> M18_SMOKE_GRENADE = AMMO.register("m18_smoke_grenade", M18SmokeGrenade::new);
    public static final RegistryObject<Item> CLAYMORE_MINE = AMMO.register("claymore_mine", ClaymoreMine::new);
    public static final RegistryObject<Item> TM_62 = AMMO.register("tm_62", Tm62Item::new);
    public static final RegistryObject<Item> C4_BOMB = AMMO.register("c4_bomb", C4BombItem::new);
    public static final RegistryObject<Item> BLU_43_MINE = AMMO.register("blu_43_mine", Blu43MineItem::new);
    public static final RegistryObject<Item> SMALL_SHELL = AMMO.register("small_shell", SmallShellItem::new);
    public static final RegistryObject<Item> SMALL_ROCKET = AMMO.register("small_rocket", SmallRocketItem::new);
    public static final RegistryObject<Item> MEDIUM_ROCKET_AP = AMMO.register("medium_rocket_ap", () -> new MediumRocketItem(500, 6, 100, 0, 0, MediumRocketEntity.Type.AP, 0));
    public static final RegistryObject<Item> MEDIUM_ROCKET_HE = AMMO.register("medium_rocket_he", () -> new MediumRocketItem(200, 12, 200, 0.2f, 40, MediumRocketEntity.Type.HE, 0));
    public static final RegistryObject<Item> MEDIUM_ROCKET_CM = AMMO.register("medium_rocket_cm", () -> new MediumRocketItem(300, 12, 300, 0, 0, MediumRocketEntity.Type.CM, 20));
    public static final RegistryObject<Item> WIRE_GUIDE_MISSILE = AMMO.register("wire_guide_missile", WireGuideMissileItem::new);
    public static final RegistryObject<Item> AGM = AMMO.register("agm", AgmItem::new);
    public static final RegistryObject<Item> SWARM_DRONE = AMMO.register("swarm_drone", SwarmDroneItem::new);
    public static final RegistryObject<Item> MEDIUM_AERIAL_BOMB = AMMO.register("medium_aerial_bomb", MediumAerialBombItem::new);
    public static final RegistryObject<Item> BEAM_TEST = AMMO.register("beam_test", BeamTest::new);

    /**
     * items
     */
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Mod.MODID);

    public static final RegistryObject<Item> SENPAI_SPAWN_EGG = ITEMS.register("senpai_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.SENPAI, -11584987, -14014413, new Item.Properties()));
    public static final RegistryObject<Item> ANCIENT_CPU = ITEMS.register("ancient_cpu", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> PROPELLER = ITEMS.register("propeller", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LARGE_PROPELLER = ITEMS.register("large_propeller", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MOTOR = ITEMS.register("motor", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LARGE_MOTOR = ITEMS.register("large_motor", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WHEEL = ITEMS.register("wheel", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TRACK = ITEMS.register("track", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DRONE = ITEMS.register("drone", Drone::new);

    public static final RegistryObject<Item> MONITOR = ITEMS.register("monitor", Monitor::new);
    public static final RegistryObject<Item> ARTILLERY_INDICATOR = ITEMS.register("artillery_indicator", ArtilleryIndicator::new);

    public static final RegistryObject<Item> DETONATOR = ITEMS.register("detonator", Detonator::new);
    public static final RegistryObject<Item> TARGET_DEPLOYER = ITEMS.register("target_deployer", TargetDeployer::new);
    public static final RegistryObject<Item> DPS_GENERATOR_DEPLOYER = ITEMS.register("dps_generator_deployer", DPSGeneratorDeployer::new);
    public static final RegistryObject<Item> KNIFE = ITEMS.register("knife", () -> new SwordItem(ModItemTier.STEEL, 0, -1.8f, new Item.Properties().durability(1200)));
    public static final RegistryObject<Item> HAMMER = ITEMS.register("hammer", Hammer::new);
    public static final RegistryObject<Item> T_BATON = ITEMS.register("t_baton", TBaton::new);
    public static final RegistryObject<Item> ELECTRIC_BATON = ITEMS.register("electric_baton", ElectricBaton::new);
    public static final RegistryObject<Item> STEEL_PIPE = ITEMS.register("steel_pipe", SteelPipe::new);
    public static final RegistryObject<Item> CROWBAR = ITEMS.register("crowbar", Crowbar::new);
    public static final RegistryObject<Item> DEFUSER = ITEMS.register("defuser", Defuser::new);
    public static final RegistryObject<Item> ARMOR_PLATE = ITEMS.register("armor_plate", ArmorPlate::new);

    public static final RegistryObject<Item> RU_HELMET_6B47 = ITEMS.register("ru_helmet_6b47", RuHelmet6b47::new);
    public static final RegistryObject<Item> RU_CHEST_6B43 = ITEMS.register("ru_chest_6b43", RuChest6b43::new);
    public static final RegistryObject<Item> US_HELMET_PASTG = ITEMS.register("us_helmet_pastg", UsHelmetPastg::new);
    public static final RegistryObject<Item> US_CHEST_IOTV = ITEMS.register("us_chest_iotv", UsChestIotv::new);
    public static final RegistryObject<Item> GE_HELMET_M_35 = ITEMS.register("ge_helmet_m_35", GeHelmetM35::new);
    public static final RegistryObject<Item> MORTAR_DEPLOYER = ITEMS.register("mortar_deployer", MortarDeployer::new);
    public static final RegistryObject<Item> MORTAR_BARREL = ITEMS.register("mortar_barrel", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MORTAR_BASE_PLATE = ITEMS.register("mortar_base_plate", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MORTAR_BIPOD = ITEMS.register("mortar_bipod", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SEEKER = ITEMS.register("seeker", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MISSILE_ENGINE = ITEMS.register("missile_engine", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FUSEE = ITEMS.register("fusee", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PRIMER = ITEMS.register("primer", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> AP_HEAD = ITEMS.register("ap_head", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HE_HEAD = ITEMS.register("he_head", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CM_HEAD = ITEMS.register("cm_head", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CANNON_CORE = ITEMS.register("cannon_core", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COPPER_PLATE = ITEMS.register("copper_plate", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STEEL_INGOT = ITEMS.register("steel_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LEAD_INGOT = ITEMS.register("lead_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SILVER_INGOT = ITEMS.register("silver_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TUNGSTEN_INGOT = ITEMS.register("tungsten_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CEMENTED_CARBIDE_INGOT = ITEMS.register("cemented_carbide_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HIGH_ENERGY_EXPLOSIVES = ITEMS.register("high_energy_explosives", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GRAIN = ITEMS.register("grain", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> IRON_POWDER = ITEMS.register("iron_powder", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TUNGSTEN_POWDER = ITEMS.register("tungsten_powder", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COAL_POWDER = ITEMS.register("coal_powder", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COAL_IRON_POWDER = ITEMS.register("coal_iron_powder", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RAW_CEMENTED_CARBIDE_POWDER = ITEMS.register("raw_cemented_carbide_powder", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GALENA = ITEMS.register("galena", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SCHEELITE = ITEMS.register("scheelite", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RAW_SILVER = ITEMS.register("raw_silver", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DOG_TAG = ITEMS.register("dog_tag", DogTag::new);
    public static final RegistryObject<Item> CELL = ITEMS.register("cell", () -> new BatteryItem(24000, new Item.Properties()));
    public static final RegistryObject<Item> BATTERY = ITEMS.register("battery", () -> new BatteryItem(100000, new Item.Properties()));
    public static final RegistryObject<Item> SMALL_BATTERY_PACK = ITEMS.register("small_battery_pack", () -> new BatteryItem(500000, new Item.Properties()));
    public static final RegistryObject<Item> MEDIUM_BATTERY_PACK = ITEMS.register("medium_battery_pack", () -> new BatteryItem(5000000, new Item.Properties()));
    public static final RegistryObject<Item> LARGE_BATTERY_PACK = ITEMS.register("large_battery_pack", () -> new BatteryItem(20000000, new Item.Properties()));
    public static final RegistryObject<Item> BEAST = ITEMS.register("beast", Beast::new);
    public static final RegistryObject<Item> TRANSCRIPT = ITEMS.register("transcript", Transcript::new);
    public static final RegistryObject<Item> FIRING_PARAMETERS = ITEMS.register("firing_parameters", FiringParameters::new);

    public static final RegistryObject<Item> TUNGSTEN_ROD = ITEMS.register("tungsten_rod", () -> new Item(new Item.Properties()));

    public static final Materials IRON_MATERIALS = registerMaterials("iron");
    public static final Materials STEEL_MATERIALS = registerMaterials("steel");
    public static final Materials CEMENTED_CARBIDE_MATERIALS = registerMaterials("cemented_carbide");
    public static final Materials NETHERITE_MATERIALS = registerMaterials("netherite");

    public static final RegistryObject<Item> COMMON_MATERIAL_PACK = ITEMS.register("common_material_pack", () -> new MaterialPack(Rarity.COMMON));
    public static final RegistryObject<Item> RARE_MATERIAL_PACK = ITEMS.register("rare_material_pack", () -> new MaterialPack(Rarity.RARE));
    public static final RegistryObject<Item> EPIC_MATERIAL_PACK = ITEMS.register("epic_material_pack", () -> new MaterialPack(Rarity.EPIC));
    public static final RegistryObject<Item> LEGENDARY_MATERIAL_PACK = ITEMS.register("legendary_material_pack", () -> new MaterialPack(RarityTool.LEGENDARY));

    public static final RegistryObject<Item> LIGHT_ARMAMENT_MODULE = ITEMS.register("light_armament_module", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> MEDIUM_ARMAMENT_MODULE = ITEMS.register("medium_armament_module", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> HEAVY_ARMAMENT_MODULE = ITEMS.register("heavy_armament_module", () -> new Item(new Item.Properties().rarity(RarityTool.LEGENDARY)));

    public static final RegistryObject<Item> TRACHELIUM_BLUEPRINT = ITEMS.register("trachelium_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final RegistryObject<Item> GLOCK_17_BLUEPRINT = ITEMS.register("glock_17_blueprint", () -> new BlueprintItem(Rarity.COMMON));
    public static final RegistryObject<Item> MP_443_BLUEPRINT = ITEMS.register("mp_443_blueprint", () -> new BlueprintItem(Rarity.COMMON));
    public static final RegistryObject<Item> GLOCK_18_BLUEPRINT = ITEMS.register("glock_18_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final RegistryObject<Item> HUNTING_RIFLE_BLUEPRINT = ITEMS.register("hunting_rifle_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final RegistryObject<Item> M_79_BLUEPRINT = ITEMS.register("m_79_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final RegistryObject<Item> RPG_BLUEPRINT = ITEMS.register("rpg_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final RegistryObject<Item> BOCEK_BLUEPRINT = ITEMS.register("bocek_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final RegistryObject<Item> M_4_BLUEPRINT = ITEMS.register("m_4_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final RegistryObject<Item> AA_12_BLUEPRINT = ITEMS.register("aa_12_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));
    public static final RegistryObject<Item> HK_416_BLUEPRINT = ITEMS.register("hk_416_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final RegistryObject<Item> RPK_BLUEPRINT = ITEMS.register("rpk_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final RegistryObject<Item> SKS_BLUEPRINT = ITEMS.register("sks_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final RegistryObject<Item> NTW_20_BLUEPRINT = ITEMS.register("ntw_20_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));
    public static final RegistryObject<Item> MP_5_BLUEPRINT = ITEMS.register("mp_5_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final RegistryObject<Item> VECTOR_BLUEPRINT = ITEMS.register("vector_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final RegistryObject<Item> MINIGUN_BLUEPRINT = ITEMS.register("minigun_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));
    public static final RegistryObject<Item> MK_14_BLUEPRINT = ITEMS.register("mk_14_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final RegistryObject<Item> SENTINEL_BLUEPRINT = ITEMS.register("sentinel_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final RegistryObject<Item> M_60_BLUEPRINT = ITEMS.register("m_60_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final RegistryObject<Item> SVD_BLUEPRINT = ITEMS.register("svd_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final RegistryObject<Item> MARLIN_BLUEPRINT = ITEMS.register("marlin_blueprint", () -> new BlueprintItem(Rarity.COMMON));
    public static final RegistryObject<Item> M_870_BLUEPRINT = ITEMS.register("m_870_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final RegistryObject<Item> M_98B_BLUEPRINT = ITEMS.register("m_98b_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final RegistryObject<Item> AK_47_BLUEPRINT = ITEMS.register("ak_47_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final RegistryObject<Item> AK_12_BLUEPRINT = ITEMS.register("ak_12_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final RegistryObject<Item> DEVOTION_BLUEPRINT = ITEMS.register("devotion_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final RegistryObject<Item> TASER_BLUEPRINT = ITEMS.register("taser_blueprint", () -> new BlueprintItem(Rarity.COMMON));
    public static final RegistryObject<Item> M_1911_BLUEPRINT = ITEMS.register("m_1911_blueprint", () -> new BlueprintItem(Rarity.COMMON));
    public static final RegistryObject<Item> QBZ_95_BLUEPRINT = ITEMS.register("qbz_95_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final RegistryObject<Item> QBZ_191_BLUEPRINT = ITEMS.register("qbz_191_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final RegistryObject<Item> K_98_BLUEPRINT = ITEMS.register("k_98_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final RegistryObject<Item> MOSIN_NAGANT_BLUEPRINT = ITEMS.register("mosin_nagant_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final RegistryObject<Item> JAVELIN_BLUEPRINT = ITEMS.register("javelin_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));
    public static final RegistryObject<Item> M_2_HB_BLUEPRINT = ITEMS.register("m_2_hb_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final RegistryObject<Item> SECONDARY_CATACLYSM_BLUEPRINT = ITEMS.register("secondary_cataclysm_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));
    public static final RegistryObject<Item> INSIDIOUS_BLUEPRINT = ITEMS.register("insidious_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final RegistryObject<Item> AURELIA_SCEPTRE_BLUEPRINT = ITEMS.register("aurelia_sceptre_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));
    public static final RegistryObject<Item> MK_42_BLUEPRINT = ITEMS.register("mk_42_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));
    public static final RegistryObject<Item> MLE_1934_BLUEPRINT = ITEMS.register("mle_1934_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));
    public static final RegistryObject<Item> HPJ_11_BLUEPRINT = ITEMS.register("hpj_11_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));
    public static final RegistryObject<Item> ANNIHILATOR_BLUEPRINT = ITEMS.register("annihilator_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));

    /**
     * Block
     */
    public static final DeferredRegister<Item> BLOCKS = DeferredRegister.create(ForgeRegistries.ITEMS, Mod.MODID);

    public static final RegistryObject<Item> GALENA_ORE = block(ModBlocks.GALENA_ORE);
    public static final RegistryObject<Item> DEEPSLATE_GALENA_ORE = block(ModBlocks.DEEPSLATE_GALENA_ORE);
    public static final RegistryObject<Item> SCHEELITE_ORE = block(ModBlocks.SCHEELITE_ORE);
    public static final RegistryObject<Item> DEEPSLATE_SCHEELITE_ORE = block(ModBlocks.DEEPSLATE_SCHEELITE_ORE);
    public static final RegistryObject<Item> SILVER_ORE = block(ModBlocks.SILVER_ORE);
    public static final RegistryObject<Item> DEEPSLATE_SILVER_ORE = block(ModBlocks.DEEPSLATE_SILVER_ORE);
    public static final RegistryObject<Item> JUMP_PAD = block(ModBlocks.JUMP_PAD);
    public static final RegistryObject<Item> SANDBAG = block(ModBlocks.SANDBAG);
    public static final RegistryObject<Item> BARBED_WIRE = block(ModBlocks.BARBED_WIRE);
    public static final RegistryObject<Item> DRAGON_TEETH = block(ModBlocks.DRAGON_TEETH);
    public static final RegistryObject<Item> REFORGING_TABLE = block(ModBlocks.REFORGING_TABLE);
    public static final RegistryObject<Item> CHARGING_STATION = BLOCKS.register("charging_station", ChargingStationBlockItem::new);
    public static final RegistryObject<Item> CREATIVE_CHARGING_STATION = BLOCKS.register("creative_charging_station", CreativeChargingStationBlockItem::new);
    public static final RegistryObject<Item> LEAD_BLOCK = block(ModBlocks.LEAD_BLOCK);
    public static final RegistryObject<Item> STEEL_BLOCK = block(ModBlocks.STEEL_BLOCK);
    public static final RegistryObject<Item> TUNGSTEN_BLOCK = block(ModBlocks.TUNGSTEN_BLOCK);
    public static final RegistryObject<Item> SILVER_BLOCK = block(ModBlocks.SILVER_BLOCK);
    public static final RegistryObject<Item> CEMENTED_CARBIDE_BLOCK = block(ModBlocks.CEMENTED_CARBIDE_BLOCK);
    public static final RegistryObject<Item> FUMO_25 = block(ModBlocks.FUMO_25);
    public static final RegistryObject<Item> VEHICLE_DEPLOYER = BLOCKS.register("vehicle_deployer", VehicleDeployerBlockItem::new);
    public static final RegistryObject<Item> AIRCRAFT_CATAPULT = block(ModBlocks.AIRCRAFT_CATAPULT);
    public static final RegistryObject<Item> SUPERB_ITEM_INTERFACE = block(ModBlocks.SUPERB_ITEM_INTERFACE);

    /**
     * Vehicle
     */
    public static final DeferredRegister<Item> VEHICLES = DeferredRegister.create(ForgeRegistries.ITEMS, Mod.MODID);

    public static final RegistryObject<Item> CONTAINER = VEHICLES.register("container", ContainerBlockItem::new);
    public static final RegistryObject<Item> SMALL_CONTAINER = VEHICLES.register("small_container", SmallContainerBlockItem::new);
    public static final RegistryObject<Item> LUCKY_CONTAINER = VEHICLES.register("lucky_container", LuckyContainerBlockItem::new);

    private static RegistryObject<Item> block(RegistryObject<Block> block) {
        return BLOCKS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public record Materials(
            String name,
            RegistryObject<Item> barrel,
            RegistryObject<Item> action,
            RegistryObject<Item> spring,
            RegistryObject<Item> trigger
    ) {
    }

    public static Materials registerMaterials(String name) {
        return new Materials(
                name,
                ITEMS.register(name + "_barrel", () -> new Item(new Item.Properties())),
                ITEMS.register(name + "_action", () -> new Item(new Item.Properties())),
                ITEMS.register(name + "_spring", () -> new Item(new Item.Properties())),
                ITEMS.register(name + "_trigger", () -> new Item(new Item.Properties()))
        );
    }

    /**
     * Perk Items
     */
    public static final DeferredRegister<Item> PERKS = DeferredRegister.create(ForgeRegistries.ITEMS, Mod.MODID);

    public static final Map<RegistryObject<Perk>, RegistryObject<Item>> PERK_ITEMS = new HashMap<>();

    /**
     * 单独注册，用于Tab图标，不要删
     */
    public static RegistryObject<Item> AP_BULLET;
    public static RegistryObject<Item> INTELLIGENT_CHIP;

    public static void registerPerkItems() {
        ModPerks.AMMO_PERKS.getEntries().forEach(ModItems::registerSinglePerkItem);
        ModPerks.FUNC_PERKS.getEntries().forEach(ModItems::registerSinglePerkItem);
        ModPerks.DAMAGE_PERKS.getEntries().forEach(ModItems::registerSinglePerkItem);

        AP_BULLET = PERK_ITEMS.get(ModPerks.AP_BULLET);
        INTELLIGENT_CHIP = PERK_ITEMS.get(ModPerks.INTELLIGENT_CHIP);
    }

    private static void registerSinglePerkItem(RegistryObject<Perk> perk) {
        PERK_ITEMS.put(perk, PERKS.register(perk.getId().getPath(), () -> new PerkItem(perk)));
    }

    public static final RegistryObject<Item> SHORTCUT_PACK = PERKS.register("shortcut_pack", ShortcutPack::new);
    public static final RegistryObject<Item> EMPTY_PERK = PERKS.register("empty_perk", () -> new Item(new Item.Properties()));


    public static void registerDispenserBehavior(FMLCommonSetupEvent event) {
        List<RegistryObject<Item>> list = new ArrayList<>();
        list.addAll(AMMO.getEntries());
        list.addAll(ITEMS.getEntries());

        for (var item : list) {
            if (item.get() instanceof DispenserLaunchable launchable) {
                DispenserBlock.registerBehavior(item.get(), launchable.getLaunchBehavior());
            }
        }
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        GUNS.register(bus);
        AMMO.register(bus);
        BLOCKS.register(bus);
        VEHICLES.register(bus);
        registerPerkItems();
        PERKS.register(bus);
    }
}
