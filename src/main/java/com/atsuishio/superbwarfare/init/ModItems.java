package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.*;
import com.atsuishio.superbwarfare.item.armor.*;
import com.atsuishio.superbwarfare.item.common.BlueprintItem;
import com.atsuishio.superbwarfare.item.common.MaterialPack;
import com.atsuishio.superbwarfare.item.common.ammo.*;
import com.atsuishio.superbwarfare.item.common.ammo.box.AmmoBox;
import com.atsuishio.superbwarfare.item.gun.handgun.*;
import com.atsuishio.superbwarfare.item.gun.heavy.Ntw20Item;
import com.atsuishio.superbwarfare.item.gun.launcher.JavelinItem;
import com.atsuishio.superbwarfare.item.gun.launcher.M79Item;
import com.atsuishio.superbwarfare.item.gun.launcher.RpgItem;
import com.atsuishio.superbwarfare.item.gun.launcher.SecondaryCataclysm;
import com.atsuishio.superbwarfare.item.gun.machinegun.DevotionItem;
import com.atsuishio.superbwarfare.item.gun.machinegun.M60Item;
import com.atsuishio.superbwarfare.item.gun.machinegun.MinigunItem;
import com.atsuishio.superbwarfare.item.gun.machinegun.RpkItem;
import com.atsuishio.superbwarfare.item.gun.rifle.*;
import com.atsuishio.superbwarfare.item.gun.shotgun.Aa12Item;
import com.atsuishio.superbwarfare.item.gun.shotgun.HomemadeShotgunItem;
import com.atsuishio.superbwarfare.item.gun.shotgun.M870Item;
import com.atsuishio.superbwarfare.item.gun.smg.Mp5Item;
import com.atsuishio.superbwarfare.item.gun.smg.VectorItem;
import com.atsuishio.superbwarfare.item.gun.sniper.*;
import com.atsuishio.superbwarfare.item.gun.special.BocekItem;
import com.atsuishio.superbwarfare.item.gun.special.TaserItem;
import com.atsuishio.superbwarfare.tiers.ModItemTier;
import com.atsuishio.superbwarfare.tools.Ammo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ModItems {
    /**
     * guns
     */
    public static final DeferredRegister<Item> GUNS = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID);

    public static final DeferredHolder<Item, TaserItem> TASER = GUNS.register("taser", TaserItem::new);
    public static final DeferredHolder<Item, Glock17Item> GLOCK_17 = GUNS.register("glock_17", Glock17Item::new);
    public static final DeferredHolder<Item, Glock18Item> GLOCK_18 = GUNS.register("glock_18", Glock18Item::new);
    public static final DeferredHolder<Item, Mp443Item> MP_443 = GUNS.register("mp_443", Mp443Item::new);
    public static final DeferredHolder<Item, M1911Item> M_1911 = GUNS.register("m_1911", M1911Item::new);
    public static final DeferredHolder<Item, HomemadeShotgunItem> HOMEMADE_SHOTGUN = GUNS.register("homemade_shotgun", HomemadeShotgunItem::new);
    public static final DeferredHolder<Item, Trachelium> TRACHELIUM = GUNS.register("trachelium", Trachelium::new);
    public static final DeferredHolder<Item, Mp5Item> MP_5 = GUNS.register("mp_5", Mp5Item::new);
    public static final DeferredHolder<Item, VectorItem> VECTOR = GUNS.register("vector", VectorItem::new);
    public static final DeferredHolder<Item, AK47Item> AK_47 = GUNS.register("ak_47", AK47Item::new);
    public static final DeferredHolder<Item, AK12Item> AK_12 = GUNS.register("ak_12", AK12Item::new);
    public static final DeferredHolder<Item, SksItem> SKS = GUNS.register("sks", SksItem::new);
    public static final DeferredHolder<Item, M4Item> M_4 = GUNS.register("m_4", M4Item::new);
    public static final DeferredHolder<Item, Hk416Item> HK_416 = GUNS.register("hk_416", Hk416Item::new);
    public static final DeferredHolder<Item, Qbz95Item> QBZ_95 = GUNS.register("qbz_95", Qbz95Item::new);
    public static final DeferredHolder<Item, InsidiousItem> INSIDIOUS = GUNS.register("insidious", InsidiousItem::new);
    public static final DeferredHolder<Item, Mk14Item> MK_14 = GUNS.register("mk_14", Mk14Item::new);
    public static final DeferredHolder<Item, MarlinItem> MARLIN = GUNS.register("marlin", MarlinItem::new);
    public static final DeferredHolder<Item, K98Item> K_98 = GUNS.register("k_98", K98Item::new);
    public static final DeferredHolder<Item, MosinNagantItem> MOSIN_NAGANT = GUNS.register("mosin_nagant", MosinNagantItem::new);
    public static final DeferredHolder<Item, SvdItem> SVD = GUNS.register("svd", SvdItem::new);
    public static final DeferredHolder<Item, M98bItem> M_98B = GUNS.register("m_98b", M98bItem::new);
    public static final DeferredHolder<Item, SentinelItem> SENTINEL = GUNS.register("sentinel", SentinelItem::new);
    public static final DeferredHolder<Item, HuntingRifleItem> HUNTING_RIFLE = GUNS.register("hunting_rifle", HuntingRifleItem::new);
    public static final DeferredHolder<Item, Ntw20Item> NTW_20 = GUNS.register("ntw_20", Ntw20Item::new);
    public static final DeferredHolder<Item, M870Item> M_870 = GUNS.register("m_870", M870Item::new);
    public static final DeferredHolder<Item, Aa12Item> AA_12 = GUNS.register("aa_12", Aa12Item::new);
    public static final DeferredHolder<Item, DevotionItem> DEVOTION = GUNS.register("devotion", DevotionItem::new);
    public static final DeferredHolder<Item, RpkItem> RPK = GUNS.register("rpk", RpkItem::new);
    public static final DeferredHolder<Item, M60Item> M_60 = GUNS.register("m_60", M60Item::new);
    public static final DeferredHolder<Item, MinigunItem> MINIGUN = GUNS.register("minigun", MinigunItem::new);
    public static final DeferredHolder<Item, M79Item> M_79 = GUNS.register("m_79", M79Item::new);
    public static final DeferredHolder<Item, SecondaryCataclysm> SECONDARY_CATACLYSM = GUNS.register("secondary_cataclysm", SecondaryCataclysm::new);
    public static final DeferredHolder<Item, RpgItem> RPG = GUNS.register("rpg", RpgItem::new);
    public static final DeferredHolder<Item, JavelinItem> JAVELIN = GUNS.register("javelin", JavelinItem::new);
    public static final DeferredHolder<Item, AureliaSceptre> AURELIA_SCEPTRE = GUNS.register("aurelia_sceptre", AureliaSceptre::new);
    public static final DeferredHolder<Item, BocekItem> BOCEK = GUNS.register("bocek", BocekItem::new);

    /**
     * Ammo
     */
    public static final DeferredRegister<Item> AMMO = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID);

    public static final DeferredHolder<Item, AmmoSupplierItem> HANDGUN_AMMO = AMMO.register("handgun_ammo", () -> new AmmoSupplierItem(Ammo.HANDGUN, 1, new Item.Properties()));
    public static final DeferredHolder<Item, AmmoSupplierItem> RIFLE_AMMO = AMMO.register("rifle_ammo", () -> new AmmoSupplierItem(Ammo.RIFLE, 1, new Item.Properties()));
    public static final DeferredHolder<Item, AmmoSupplierItem> SNIPER_AMMO = AMMO.register("sniper_ammo", () -> new AmmoSupplierItem(Ammo.SNIPER, 1, new Item.Properties()));
    public static final DeferredHolder<Item, AmmoSupplierItem> SHOTGUN_AMMO = AMMO.register("shotgun_ammo", () -> new AmmoSupplierItem(Ammo.SHOTGUN, 1, new Item.Properties()));
    public static final DeferredHolder<Item, AmmoSupplierItem> HEAVY_AMMO = AMMO.register("heavy_ammo", () -> new AmmoSupplierItem(Ammo.HEAVY, 1, new Item.Properties()));
    public static final DeferredHolder<Item, HandgunAmmoBox> HANDGUN_AMMO_BOX = AMMO.register("handgun_ammo_box", HandgunAmmoBox::new);
    public static final DeferredHolder<Item, RifleAmmoBox> RIFLE_AMMO_BOX = AMMO.register("rifle_ammo_box", RifleAmmoBox::new);
    public static final DeferredHolder<Item, SniperAmmoBox> SNIPER_AMMO_BOX = AMMO.register("sniper_ammo_box", SniperAmmoBox::new);
    public static final DeferredHolder<Item, ShotgunAmmoBox> SHOTGUN_AMMO_BOX = AMMO.register("shotgun_ammo_box", ShotgunAmmoBox::new);
    public static final DeferredHolder<Item, CreativeAmmoBox> CREATIVE_AMMO_BOX = AMMO.register("creative_ammo_box", CreativeAmmoBox::new);
    public static final DeferredHolder<Item, AmmoBox> AMMO_BOX = AMMO.register("ammo_box", AmmoBox::new);
    public static final DeferredHolder<Item, Item> TASER_ELECTRODE = AMMO.register("taser_electrode", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> GRENADE_40MM = AMMO.register("grenade_40mm", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> JAVELIN_MISSILE = AMMO.register("javelin_missile", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, MortarShell> MORTAR_SHELL = AMMO.register("mortar_shell", MortarShell::new);
    public static final DeferredHolder<Item, PotionMortarShell> POTION_MORTAR_SHELL = AMMO.register("potion_mortar_shell", PotionMortarShell::new);
    public static final DeferredHolder<Item, Rocket> ROCKET = AMMO.register("rocket", Rocket::new);
    public static final DeferredHolder<Item, LungeMine> LUNGE_MINE = AMMO.register("lunge_mine", LungeMine::new);
    public static final DeferredHolder<Item, Item> HE_5_INCHES = AMMO.register("he_5_inches", () -> new CannonShellItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> AP_5_INCHES = AMMO.register("ap_5_inches", () -> new CannonShellItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, HandGrenade> HAND_GRENADE = AMMO.register("hand_grenade", HandGrenade::new);
    public static final DeferredHolder<Item, RgoGrenade> RGO_GRENADE = AMMO.register("rgo_grenade", RgoGrenade::new);
    public static final DeferredHolder<Item, ClaymoreMine> CLAYMORE_MINE = AMMO.register("claymore_mine", ClaymoreMine::new);
    public static final DeferredHolder<Item, Tm62> TM_62 = AMMO.register("tm_62", Tm62::new);
    public static final DeferredHolder<Item, C4Bomb> C4_BOMB = AMMO.register("c4_bomb", C4Bomb::new);
    public static final DeferredHolder<Item, Blu43Mine> BLU_43_MINE = AMMO.register("blu_43_mine", Blu43Mine::new);
    public static final DeferredHolder<Item, Item> SMALL_SHELL = AMMO.register("small_shell", SmallShellItem::new);
    public static final DeferredHolder<Item, Item> ROCKET_70 = AMMO.register("rocket_70", Rocket70::new);
    public static final DeferredHolder<Item, WireGuideMissile> WIRE_GUIDE_MISSILE = AMMO.register("wire_guide_missile", WireGuideMissile::new);
    public static final DeferredHolder<Item, Agm> AGM = AMMO.register("agm", Agm::new);
    public static final DeferredHolder<Item, SwarmDrone> SWARM_DRONE = AMMO.register("swarm_drone", SwarmDrone::new);
    public static final DeferredHolder<Item, MediumAerialBomb> MEDIUM_AERIAL_BOMB = AMMO.register("medium_aerial_bomb", MediumAerialBomb::new);
    public static final DeferredHolder<Item, BeamTest> BEAM_TEST = AMMO.register("beam_test", BeamTest::new);

    /**
     * items
     */
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Mod.MODID);

    public static final DeferredHolder<Item, DeferredSpawnEggItem> SENPAI_SPAWN_EGG = ITEMS.register("senpai_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.SENPAI::value, -11584987, -14014413, new Item.Properties()));
    public static final DeferredHolder<Item, Item> ANCIENT_CPU = ITEMS.register("ancient_cpu", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> PROPELLER = ITEMS.register("propeller", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> LARGE_PROPELLER = ITEMS.register("large_propeller", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> MOTOR = ITEMS.register("motor", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> LARGE_MOTOR = ITEMS.register("large_motor", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> WHEEL = ITEMS.register("wheel", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> TRACK = ITEMS.register("track", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Drone> DRONE = ITEMS.register("drone", Drone::new);

    public static final DeferredHolder<Item, Monitor> MONITOR = ITEMS.register("monitor", Monitor::new);

    public static final DeferredHolder<Item, Detonator> DETONATOR = ITEMS.register("detonator", Detonator::new);
    public static final DeferredHolder<Item, TargetDeployer> TARGET_DEPLOYER = ITEMS.register("target_deployer", TargetDeployer::new);
    public static final DeferredHolder<Item, DPSGeneratorDeployer> DPS_GENERATOR_DEPLOYER = ITEMS.register("dps_generator_deployer", DPSGeneratorDeployer::new);
    public static final DeferredHolder<Item, Item> KNIFE = ITEMS.register("knife", () -> new SwordItem(ModItemTier.STEEL, new Item.Properties()
            .attributes(SwordItem.createAttributes(ModItemTier.STEEL, 0, -1.8f))
            .durability(1200)
    ));
    public static final DeferredHolder<Item, Hammer> HAMMER = ITEMS.register("hammer", Hammer::new);
    public static final DeferredHolder<Item, TBaton> T_BATON = ITEMS.register("t_baton", TBaton::new);
    public static final DeferredHolder<Item, ElectricBaton> ELECTRIC_BATON = ITEMS.register("electric_baton", ElectricBaton::new);
    public static final DeferredHolder<Item, Crowbar> CROWBAR = ITEMS.register("crowbar", Crowbar::new);
    public static final DeferredHolder<Item, Defuser> DEFUSER = ITEMS.register("defuser", Defuser::new);
    public static final DeferredHolder<Item, ArmorPlate> ARMOR_PLATE = ITEMS.register("armor_plate", ArmorPlate::new);

    public static final DeferredHolder<Item, RuHelmet6b47> RU_HELMET_6B47 = ITEMS.register("ru_helmet_6b47", RuHelmet6b47::new);
    public static final DeferredHolder<Item, RuChest6b43> RU_CHEST_6B43 = ITEMS.register("ru_chest_6b43", RuChest6b43::new);
    public static final DeferredHolder<Item, UsHelmetPastg> US_HELMET_PASTG = ITEMS.register("us_helmet_pastg", UsHelmetPastg::new);
    public static final DeferredHolder<Item, UsChestIotv> US_CHEST_IOTV = ITEMS.register("us_chest_iotv", UsChestIotv::new);
    public static final DeferredHolder<Item, GeHelmetM35> GE_HELMET_M_35 = ITEMS.register("ge_helmet_m_35", GeHelmetM35::new);
    public static final DeferredHolder<Item, MortarDeployer> MORTAR_DEPLOYER = ITEMS.register("mortar_deployer", MortarDeployer::new);
    public static final DeferredHolder<Item, Item> MORTAR_BARREL = ITEMS.register("mortar_barrel", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> MORTAR_BASE_PLATE = ITEMS.register("mortar_base_plate", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> MORTAR_BIPOD = ITEMS.register("mortar_bipod", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SEEKER = ITEMS.register("seeker", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> MISSILE_ENGINE = ITEMS.register("missile_engine", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> FUSEE = ITEMS.register("fusee", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> PRIMER = ITEMS.register("primer", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> AP_HEAD = ITEMS.register("ap_head", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> HE_HEAD = ITEMS.register("he_head", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CANNON_CORE = ITEMS.register("cannon_core", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> COPPER_PLATE = ITEMS.register("copper_plate", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STEEL_INGOT = ITEMS.register("steel_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> LEAD_INGOT = ITEMS.register("lead_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SILVER_INGOT = ITEMS.register("silver_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> TUNGSTEN_INGOT = ITEMS.register("tungsten_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CEMENTED_CARBIDE_INGOT = ITEMS.register("cemented_carbide_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> HIGH_ENERGY_EXPLOSIVES = ITEMS.register("high_energy_explosives", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> GRAIN = ITEMS.register("grain", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> IRON_POWDER = ITEMS.register("iron_powder", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> TUNGSTEN_POWDER = ITEMS.register("tungsten_powder", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> COAL_POWDER = ITEMS.register("coal_powder", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> COAL_IRON_POWDER = ITEMS.register("coal_iron_powder", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAW_CEMENTED_CARBIDE_POWDER = ITEMS.register("raw_cemented_carbide_powder", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> GALENA = ITEMS.register("galena", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SCHEELITE = ITEMS.register("scheelite", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAW_SILVER = ITEMS.register("raw_silver", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, DogTag> DOG_TAG = ITEMS.register("dog_tag", DogTag::new);
    public static final DeferredHolder<Item, BatteryItem> CELL = ITEMS.register("cell", () -> new BatteryItem(24000, new Item.Properties()));
    public static final DeferredHolder<Item, BatteryItem> BATTERY = ITEMS.register("battery", () -> new BatteryItem(100000, new Item.Properties()));
    public static final DeferredHolder<Item, BatteryItem> SMALL_BATTERY_PACK = ITEMS.register("small_battery_pack", () -> new BatteryItem(500000, new Item.Properties()));
    public static final DeferredHolder<Item, BatteryItem> MEDIUM_BATTERY_PACK = ITEMS.register("medium_battery_pack", () -> new BatteryItem(5000000, new Item.Properties()));
    public static final DeferredHolder<Item, BatteryItem> LARGE_BATTERY_PACK = ITEMS.register("large_battery_pack", () -> new BatteryItem(20000000, new Item.Properties()));
    public static final DeferredHolder<Item, Beast> BEAST = ITEMS.register("beast", Beast::new);
    public static final DeferredHolder<Item, Transcript> TRANSCRIPT = ITEMS.register("transcript", Transcript::new);
    public static final DeferredHolder<Item, FiringParameters> FIRING_PARAMETERS = ITEMS.register("firing_parameters", FiringParameters::new);

    public static final DeferredHolder<Item, Item> TUNGSTEN_ROD = ITEMS.register("tungsten_rod", () -> new Item(new Item.Properties()));

    public static final Materials IRON_MATERIALS = registerMaterials("iron");
    public static final Materials STEEL_MATERIALS = registerMaterials("steel");
    public static final Materials CEMENTED_CARBIDE_MATERIALS = registerMaterials("cemented_carbide");
    public static final Materials NETHERITE_MATERIALS = registerMaterials("netherite");

    public static final DeferredHolder<Item, MaterialPack> COMMON_MATERIAL_PACK = ITEMS.register("common_material_pack", () -> new MaterialPack(Rarity.COMMON));
    public static final DeferredHolder<Item, MaterialPack> RARE_MATERIAL_PACK = ITEMS.register("rare_material_pack", () -> new MaterialPack(Rarity.RARE));
    public static final DeferredHolder<Item, MaterialPack> EPIC_MATERIAL_PACK = ITEMS.register("epic_material_pack", () -> new MaterialPack(Rarity.EPIC));
    public static final DeferredHolder<Item, MaterialPack> LEGENDARY_MATERIAL_PACK = ITEMS.register("legendary_material_pack", () -> new MaterialPack(ModEnumExtensions.getLegendary()));

    public static final DeferredHolder<Item, BlueprintItem> TRACHELIUM_BLUEPRINT = ITEMS.register("trachelium_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, BlueprintItem> GLOCK_17_BLUEPRINT = ITEMS.register("glock_17_blueprint", () -> new BlueprintItem(Rarity.COMMON));
    public static final DeferredHolder<Item, BlueprintItem> MP_443_BLUEPRINT = ITEMS.register("mp_443_blueprint", () -> new BlueprintItem(Rarity.COMMON));
    public static final DeferredHolder<Item, BlueprintItem> GLOCK_18_BLUEPRINT = ITEMS.register("glock_18_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, BlueprintItem> HUNTING_RIFLE_BLUEPRINT = ITEMS.register("hunting_rifle_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, BlueprintItem> M_79_BLUEPRINT = ITEMS.register("m_79_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, BlueprintItem> RPG_BLUEPRINT = ITEMS.register("rpg_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, BlueprintItem> BOCEK_BLUEPRINT = ITEMS.register("bocek_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, BlueprintItem> M_4_BLUEPRINT = ITEMS.register("m_4_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, BlueprintItem> AA_12_BLUEPRINT = ITEMS.register("aa_12_blueprint", () -> new BlueprintItem(ModEnumExtensions.getLegendary()));
    public static final DeferredHolder<Item, BlueprintItem> HK_416_BLUEPRINT = ITEMS.register("hk_416_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, BlueprintItem> RPK_BLUEPRINT = ITEMS.register("rpk_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, BlueprintItem> SKS_BLUEPRINT = ITEMS.register("sks_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, BlueprintItem> NTW_20_BLUEPRINT = ITEMS.register("ntw_20_blueprint", () -> new BlueprintItem(ModEnumExtensions.getLegendary()));
    public static final DeferredHolder<Item, BlueprintItem> MP_5_BLUEPRINT = ITEMS.register("mp_5_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, BlueprintItem> VECTOR_BLUEPRINT = ITEMS.register("vector_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, BlueprintItem> MINIGUN_BLUEPRINT = ITEMS.register("minigun_blueprint", () -> new BlueprintItem(ModEnumExtensions.getLegendary()));
    public static final DeferredHolder<Item, BlueprintItem> MK_14_BLUEPRINT = ITEMS.register("mk_14_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, BlueprintItem> SENTINEL_BLUEPRINT = ITEMS.register("sentinel_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, BlueprintItem> M_60_BLUEPRINT = ITEMS.register("m_60_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, BlueprintItem> SVD_BLUEPRINT = ITEMS.register("svd_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, BlueprintItem> MARLIN_BLUEPRINT = ITEMS.register("marlin_blueprint", () -> new BlueprintItem(Rarity.COMMON));
    public static final DeferredHolder<Item, BlueprintItem> M_870_BLUEPRINT = ITEMS.register("m_870_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, BlueprintItem> M_98B_BLUEPRINT = ITEMS.register("m_98b_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, BlueprintItem> AK_47_BLUEPRINT = ITEMS.register("ak_47_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, BlueprintItem> AK_12_BLUEPRINT = ITEMS.register("ak_12_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, BlueprintItem> DEVOTION_BLUEPRINT = ITEMS.register("devotion_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, BlueprintItem> TASER_BLUEPRINT = ITEMS.register("taser_blueprint", () -> new BlueprintItem(Rarity.COMMON));
    public static final DeferredHolder<Item, BlueprintItem> M_1911_BLUEPRINT = ITEMS.register("m_1911_blueprint", () -> new BlueprintItem(Rarity.COMMON));
    public static final DeferredHolder<Item, BlueprintItem> QBZ_95_BLUEPRINT = ITEMS.register("qbz_95_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, BlueprintItem> K_98_BLUEPRINT = ITEMS.register("k_98_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, BlueprintItem> MOSIN_NAGANT_BLUEPRINT = ITEMS.register("mosin_nagant_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, BlueprintItem> JAVELIN_BLUEPRINT = ITEMS.register("javelin_blueprint", () -> new BlueprintItem(ModEnumExtensions.getLegendary()));
    public static final DeferredHolder<Item, BlueprintItem> M_2_HB_BLUEPRINT = ITEMS.register("m2hb_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, BlueprintItem> SECONDARY_CATACLYSM_BLUEPRINT = ITEMS.register("secondary_cataclysm_blueprint", () -> new BlueprintItem(ModEnumExtensions.getLegendary()));
    public static final DeferredHolder<Item, BlueprintItem> INSIDIOUS_BLUEPRINT = ITEMS.register("insidious_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, BlueprintItem> AURELIA_SCEPTRE_BLUEPRINT = ITEMS.register("aurelia_sceptre_blueprint", () -> new BlueprintItem(ModEnumExtensions.getLegendary()));
    public static final DeferredHolder<Item, BlueprintItem> MK_42_BLUEPRINT = ITEMS.register("mk_42_blueprint", () -> new BlueprintItem(ModEnumExtensions.getLegendary()));
    public static final DeferredHolder<Item, BlueprintItem> MLE_1934_BLUEPRINT = ITEMS.register("mle_1934_blueprint", () -> new BlueprintItem(ModEnumExtensions.getLegendary()));
    public static final DeferredHolder<Item, BlueprintItem> HPJ_11_BLUEPRINT = ITEMS.register("hpj_11_blueprint", () -> new BlueprintItem(ModEnumExtensions.getLegendary()));
    public static final DeferredHolder<Item, BlueprintItem> ANNIHILATOR_BLUEPRINT = ITEMS.register("annihilator_blueprint", () -> new BlueprintItem(ModEnumExtensions.getLegendary()));

    public static final DeferredHolder<Item, Item> LIGHT_ARMAMENT_MODULE = ITEMS.register("light_armament_module", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> MEDIUM_ARMAMENT_MODULE = ITEMS.register("medium_armament_module", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, Item> HEAVY_ARMAMENT_MODULE = ITEMS.register("heavy_armament_module", () -> new Item(new Item.Properties().rarity(ModEnumExtensions.getLegendary())));

    /**
     * Block
     */
    public static final DeferredRegister<Item> BLOCKS = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID);

    public static final DeferredHolder<Item, BlockItem> GALENA_ORE = block(ModBlocks.GALENA_ORE);
    public static final DeferredHolder<Item, BlockItem> DEEPSLATE_GALENA_ORE = block(ModBlocks.DEEPSLATE_GALENA_ORE);
    public static final DeferredHolder<Item, BlockItem> SCHEELITE_ORE = block(ModBlocks.SCHEELITE_ORE);
    public static final DeferredHolder<Item, BlockItem> DEEPSLATE_SCHEELITE_ORE = block(ModBlocks.DEEPSLATE_SCHEELITE_ORE);
    public static final DeferredHolder<Item, BlockItem> SILVER_ORE = block(ModBlocks.SILVER_ORE);
    public static final DeferredHolder<Item, BlockItem> DEEPSLATE_SILVER_ORE = block(ModBlocks.DEEPSLATE_SILVER_ORE);
    public static final DeferredHolder<Item, BlockItem> JUMP_PAD = block(ModBlocks.JUMP_PAD);
    public static final DeferredHolder<Item, BlockItem> SANDBAG = block(ModBlocks.SANDBAG);
    public static final DeferredHolder<Item, BlockItem> BARBED_WIRE = block(ModBlocks.BARBED_WIRE);
    public static final DeferredHolder<Item, BlockItem> DRAGON_TEETH = block(ModBlocks.DRAGON_TEETH);
    public static final DeferredHolder<Item, BlockItem> REFORGING_TABLE = block(ModBlocks.REFORGING_TABLE);
    public static final DeferredHolder<Item, CreativeChargingStationBlockItem> CREATIVE_CHARGING_STATION = BLOCKS.register("creative_charging_station", CreativeChargingStationBlockItem::new);
    public static final DeferredHolder<Item, ChargingStationBlockItem> CHARGING_STATION = BLOCKS.register("charging_station", ChargingStationBlockItem::new);
    public static final DeferredHolder<Item, BlockItem> LEAD_BLOCK = block(ModBlocks.LEAD_BLOCK);
    public static final DeferredHolder<Item, BlockItem> STEEL_BLOCK = block(ModBlocks.STEEL_BLOCK);
    public static final DeferredHolder<Item, BlockItem> TUNGSTEN_BLOCK = block(ModBlocks.TUNGSTEN_BLOCK);
    public static final DeferredHolder<Item, BlockItem> SILVER_BLOCK = block(ModBlocks.SILVER_BLOCK);
    public static final DeferredHolder<Item, BlockItem> CEMENTED_CARBIDE_BLOCK = block(ModBlocks.CEMENTED_CARBIDE_BLOCK);
    public static final DeferredHolder<Item, BlockItem> FUMO_25 = block(ModBlocks.FUMO_25);
    public static final DeferredHolder<Item, ContainerBlockItem> CONTAINER = BLOCKS.register("container", ContainerBlockItem::new);
    public static final DeferredHolder<Item, SmallContainerBlockItem> SMALL_CONTAINER = BLOCKS.register("small_container", SmallContainerBlockItem::new);
    public static final DeferredHolder<Item, VehicleDeployerBlockItem> VEHICLE_DEPLOYER = BLOCKS.register("vehicle_deployer", VehicleDeployerBlockItem::new);
    public static final DeferredHolder<Item, BlockItem> AIRCRAFT_CATAPULT = block(ModBlocks.AIRCRAFT_CATAPULT);
    public static final DeferredHolder<Item, BlockItem> SUPERB_ITEM_INTERFACE = block(ModBlocks.SUPERB_ITEM_INTERFACE);

    public record Materials(
            String name,
            DeferredHolder<Item, Item> barrel,
            DeferredHolder<Item, Item> action,
            DeferredHolder<Item, Item> spring,
            DeferredHolder<Item, Item> trigger
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
    public static final DeferredRegister<Item> PERKS = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID);

    public static void registerPerkItems() {
        ModPerks.AMMO_PERKS.getEntries().stream().filter(p -> p != ModPerks.AP_BULLET)
                .forEach(registryObject -> PERKS.register(registryObject.getId().getPath(), () -> new PerkItem<>(registryObject)));
        ModPerks.FUNC_PERKS.getEntries().forEach(registryObject -> PERKS.register(registryObject.getId().getPath(), () -> new PerkItem<>(registryObject)));
        ModPerks.DAMAGE_PERKS.getEntries().forEach(registryObject -> PERKS.register(registryObject.getId().getPath(), () -> new PerkItem<>(registryObject)));
    }

    public static final DeferredHolder<Item, ShortcutPack> SHORTCUT_PACK = PERKS.register("shortcut_pack", ShortcutPack::new);
    public static final DeferredHolder<Item, Item> EMPTY_PERK = PERKS.register("empty_perk", () -> new Item(new Item.Properties()));
    /**
     * 单独注册，用于Tab图标，不要删
     */
    public static final DeferredHolder<Item, Item> AP_BULLET = PERKS.register("ap_bullet", () -> new PerkItem<>(ModPerks.AP_BULLET));

    private static <T extends Block> DeferredHolder<Item, BlockItem> block(DeferredHolder<Block, T> block) {
        return BLOCKS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void registerDispenserBehavior(FMLCommonSetupEvent event) {
        List<DeferredHolder<Item, ? extends Item>> list = new ArrayList<>();
        list.addAll(AMMO.getEntries());
        list.addAll(ITEMS.getEntries());

        for (var item : list) {
            if (item.get() instanceof ProjectileItem launchable) {
                DispenserBlock.registerProjectileBehavior(item.get());
            }
        }

        DispenserBlock.registerBehavior(SWARM_DRONE.get(), new SwarmDrone.SwarmDroneDispenseBehavior());
        DispenserBlock.registerBehavior(C4_BOMB.get(), new C4Bomb.C4DispenseItemBehavior());
        DispenserBlock.registerBehavior(CLAYMORE_MINE.get(), new ClaymoreMine.ClaymoreDispenseBehavior());
        DispenserBlock.registerBehavior(BLU_43_MINE.get(), new Blu43Mine.Blu43MineDispenseBehavior());
        DispenserBlock.registerBehavior(ROCKET.get(), new Rocket.RocketDispenseBehavior());
        DispenserBlock.registerBehavior(ROCKET_70.get(), new Rocket70.Rocket70DispenseBehavior());
        DispenserBlock.registerBehavior(MEDIUM_AERIAL_BOMB.get(), new MediumAerialBomb.MediumAerialBombDispenseBehavior());
        DispenserBlock.registerBehavior(RGO_GRENADE.get(), new RgoGrenade.RgoGrenadeDispenserBehavior());
        DispenserBlock.registerBehavior(TM_62.get(), new Tm62.Tm62DispenseBehavior());
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        GUNS.register(bus);
        AMMO.register(bus);
        BLOCKS.register(bus);
        registerPerkItems();
        PERKS.register(bus);
    }

}
