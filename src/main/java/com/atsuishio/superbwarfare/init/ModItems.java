package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.item.*;
import com.atsuishio.superbwarfare.item.common.BlueprintItem;
import com.atsuishio.superbwarfare.item.common.CannonShellItem;
import com.atsuishio.superbwarfare.item.common.MaterialPack;
import com.atsuishio.superbwarfare.item.common.ammo.*;
import com.atsuishio.superbwarfare.item.common.ammo.box.AmmoBox;
import com.atsuishio.superbwarfare.tools.AmmoType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public class ModItems {
    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(Registries.ITEM, ModUtils.MODID);

    /**
     * guns
     */
    public static final DeferredRegister<Item> GUNS = DeferredRegister.create(BuiltInRegistries.ITEM, ModUtils.MODID);

//    public static final DeferredHolder<Item, Item> TASER = GUNS.register("taser", TaserItem::new);
//    public static final DeferredHolder<Item, Item> GLOCK_17 = GUNS.register("glock_17", Glock17Item::new);
//    public static final DeferredHolder<Item, Item> GLOCK_18 = GUNS.register("glock_18", Glock18Item::new);
//    public static final DeferredHolder<Item, Item> MP_443 = GUNS.register("mp_443", Mp443Item::new);
//    public static final DeferredHolder<Item, Item> M_1911 = GUNS.register("m_1911", M1911Item::new);
//    public static final DeferredHolder<Item, Item> HOMEMADE_SHOTGUN = GUNS.register("homemade_shotgun", HomemadeShotgunItem::new);
//    public static final DeferredHolder<Item, Item> TRACHELIUM = GUNS.register("trachelium", Trachelium::new);
//    public static final DeferredHolder<Item, Item> VECTOR = GUNS.register("vector", VectorItem::new);
//    public static final DeferredHolder<Item, Item> AK_47 = GUNS.register("ak_47", AK47Item::new);
//    public static final DeferredHolder<Item, Item> AK_12 = GUNS.register("ak_12", AK12Item::new);
//    public static final DeferredHolder<Item, Item> SKS = GUNS.register("sks", SksItem::new);
//    public static final DeferredHolder<Item, Item> M_4 = GUNS.register("m_4", M4Item::new);
//    public static final DeferredHolder<Item, Item> HK_416 = GUNS.register("hk_416", Hk416Item::new);
//    public static final DeferredHolder<Item, Item> QBZ_95 = GUNS.register("qbz_95", Qbz95Item::new);
//    public static final DeferredHolder<Item, Item> MK_14 = GUNS.register("mk_14", Mk14Item::new);
//    public static final DeferredHolder<Item, Item> MARLIN = GUNS.register("marlin", MarlinItem::new);
//    public static final DeferredHolder<Item, Item> K_98 = GUNS.register("k_98", K98Item::new);
//    public static final DeferredHolder<Item, Item> MOSIN_NAGANT = GUNS.register("mosin_nagant", MosinNagantItem::new);
//    public static final DeferredHolder<Item, Item> SVD = GUNS.register("svd", SvdItem::new);
//    public static final DeferredHolder<Item, Item> M_98B = GUNS.register("m_98b", M98bItem::new);
//    public static final DeferredHolder<Item, Item> SENTINEL = GUNS.register("sentinel", SentinelItem::new);
//    public static final DeferredHolder<Item, Item> HUNTING_RIFLE = GUNS.register("hunting_rifle", HuntingRifleItem::new);
//    public static final DeferredHolder<Item, Item> NTW_20 = GUNS.register("ntw_20", Ntw20Item::new);
//    public static final DeferredHolder<Item, Item> M_870 = GUNS.register("m_870", M870Item::new);
//    public static final DeferredHolder<Item, Item> AA_12 = GUNS.register("aa_12", Aa12Item::new);
//    public static final DeferredHolder<Item, Item> DEVOTION = GUNS.register("devotion", DevotionItem::new);
//    public static final DeferredHolder<Item, Item> RPK = GUNS.register("rpk", RpkItem::new);
//    public static final DeferredHolder<Item, Item> M_60 = GUNS.register("m_60", M60Item::new);
//    public static final DeferredHolder<Item, Item> MINIGUN = GUNS.register("minigun", MinigunItem::new);
//    public static final DeferredHolder<Item, Item> M_79 = GUNS.register("m_79", M79Item::new);
//    public static final DeferredHolder<Item, Item> SECONDARY_CATACLYSM = GUNS.register("secondary_cataclysm", SecondaryCataclysm::new);
//    public static final DeferredHolder<Item, Item> RPG = GUNS.register("rpg", RpgItem::new);
//    public static final DeferredHolder<Item, Item> JAVELIN = GUNS.register("javelin", JavelinItem::new);
//    public static final DeferredHolder<Item, Item> BOCEK = GUNS.register("bocek", BocekItem::new);

    /**
     * Ammo
     */
    public static final DeferredRegister<Item> AMMO = DeferredRegister.create(BuiltInRegistries.ITEM, ModUtils.MODID);

    public static final DeferredHolder<Item, Item> HANDGUN_AMMO = AMMO.register("handgun_ammo", () -> new AmmoSupplierItem(AmmoType.HANDGUN, 1, new Item.Properties()));
    public static final DeferredHolder<Item, Item> RIFLE_AMMO = AMMO.register("rifle_ammo", () -> new AmmoSupplierItem(AmmoType.RIFLE, 1, new Item.Properties()));
    public static final DeferredHolder<Item, Item> SNIPER_AMMO = AMMO.register("sniper_ammo", () -> new AmmoSupplierItem(AmmoType.SNIPER, 1, new Item.Properties()));
    public static final DeferredHolder<Item, Item> SHOTGUN_AMMO = AMMO.register("shotgun_ammo", () -> new AmmoSupplierItem(AmmoType.SHOTGUN, 1, new Item.Properties()));
    public static final DeferredHolder<Item, Item> HEAVY_AMMO = AMMO.register("heavy_ammo", () -> new AmmoSupplierItem(AmmoType.HEAVY, 1, new Item.Properties()));
    public static final DeferredHolder<Item, Item> HANDGUN_AMMO_BOX = AMMO.register("handgun_ammo_box", HandgunAmmoBox::new);
    public static final DeferredHolder<Item, Item> RIFLE_AMMO_BOX = AMMO.register("rifle_ammo_box", RifleAmmoBox::new);
    public static final DeferredHolder<Item, Item> SNIPER_AMMO_BOX = AMMO.register("sniper_ammo_box", SniperAmmoBox::new);
    public static final DeferredHolder<Item, Item> SHOTGUN_AMMO_BOX = AMMO.register("shotgun_ammo_box", ShotgunAmmoBox::new);
    public static final DeferredHolder<Item, Item> CREATIVE_AMMO_BOX = AMMO.register("creative_ammo_box", CreativeAmmoBox::new);
    public static final DeferredHolder<Item, Item> AMMO_BOX = AMMO.register("ammo_box", AmmoBox::new);
    public static final DeferredHolder<Item, Item> TASER_ELECTRODE = AMMO.register("taser_electrode", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> GRENADE_40MM = AMMO.register("grenade_40mm", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> JAVELIN_MISSILE = AMMO.register("javelin_missile", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> MORTAR_SHELL = AMMO.register("mortar_shell", MortarShell::new);
    public static final DeferredHolder<Item, Item> POTION_MORTAR_SHELL = AMMO.register("potion_mortar_shell", PotionMortarShell::new);
    public static final DeferredHolder<Item, Item> ROCKET = AMMO.register("rocket", Rocket::new);
    //    public static final DeferredHolder<Item, Item> LUNGE_MINE = AMMO.register("lunge_mine", LungeMine::new);
    public static final DeferredHolder<Item, Item> HE_5_INCHES = AMMO.register("he_5_inches", () -> new CannonShellItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> AP_5_INCHES = AMMO.register("ap_5_inches", () -> new CannonShellItem(new Item.Properties().rarity(Rarity.RARE)));
    //    public static final DeferredHolder<Item, Item> HAND_GRENADE = AMMO.register("hand_grenade", HandGrenade::new);
//    public static final DeferredHolder<Item, Item> RGO_GRENADE = AMMO.register("rgo_grenade", RgoGrenade::new);
//    public static final DeferredHolder<Item, Item> CLAYMORE_MINE = AMMO.register("claymore_mine", ClaymoreMine::new);
//    public static final DeferredHolder<Item, Item> C4_BOMB = AMMO.register("c4_bomb", C4Bomb::new);
    public static final DeferredHolder<Item, Item> SMALL_SHELL = AMMO.register("small_shell", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ROCKET_70 = AMMO.register("rocket_70", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> WIRE_GUIDE_MISSILE = AMMO.register("wire_guide_missile", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> BEAM_TEST = AMMO.register("beam_test", BeamTest::new);

    /**
     * items
     */
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ModUtils.MODID);

    //    public static final DeferredHolder<Item, Item> CONTAINER = ITEMS.register("container", ContainerBlockItem::new);
//    public static final DeferredHolder<Item, Item> SENPAI_SPAWN_EGG = ITEMS.register("senpai_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.SENPAI, -11584987, -14014413, new Item.Properties()));
    public static final DeferredHolder<Item, Item> ANCIENT_CPU = ITEMS.register("ancient_cpu", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> PROPELLER = ITEMS.register("propeller", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> LARGE_PROPELLER = ITEMS.register("large_propeller", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> MOTOR = ITEMS.register("motor", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> LARGE_MOTOR = ITEMS.register("large_motor", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> WHEEL = ITEMS.register("wheel", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> TRACK = ITEMS.register("track", () -> new Item(new Item.Properties()));
    //    public static final DeferredHolder<Item, Item> DRONE = ITEMS.register("drone", Drone::new);
//
//    public static final DeferredHolder<Item, Item> MONITOR = ITEMS.register("monitor", Monitor::new);
//
//    public static final DeferredHolder<Item, Item> DETONATOR = ITEMS.register("detonator", Detonator::new);
//    public static final DeferredHolder<Item, Item> TARGET_DEPLOYER = ITEMS.register("target_deployer", TargetDeployer::new);
//    public static final DeferredHolder<Item, Item> KNIFE = ITEMS.register("knife", Knife::new);
//    public static final DeferredHolder<Item, Item> HAMMER = ITEMS.register("hammer", Hammer::new);
    public static final DeferredHolder<Item, Item> CROWBAR = ITEMS.register("crowbar", Crowbar::new);
    //    public static final DeferredHolder<Item, Item> DEFUSER = ITEMS.register("defuser", Defuser::new);
//    public static final DeferredHolder<Item, Item> ARMOR_PLATE = ITEMS.register("armor_plate", ArmorPlate::new);
//
//    public static final DeferredHolder<Item, Item> RU_HELMET_6B47 = ITEMS.register("ru_helmet_6b47", RuHelmet6b47::new);
//    public static final DeferredHolder<Item, Item> RU_CHEST_6B43 = ITEMS.register("ru_chest_6b43", RuChest6b43::new);
//    public static final DeferredHolder<Item, Item> US_HELMET_PASTG = ITEMS.register("us_helmet_pastg", UsHelmetPastg::new);
//    public static final DeferredHolder<Item, Item> US_CHEST_IOTV = ITEMS.register("us_chest_iotv", UsChestIotv::new);
//    public static final DeferredHolder<Item, Item> GE_HELMET_M_35 = ITEMS.register("ge_helmet_m_35", GeHelmetM35::new);
//    public static final DeferredHolder<Item, Item> MORTAR_DEPLOYER = ITEMS.register("mortar_deployer", MortarDeployer::new);
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
    //    public static final DeferredHolder<Item, Item> DOG_TAG = ITEMS.register("dog_tag", DogTag::new);
    public static final DeferredHolder<Item, Item> CELL = ITEMS.register("cell", () -> new BatteryItem(24000, new Item.Properties()));
    public static final DeferredHolder<Item, Item> BATTERY = ITEMS.register("battery", () -> new BatteryItem(100000, new Item.Properties()));
    public static final DeferredHolder<Item, Item> SMALL_BATTERY_PACK = ITEMS.register("small_battery_pack", () -> new BatteryItem(500000, new Item.Properties()));
    public static final DeferredHolder<Item, Item> MEDIUM_BATTERY_PACK = ITEMS.register("medium_battery_pack", () -> new BatteryItem(5000000, new Item.Properties()));
    public static final DeferredHolder<Item, Item> LARGE_BATTERY_PACK = ITEMS.register("large_battery_pack", () -> new BatteryItem(20000000, new Item.Properties()));
//    public static final DeferredHolder<Item, Item> TRANSCRIPT = ITEMS.register("transcript", Transcript::new);
    public static final DeferredHolder<Item, FiringParameters> FIRING_PARAMETERS = ITEMS.register("firing_parameters", FiringParameters::new);

    public static final DeferredHolder<Item, Item> TUNGSTEN_ROD = ITEMS.register("tungsten_rod", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> IRON_BARREL = ITEMS.register("iron_barrel", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> IRON_ACTION = ITEMS.register("iron_action", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> IRON_TRIGGER = ITEMS.register("iron_trigger", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> IRON_SPRING = ITEMS.register("iron_spring", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STEEL_BARREL = ITEMS.register("steel_barrel", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> STEEL_ACTION = ITEMS.register("steel_action", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> STEEL_TRIGGER = ITEMS.register("steel_trigger", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> STEEL_SPRING = ITEMS.register("steel_spring", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> CEMENTED_CARBIDE_BARREL = ITEMS.register("cemented_carbide_barrel", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, Item> CEMENTED_CARBIDE_ACTION = ITEMS.register("cemented_carbide_action", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, Item> CEMENTED_CARBIDE_TRIGGER = ITEMS.register("cemented_carbide_trigger", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, Item> CEMENTED_CARBIDE_SPRING = ITEMS.register("cemented_carbide_spring", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
//    public static final DeferredHolder<Item, Item> NETHERITE_BARREL = ITEMS.register("netherite_barrel", () -> new Item(new Item.Properties().rarity(RarityTool.LEGENDARY)));
//    public static final DeferredHolder<Item, Item> NETHERITE_ACTION = ITEMS.register("netherite_action", () -> new Item(new Item.Properties().rarity(RarityTool.LEGENDARY)));
//    public static final DeferredHolder<Item, Item> NETHERITE_TRIGGER = ITEMS.register("netherite_trigger", () -> new Item(new Item.Properties().rarity(RarityTool.LEGENDARY)));
//    public static final DeferredHolder<Item, Item> NETHERITE_SPRING = ITEMS.register("netherite_spring", () -> new Item(new Item.Properties().rarity(RarityTool.LEGENDARY)));

    public static final DeferredHolder<Item, Item> COMMON_MATERIAL_PACK = ITEMS.register("common_material_pack", () -> new MaterialPack(Rarity.COMMON));
    public static final DeferredHolder<Item, Item> RARE_MATERIAL_PACK = ITEMS.register("rare_material_pack", () -> new MaterialPack(Rarity.RARE));
    public static final DeferredHolder<Item, Item> EPIC_MATERIAL_PACK = ITEMS.register("epic_material_pack", () -> new MaterialPack(Rarity.EPIC));
//    public static final DeferredHolder<Item, Item> LEGENDARY_MATERIAL_PACK = ITEMS.register("legendary_material_pack", () -> new MaterialPack(RarityTool.LEGENDARY));

    public static final DeferredHolder<Item, Item> TRACHELIUM_BLUEPRINT = ITEMS.register("trachelium_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, Item> GLOCK_17_BLUEPRINT = ITEMS.register("glock_17_blueprint", () -> new BlueprintItem(Rarity.COMMON));
    public static final DeferredHolder<Item, Item> MP_443_BLUEPRINT = ITEMS.register("mp_443_blueprint", () -> new BlueprintItem(Rarity.COMMON));
    public static final DeferredHolder<Item, Item> GLOCK_18_BLUEPRINT = ITEMS.register("glock_18_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, Item> HUNTING_RIFLE_BLUEPRINT = ITEMS.register("hunting_rifle_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, Item> M_79_BLUEPRINT = ITEMS.register("m_79_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, Item> RPG_BLUEPRINT = ITEMS.register("rpg_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, Item> BOCEK_BLUEPRINT = ITEMS.register("bocek_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, Item> M_4_BLUEPRINT = ITEMS.register("m_4_blueprint", () -> new BlueprintItem(Rarity.RARE));
    //    public static final DeferredHolder<Item, Item> AA_12_BLUEPRINT = ITEMS.register("aa_12_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));
    public static final DeferredHolder<Item, Item> HK_416_BLUEPRINT = ITEMS.register("hk_416_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, Item> RPK_BLUEPRINT = ITEMS.register("rpk_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, Item> SKS_BLUEPRINT = ITEMS.register("sks_blueprint", () -> new BlueprintItem(Rarity.RARE));
    //    public static final DeferredHolder<Item, Item> NTW_20_BLUEPRINT = ITEMS.register("ntw_20_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));
    public static final DeferredHolder<Item, Item> VECTOR_BLUEPRINT = ITEMS.register("vector_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    //    public static final DeferredHolder<Item, Item> MINIGUN_BLUEPRINT = ITEMS.register("minigun_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));
    public static final DeferredHolder<Item, Item> MK_14_BLUEPRINT = ITEMS.register("mk_14_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    //    public static final DeferredHolder<Item, Item> SENTINEL_BLUEPRINT = ITEMS.register("sentinel_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));
    public static final DeferredHolder<Item, Item> M_60_BLUEPRINT = ITEMS.register("m_60_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, Item> SVD_BLUEPRINT = ITEMS.register("svd_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, Item> MARLIN_BLUEPRINT = ITEMS.register("marlin_blueprint", () -> new BlueprintItem(Rarity.COMMON));
    public static final DeferredHolder<Item, Item> M_870_BLUEPRINT = ITEMS.register("m_870_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, Item> M_98B_BLUEPRINT = ITEMS.register("m_98b_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, Item> AK_47_BLUEPRINT = ITEMS.register("ak_47_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, Item> AK_12_BLUEPRINT = ITEMS.register("ak_12_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, Item> DEVOTION_BLUEPRINT = ITEMS.register("devotion_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, Item> TASER_BLUEPRINT = ITEMS.register("taser_blueprint", () -> new BlueprintItem(Rarity.COMMON));
    public static final DeferredHolder<Item, Item> M_1911_BLUEPRINT = ITEMS.register("m_1911_blueprint", () -> new BlueprintItem(Rarity.COMMON));
    public static final DeferredHolder<Item, Item> QBZ_95_BLUEPRINT = ITEMS.register("qbz_95_blueprint", () -> new BlueprintItem(Rarity.EPIC));
    public static final DeferredHolder<Item, Item> K_98_BLUEPRINT = ITEMS.register("k_98_blueprint", () -> new BlueprintItem(Rarity.RARE));
    public static final DeferredHolder<Item, Item> MOSIN_NAGANT_BLUEPRINT = ITEMS.register("mosin_nagant_blueprint", () -> new BlueprintItem(Rarity.RARE));
    //    public static final DeferredHolder<Item, Item> JAVELIN_BLUEPRINT = ITEMS.register("javelin_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));
    public static final DeferredHolder<Item, Item> M_2_HB_BLUEPRINT = ITEMS.register("m2hb_blueprint", () -> new BlueprintItem(Rarity.RARE));
//    public static final DeferredHolder<Item, Item> SECONDARY_CATACLYSM_BLUEPRINT = ITEMS.register("secondary_cataclysm_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));
//    public static final DeferredHolder<Item, Item> MK_42_BLUEPRINT = ITEMS.register("mk_42_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));
//    public static final DeferredHolder<Item, Item> MLE_1934_BLUEPRINT = ITEMS.register("mle_1934_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));
//    public static final DeferredHolder<Item, Item> ANNIHILATOR_BLUEPRINT = ITEMS.register("annihilator_blueprint", () -> new BlueprintItem(RarityTool.LEGENDARY));

    public static final DeferredHolder<Item, Item> LIGHT_ARMAMENT_MODULE = ITEMS.register("light_armament_module", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, Item> MEDIUM_ARMAMENT_MODULE = ITEMS.register("medium_armament_module", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
//    public static final DeferredHolder<Item, Item> HEAVY_ARMAMENT_MODULE = ITEMS.register("heavy_armament_module", () -> new Item(new Item.Properties().rarity(RarityTool.LEGENDARY)));

    /**
     * Block
     */
    public static final DeferredRegister<Item> BLOCKS = DeferredRegister.create(BuiltInRegistries.ITEM, ModUtils.MODID);

    public static final DeferredHolder<Item, Item> GALENA_ORE = block(ModBlocks.GALENA_ORE);
    public static final DeferredHolder<Item, Item> DEEPSLATE_GALENA_ORE = block(ModBlocks.DEEPSLATE_GALENA_ORE);
    public static final DeferredHolder<Item, Item> SCHEELITE_ORE = block(ModBlocks.SCHEELITE_ORE);
    public static final DeferredHolder<Item, Item> DEEPSLATE_SCHEELITE_ORE = block(ModBlocks.DEEPSLATE_SCHEELITE_ORE);
    public static final DeferredHolder<Item, Item> SILVER_ORE = block(ModBlocks.SILVER_ORE);
    public static final DeferredHolder<Item, Item> DEEPSLATE_SILVER_ORE = block(ModBlocks.DEEPSLATE_SILVER_ORE);
    public static final DeferredHolder<Item, Item> JUMP_PAD = block(ModBlocks.JUMP_PAD);
    public static final DeferredHolder<Item, Item> SANDBAG = block(ModBlocks.SANDBAG);
    public static final DeferredHolder<Item, Item> BARBED_WIRE = block(ModBlocks.BARBED_WIRE);
    public static final DeferredHolder<Item, Item> DRAGON_TEETH = block(ModBlocks.DRAGON_TEETH);
    public static final DeferredHolder<Item, Item> REFORGING_TABLE = block(ModBlocks.REFORGING_TABLE);
    public static final DeferredHolder<Item, Item> CHARGING_STATION = block(ModBlocks.CHARGING_STATION);
    public static final DeferredHolder<Item, CreativeChargingStationBlockItem> CREATIVE_CHARGING_STATION = BLOCKS.register("creative_charging_station", CreativeChargingStationBlockItem::new);
    public static final DeferredHolder<Item, Item> LEAD_BLOCK = block(ModBlocks.LEAD_BLOCK);
    public static final DeferredHolder<Item, Item> STEEL_BLOCK = block(ModBlocks.STEEL_BLOCK);
    public static final DeferredHolder<Item, Item> TUNGSTEN_BLOCK = block(ModBlocks.TUNGSTEN_BLOCK);
    public static final DeferredHolder<Item, Item> SILVER_BLOCK = block(ModBlocks.SILVER_BLOCK);
    public static final DeferredHolder<Item, Item> CEMENTED_CARBIDE_BLOCK = block(ModBlocks.CEMENTED_CARBIDE_BLOCK);
    public static final DeferredHolder<Item, Item> FUMO_25 = block(ModBlocks.FUMO_25);

    /**
     * Perk Items
     */
    public static final DeferredRegister<Item> PERKS = DeferredRegister.create(BuiltInRegistries.ITEM, ModUtils.MODID);

    public static void registerPerkItems() {
        ModPerks.AMMO_PERKS.getEntries().stream().filter(p -> p != ModPerks.AP_BULLET)
                .forEach(registryObject -> PERKS.register(registryObject.getId().getPath(), () -> new PerkItem<>(registryObject)));
        ModPerks.FUNC_PERKS.getEntries().forEach(registryObject -> PERKS.register(registryObject.getId().getPath(), () -> new PerkItem<>(registryObject)));
        ModPerks.DAMAGE_PERKS.getEntries().forEach(registryObject -> PERKS.register(registryObject.getId().getPath(), () -> new PerkItem<>(registryObject)));
    }

    public static final DeferredHolder<Item, Item> SHORTCUT_PACK = PERKS.register("shortcut_pack", ShortcutPack::new);
    public static final DeferredHolder<Item, Item> EMPTY_PERK = PERKS.register("empty_perk", () -> new Item(new Item.Properties()));
    /**
     * 单独注册，用于Tab图标，不要删
     */
    public static final DeferredHolder<Item, Item> AP_BULLET = PERKS.register("ap_bullet", () -> new PerkItem<>(ModPerks.AP_BULLET));

    private static DeferredHolder<Item, Item> block(DeferredHolder<Block, Block> block) {
        return BLOCKS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        GUNS.register(bus);
        AMMO.register(bus);
        BLOCKS.register(bus);
        registerPerkItems();
        PERKS.register(bus);

        REGISTRY.register(bus);
    }

}
