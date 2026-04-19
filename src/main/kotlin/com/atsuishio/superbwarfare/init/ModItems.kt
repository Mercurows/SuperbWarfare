package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity
import com.atsuishio.superbwarfare.item.*
import com.atsuishio.superbwarfare.item.ammo.*
import com.atsuishio.superbwarfare.item.armor.*
import com.atsuishio.superbwarfare.item.blockitem.BlueprintResearchTableBlockItem
import com.atsuishio.superbwarfare.item.blockitem.ChargingStationBlockItem
import com.atsuishio.superbwarfare.item.blockitem.CreativeChargingStationBlockItem
import com.atsuishio.superbwarfare.item.blockitem.VehicleAssemblingTableBlockItem
import com.atsuishio.superbwarfare.item.container.ContainerBlockItem
import com.atsuishio.superbwarfare.item.container.LuckyContainerBlockItem
import com.atsuishio.superbwarfare.item.container.SmallContainerBlockItem
import com.atsuishio.superbwarfare.item.curio.DogTagItem
import com.atsuishio.superbwarfare.item.curio.IffItem
import com.atsuishio.superbwarfare.item.curio.ParachuteItem
import com.atsuishio.superbwarfare.item.curio.ThermalImagingGogglesItem
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.item.gun.handgun.*
import com.atsuishio.superbwarfare.item.gun.launcher.*
import com.atsuishio.superbwarfare.item.gun.machinegun.*
import com.atsuishio.superbwarfare.item.gun.rifle.*
import com.atsuishio.superbwarfare.item.gun.shotgun.Aa12Item
import com.atsuishio.superbwarfare.item.gun.shotgun.HomemadeShotgunItem
import com.atsuishio.superbwarfare.item.gun.shotgun.M870Item
import com.atsuishio.superbwarfare.item.gun.smg.Mp5Item
import com.atsuishio.superbwarfare.item.gun.smg.VectorItem
import com.atsuishio.superbwarfare.item.gun.sniper.*
import com.atsuishio.superbwarfare.item.gun.special.BocekItem
import com.atsuishio.superbwarfare.item.gun.special.RepairToolItem
import com.atsuishio.superbwarfare.item.gun.special.TaserItem
import com.atsuishio.superbwarfare.item.gun.vehicle.VehicleGun
import com.atsuishio.superbwarfare.item.material.*
import com.atsuishio.superbwarfare.item.misc.*
import com.atsuishio.superbwarfare.item.projectile.*
import com.atsuishio.superbwarfare.item.weapon.*
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.tiers.ModItemTier
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.*
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DispenserBlock
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.DeferredSpawnEggItem
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

@Suppress("unused")
object ModItems {

    val LEGENDARY: Rarity by lazy { ModRarities.LEGENDARY }
    val SUPERB: Rarity by lazy { ModRarities.SUPERB }
    val VIRTUAL: Rarity by lazy { ModRarities.VIRTUAL }

    /**
     * guns
     */
    private fun <T : GunItem> registerGun(id: String, gun: () -> T): DeferredHolder<Item, T> = GUNS.register(id, gun)

    @JvmField
    val GUNS: DeferredRegister<Item> = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID)

    // @formatter:off
    @JvmField val REPAIR_TOOL = registerGun("repair_tool") { RepairToolItem() }
    @JvmField val TASER = registerGun("taser") { TaserItem() }
    @JvmField val GLOCK_17 = registerGun("glock_17") { Glock17Item() }
    @JvmField val GLOCK_18 = registerGun("glock_18") { Glock18Item() }
    @JvmField val MP_443 = registerGun("mp_443") { Mp443Item() }
    @JvmField val M_1911 = registerGun("m_1911") { M1911Item() }
    @JvmField val HOMEMADE_SHOTGUN = registerGun("homemade_shotgun") { HomemadeShotgunItem() }
    @JvmField val TRACHELIUM = registerGun("trachelium") { TracheliumItem() }
    @JvmField val MP_5 = registerGun("mp_5") { Mp5Item() }
    @JvmField val VECTOR = registerGun("vector") { VectorItem() }
    @JvmField val AK_47 = registerGun("ak_47") { AK47Item() }
    @JvmField val AK_12 = registerGun("ak_12") { AK12Item() }
    @JvmField val SKS = registerGun("sks") { SksItem() }
    @JvmField val M_4 = registerGun("m_4") { M4Item() }
    @JvmField val HK_416 = registerGun("hk_416") { Hk416Item() }
    @JvmField val QBZ_95 = registerGun("qbz_95") { Qbz95Item() }
    @JvmField val QBZ_191 = registerGun("qbz_191") { Qbz191Item() }
    @JvmField val INSIDIOUS = registerGun("insidious") { InsidiousItem() }
    @JvmField val MK_14 = registerGun("mk_14") { Mk14Item() }
    @JvmField val QL_1031 = registerGun("ql_1031") { Ql1031Item() }
    @JvmField val MARLIN = registerGun("marlin") { MarlinItem() }
    @JvmField val K_98 = registerGun("k_98") { K98Item() }
    @JvmField val MOSIN_NAGANT = registerGun("mosin_nagant") { MosinNagantItem() }
    @JvmField val SVD = registerGun("svd") { SvdItem() }
    @JvmField val AWM = registerGun("awm") { AwmItem() }
    @JvmField val M_98B = registerGun("m_98b") { M98bItem() }
    @JvmField val SENTINEL = registerGun("sentinel") { SentinelItem() }
    @JvmField val HUNTING_RIFLE = registerGun("hunting_rifle") { HuntingRifleItem() }
    @JvmField val NTW_20 = registerGun("ntw_20") { Ntw20Item() }
    @JvmField val M_870 = registerGun("m_870") { M870Item() }
    @JvmField val AA_12 = registerGun("aa_12") { Aa12Item() }
    @JvmField val DEVOTION = registerGun("devotion") { DevotionItem() }
    @JvmField val RPK = registerGun("rpk") { RpkItem() }
    @JvmField val M_60 = registerGun("m_60") { M60Item() }
    @JvmField val M_2_HB = registerGun("m_2_hb") { M2HBItem() }
    @JvmField val MINIGUN = registerGun("minigun") { MinigunItem() }
    @JvmField val M_79 = registerGun("m_79") { M79Item() }
    @JvmField val SECONDARY_CATACLYSM = registerGun("secondary_cataclysm") { SecondaryCataclysmItem() }
    @JvmField val RPG = registerGun("rpg") { RpgItem() }
    @JvmField val JAVELIN = registerGun("javelin") { JavelinItem() }
    @JvmField val IGLA_9K38 = registerGun("igla_9k38") { IglaItem() }
    @JvmField val BOCEK = registerGun("bocek") { BocekItem() }
    @JvmField val SUPER_STAR_SHOOTER = registerGun("super_star_shooter") { SuperStarShooterItem() }

    @JvmField val VEHICLE_GUN = registerGun("vehicle_gun") { VehicleGun() }
    // @formatter:on

    /**
     * Ammo
     */
    private fun registerAmmo(id: String) = registerAmmo(id) { Item(Properties()) }
    private fun <T : Item> registerAmmo(id: String, ammo: () -> T): DeferredHolder<Item, T> = AMMO.register(id, ammo)

    @JvmField
    val AMMO: DeferredRegister<Item> = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID)

    // @formatter:off
    @JvmField val HANDGUN_AMMO = registerAmmo("handgun_ammo") { AmmoSupplierItem(Ammo.HANDGUN, 1, Properties()) }
    @JvmField val RIFLE_AMMO = registerAmmo("rifle_ammo") { AmmoSupplierItem(Ammo.RIFLE, 1, Properties()) }
    @JvmField val SNIPER_AMMO = registerAmmo("sniper_ammo") { AmmoSupplierItem(Ammo.SNIPER, 1, Properties()) }
    @JvmField val SHOTGUN_AMMO = registerAmmo("shotgun_ammo") { AmmoSupplierItem(Ammo.SHOTGUN, 1, Properties()) }
    @JvmField val HEAVY_AMMO = registerAmmo("heavy_ammo") { AmmoSupplierItem(Ammo.HEAVY, 1, Properties()) }
    @JvmField val HANDGUN_AMMO_BOX = registerAmmo("handgun_ammo_box") { HandgunAmmoBoxItem() }
    @JvmField val RIFLE_AMMO_BOX = registerAmmo("rifle_ammo_box") { RifleAmmoBoxItem() }
    @JvmField val SNIPER_AMMO_BOX = registerAmmo("sniper_ammo_box") { SniperAmmoBoxItem() }
    @JvmField val SHOTGUN_AMMO_BOX = registerAmmo("shotgun_ammo_box") { ShotgunAmmoBoxItem() }
    @JvmField val CREATIVE_AMMO_BOX = registerAmmo("creative_ammo_box") { CreativeAmmoBoxItem() }
    @JvmField val AMMO_BOX = registerAmmo("ammo_box") { AmmoBoxItem() }
    @JvmField val TASER_ELECTRODE = registerAmmo("taser_electrode")
    @JvmField val GRENADE_40MM = registerAmmo("grenade_40mm")

    @JvmField val MORTAR_SHELL = registerAmmo("mortar_shell") { MortarShellItem() }
    @JvmField val MORTAR_SHELL_WP = registerAmmo("mortar_shell_wp") { MortarShellItem() }
    @JvmField val POTION_MORTAR_SHELL = registerAmmo("potion_mortar_shell") { PotionMortarShellItem() }
    @JvmField val RPG_ROCKET_STANDARD = registerAmmo("rpg_rocket_standard") { RpgRocketStandardItem() }
    @JvmField val RPG_ROCKET_TBG = registerAmmo("rpg_rocket_tbg") { RpgRocketTBGItem() }
    @JvmField val JAVELIN_MISSILE = registerAmmo("javelin_missile") { Item(Properties().stacksTo(4)) }
    @JvmField val LUNGE_MINE = registerAmmo("lunge_mine") { LungeMine() }
    @JvmField val SMALL_SHELL_AP = registerAmmo("small_shell_ap")
    @JvmField val SMALL_SHELL_HE = registerAmmo("small_shell_he")
    @JvmField val SMALL_SHELL_GS = registerAmmo("small_shell_gs")
    @JvmField val SMALL_SHELL_AA = registerAmmo("small_shell_aa")
    @JvmField val LARGE_SHELL_AP = registerAmmo("large_shell_ap") { Item(Properties().rarity(Rarity.RARE)) }
    @JvmField val LARGE_SHELL_HE = registerAmmo("large_shell_he") { Item(Properties().rarity(Rarity.RARE)) }
    @JvmField val LARGE_SHELL_CM = registerAmmo("large_shell_cm") { Item(Properties().rarity(Rarity.RARE)) }
    @JvmField val LARGE_SHELL_GS = registerAmmo("large_shell_gs") { Item(Properties().rarity(Rarity.RARE)) }
    @JvmField val LARGE_SHELL_WP = registerAmmo("large_shell_wp") { Item(Properties().rarity(Rarity.RARE)) }
    @JvmField val HAND_GRENADE = registerAmmo("hand_grenade") { HandGrenade() }
    @JvmField val RGO_GRENADE = registerAmmo("rgo_grenade") { RgoGrenade() }
    @JvmField val M18_SMOKE_GRENADE = registerAmmo("m18_smoke_grenade") { M18SmokeGrenadeItem() }
    @JvmField val CLAYMORE_MINE = registerAmmo("claymore_mine") { ClaymoreMineItem() }
    @JvmField val TM_62 = registerAmmo("tm_62") { Tm62Item() }
    @JvmField val PTKM_1R = registerAmmo("ptkm_1r") { Ptkm1rItem() }
    @JvmField val C4_BOMB = registerAmmo("c4_bomb") { C4BombItem() }
    @JvmField val BLU_43_MINE = registerAmmo("blu_43_mine") { Blu43MineItem() }
    @JvmField val EDD = registerAmmo("edd") { EDDItem() }
    @JvmField val SMALL_ROCKET = registerAmmo("small_rocket") { Item(Properties().stacksTo(16)) }
    @JvmField val MEDIUM_ROCKET_AP =
        registerAmmo("medium_rocket_ap") { MediumRocketItem(500f, 6f, 100f, 0f, 0, MediumRocketEntity.Type.AP, 0) }
    @JvmField val MEDIUM_ROCKET_HE =
        registerAmmo("medium_rocket_he") { MediumRocketItem(200f, 12f, 200f, 0.2f, 40, MediumRocketEntity.Type.HE, 0) }
    @JvmField val MEDIUM_ROCKET_CM =
        registerAmmo("medium_rocket_cm") { MediumRocketItem(300f, 12f, 300f, 0f, 0, MediumRocketEntity.Type.CM, 20) }
    @JvmField val MEDIUM_ANTI_AIR_MISSILE = registerAmmo("medium_anti_air_missile") { Item(Properties().stacksTo(4)) }
    @JvmField val MEDIUM_ANTI_GROUND_MISSILE = registerAmmo("medium_anti_ground_missile") { Item(Properties().stacksTo(4)) }
    @JvmField val LARGE_ANTI_GROUND_MISSILE = registerAmmo("large_anti_ground_missile") { Item(Properties().stacksTo(2)) }
    @JvmField val SWARM_DRONE = registerAmmo("swarm_drone") { Item(Properties().stacksTo(14)) }
    @JvmField val SMALL_AERIAL_BOMB = registerAmmo("small_aerial_bomb") { Item(Properties().stacksTo(4)) }
    @JvmField val MEDIUM_AERIAL_BOMB = registerAmmo("medium_aerial_bomb") { Item(Properties().stacksTo(2)) }
    // @formatter:on

    /**
     * items
     */
    private fun registerItem(id: String) = registerItem(id) { Item(Properties()) }
    private fun <T : Item> registerItem(id: String, item: () -> T): DeferredHolder<Item, T> = ITEMS.register(id, item)
    private fun registerBlueprint(id: String, rarity: Rarity) = registerItem(id) { BlueprintItem(rarity) }

    @JvmField
    val ITEMS: DeferredRegister<Item> = DeferredRegister.create(Registries.ITEM, Mod.MODID)

    // @formatter:off
    @JvmField val SENPAI_SPAWN_EGG = registerItem("senpai_spawn_egg") {
        DeferredSpawnEggItem(
            Supplier { ModEntities.SENPAI.value() },
            -11584987,
            -14014413,
            Properties()
        )
    }
    @JvmField val STEEL_COIL_SPAWN_EGG = registerItem("steel_coil_spawn_egg") {
        DeferredSpawnEggItem(ModEntities.STEEL_COIL, 0, 0xc0c0c0, Properties())
    }
    @JvmField val ANCIENT_CPU = registerItem("ancient_cpu") { Item(Properties().rarity(Rarity.RARE)) }
    @JvmField val PROPELLER = registerItem("propeller")
    @JvmField val LARGE_PROPELLER = registerItem("large_propeller")
    @JvmField val MOTOR = registerItem("motor")
    @JvmField val LARGE_MOTOR = registerItem("large_motor")
    @JvmField val WHEEL = registerItem("wheel")
    @JvmField val TRACK = registerItem("track")
    @JvmField val DRONE = registerItem("drone") { DroneItem() }

    @JvmField val MONITOR = registerItem("monitor") { MonitorItem() }
    @JvmField val ARTILLERY_INDICATOR = registerItem("artillery_indicator") { ArtilleryIndicatorItem() }

    @JvmField val DETONATOR = registerItem("detonator") { DetonatorItem() }
    @JvmField val TARGET_DEPLOYER = registerItem("target_deployer") { TargetDeployerItem() }
    @JvmField val DPS_GENERATOR_DEPLOYER = registerItem("dps_generator_deployer") { DPSGeneratorDeployerItem() }
    @JvmField val KNIFE = registerItem("knife") {
        SwordItem(
            ModItemTier.STEEL,
            CustomDamageProperty(1200).attributes(SwordItem.createAttributes(ModItemTier.STEEL, 0, -1.8f))
        )
    }
    @JvmField val HAMMER = registerItem("hammer") { HammerItem(Tiers.IRON, 11, -3.2f, Properties().durability(400)) }
    @JvmField val GOLDEN_HAMMER = registerItem("golden_hammer") { HammerItem(Tiers.GOLD, 11, -3.2f, Properties().durability(150)) }
    @JvmField val STEEL_HAMMER = registerItem("steel_hammer") { HammerItem(ModItemTier.STEEL, 9, -3.2f, Properties().durability(600)) }
    @JvmField val DIAMOND_HAMMER = registerItem("diamond_hammer") { HammerItem(Tiers.DIAMOND, 12, -3.2f, Properties().durability(1500)) }
    @JvmField val CEMENTED_CARBIDE_HAMMER = registerItem("cemented_carbide_hammer") { HammerItem(ModItemTier.CEMENTED_CARBIDE, 8, -3.2f, Properties().durability(2000)) }
    @JvmField val NETHERITE_HAMMER = registerItem("netherite_hammer") { NetheriteHammerItem() }

    @JvmField val T_BATON = registerItem("t_baton") { TBatonItem() }
    @JvmField val ELECTRIC_BATON = registerItem("electric_baton") { ElectricBatonItem() }
    @JvmField val STEEL_PIPE = registerItem("steel_pipe") { SteelPipeItem() }
    @JvmField val CROWBAR = registerItem("crowbar") { CrowbarItem() }
    @JvmField val MILITARY_SHOVEL = registerItem("military_shovel") { MilitaryShovelItem() }
    @JvmField val DEFUSER = registerItem("defuser") { DefuserItem() }
    @JvmField val ARMOR_PLATE = registerItem("armor_plate") { ArmorPlateItem() }

    @JvmField val RU_HELMET_6B47 = registerItem("ru_helmet_6b47") { RuHelmet6b47Item() }
    @JvmField val RU_CHEST_6B43 = registerItem("ru_chest_6b43") { RuChest6b43Item() }
    @JvmField val US_HELMET_PASGT = registerItem("us_helmet_pasgt") { UsHelmetPasgtItem() }
    @JvmField val US_CHEST_IOTV = registerItem("us_chest_iotv") { UsChestIotvItem() }
    @JvmField val GE_HELMET_M_35 = registerItem("ge_helmet_m_35") { GeHelmetM35Item() }
    @JvmField val PARACHUTE = registerItem("parachute") { ParachuteItem() }
    @JvmField val THERMAL_IMAGING_GOGGLES = registerItem("thermal_imaging_goggles") { ThermalImagingGogglesItem() }

    @JvmField val MORTAR_DEPLOYER = registerItem("mortar_deployer") { MortarDeployerItem() }
    @JvmField val MORTAR_BARREL = registerItem("mortar_barrel")
    @JvmField val MORTAR_BASE_PLATE = registerItem("mortar_base_plate")
    @JvmField val MORTAR_BIPOD = registerItem("mortar_bipod")
    @JvmField val TOW_DEPLOYER = registerItem("tow_deployer") { TowDeployerItem() }
    @JvmField val SEEKER = registerItem("seeker")
    @JvmField val MISSILE_ENGINE = registerItem("missile_engine")
    @JvmField val FUSEE = registerItem("fusee")
    @JvmField val PRIMER = registerItem("primer")
    @JvmField val AP_HEAD = registerItem("ap_head")
    @JvmField val HE_HEAD = registerItem("he_head")
    @JvmField val CM_HEAD = registerItem("cm_head")
    @JvmField val GS_HEAD = registerItem("gs_head")
    @JvmField val WP_HEAD = registerItem("wp_head")
    @JvmField val CANNON_CORE = registerItem("cannon_core")
    @JvmField val COPPER_PLATE = registerItem("copper_plate")
    @JvmField val STEEL_INGOT = registerItem("steel_ingot")
    @JvmField val LEAD_INGOT = registerItem("lead_ingot")
    @JvmField val SILVER_INGOT = registerItem("silver_ingot")
    @JvmField val TUNGSTEN_INGOT = registerItem("tungsten_ingot")
    @JvmField val CEMENTED_CARBIDE_INGOT = registerItem("cemented_carbide_ingot")
    @JvmField val HIGH_ENERGY_EXPLOSIVES = registerItem("high_energy_explosives")
    @JvmField val GRAIN = registerItem("grain")
    @JvmField val IRON_POWDER = registerItem("iron_powder")
    @JvmField val TUNGSTEN_POWDER = registerItem("tungsten_powder")
    @JvmField val COAL_POWDER = registerItem("coal_powder")
    @JvmField val COAL_IRON_POWDER = registerItem("coal_iron_powder")
    @JvmField val RAW_CEMENTED_CARBIDE_POWDER = registerItem("raw_cemented_carbide_powder")
    @JvmField val GALENA = registerItem("galena")
    @JvmField val SCHEELITE = registerItem("scheelite")
    @JvmField val RAW_SILVER = registerItem("raw_silver")
    @JvmField val DOG_TAG = registerItem("dog_tag") { DogTagItem() }
    @JvmField val IFF = registerItem("iff") { IffItem() }
    @JvmField val CELL = registerItem("cell") { BatteryItem(24000, Properties()) }
    @JvmField val BATTERY = registerItem("battery") { BatteryItem(100000, Properties()) }
    @JvmField val SMALL_BATTERY_PACK = registerItem("small_battery_pack") { BatteryItem(500000, Properties()) }
    @JvmField val MEDIUM_BATTERY_PACK = registerItem("medium_battery_pack") { BatteryItem(5000000, Properties()) }
    @JvmField val LARGE_BATTERY_PACK = registerItem("large_battery_pack") { BatteryItem(20000000, Properties()) }
    @JvmField val LASER_UNIT = registerItem("laser_unit")
    @JvmField val BEAST = registerItem("beast") { BeastItem() }
    @JvmField val TRANSCRIPT = registerItem("transcript") { TranscriptItem() }
    @JvmField val FIRING_PARAMETERS = registerItem("firing_parameters") { FiringParametersItem() }
    @JvmField val MEDICAL_KIT = registerItem("medical_kit") { MedicalKitItem() }
    @JvmField val VEHICLE_DAMAGE_ANALYZER = registerItem("vehicle_damage_analyzer") { VehicleDamageAnalyzerItem() }
    @JvmField val VEHICLE_RESET_KIT = registerItem("vehicle_reset_kit") { VehicleResetKitItem() }

    @JvmField val TUNGSTEN_ROD = registerItem("tungsten_rod")

    @JvmField val IRON_MATERIALS = registerMaterials("iron")
    @JvmField val STEEL_MATERIALS = registerMaterials("steel")
    @JvmField val CEMENTED_CARBIDE_MATERIALS = registerMaterials("cemented_carbide")
    @JvmField val NETHERITE_MATERIALS = registerMaterials("netherite")
    @JvmField val CRYSTAL_MATERIALS = registerMaterials("crystal")

    @JvmField val COMMON_MATERIAL_PACK = registerItem("common_material_pack") { MaterialPackItem(Rarity.COMMON) }
    @JvmField val RARE_MATERIAL_PACK = registerItem("rare_material_pack") { MaterialPackItem(Rarity.RARE) }
    @JvmField val EPIC_MATERIAL_PACK = registerItem("epic_material_pack") { MaterialPackItem(Rarity.EPIC) }
    @JvmField val LEGENDARY_MATERIAL_PACK = registerItem("legendary_material_pack") { MaterialPackItem(LEGENDARY) }
    @JvmField val SUPERB_MATERIAL_PACK = registerItem("superb_material_pack") { MaterialPackItem(SUPERB) }
    @JvmField val VIRTUAL_MATERIAL_PACK = registerItem("virtual_material_pack") { MaterialPackItem(VIRTUAL) }

    @JvmField val LIGHT_ARMAMENT_MODULE = registerItem("light_armament_module") { Item(Properties().rarity(Rarity.RARE)) }
    @JvmField val MEDIUM_ARMAMENT_MODULE = registerItem("medium_armament_module") { Item(Properties().rarity(Rarity.EPIC)) }
    @JvmField val HEAVY_ARMAMENT_MODULE = registerItem("heavy_armament_module") { Item(Properties().rarity(LEGENDARY)) }

    @JvmField val DATA_CHIP_SUBSTRATE = registerItem("data_chip_substrate")
    @JvmField val COMMON_BLUEPRINT_DATA_CHIP = registerItem("common_blueprint_data_chip")
    @JvmField val RARE_BLUEPRINT_DATA_CHIP = registerItem("rare_blueprint_data_chip") { Item(Properties().rarity(Rarity.RARE)) }
    @JvmField val EPIC_BLUEPRINT_DATA_CHIP = registerItem("epic_blueprint_data_chip") { Item(Properties().rarity(Rarity.EPIC)) }
    @JvmField val LEGENDARY_BLUEPRINT_DATA_CHIP = registerItem("legendary_blueprint_data_chip") { Item(Properties().rarity(LEGENDARY)) }
    @JvmField val SUPERB_BLUEPRINT_DATA_CHIP = registerItem("superb_blueprint_data_chip") { Item(Properties().rarity(SUPERB)) }
    @JvmField val VIRTUAL_BLUEPRINT_DATA_CHIP = registerItem("virtual_blueprint_data_chip") { Item(Properties().rarity(VIRTUAL)) }

    @JvmField val AMMO_PERK_DATA_CHIP = registerItem("ammo_perk_data_chip")
    @JvmField val FUNCTIONAL_PERK_DATA_CHIP = registerItem("functional_perk_data_chip")
    @JvmField val DAMAGE_PERK_DATA_CHIP = registerItem("damage_perk_data_chip")

    @JvmField val DIRECTIONAL_RESEARCH_MODULE = registerItem("directional_research_module") { Item(Properties().rarity(Rarity.EPIC)) }
    @JvmField val ENLARGEMENT_RESEARCH_MODULE = registerItem("enlargement_research_module") { Item(Properties().rarity(Rarity.EPIC)) }
    @JvmField val EFFECTIVE_RESEARCH_MODULE = registerItem("effective_research_module") { Item(Properties().rarity(Rarity.RARE)) }
    @JvmField val BOOST_RESEARCH_MODULE = registerItem("boost_research_module") { Item(Properties().rarity(Rarity.RARE)) }

    @JvmField val TRACHELIUM_BLUEPRINT = registerBlueprint("trachelium_blueprint", VIRTUAL)
    @JvmField val GLOCK_17_BLUEPRINT = registerBlueprint("glock_17_blueprint", Rarity.COMMON)
    @JvmField val MP_443_BLUEPRINT = registerBlueprint("mp_443_blueprint", Rarity.COMMON)
    @JvmField val GLOCK_18_BLUEPRINT = registerBlueprint("glock_18_blueprint", Rarity.RARE)
    @JvmField val HUNTING_RIFLE_BLUEPRINT = registerBlueprint("hunting_rifle_blueprint", Rarity.RARE)
    @JvmField val M_79_BLUEPRINT = registerBlueprint("m_79_blueprint", Rarity.RARE)
    @JvmField val RPG_BLUEPRINT = registerBlueprint("rpg_blueprint", Rarity.RARE)
    @JvmField val BOCEK_BLUEPRINT = registerBlueprint("bocek_blueprint", Rarity.EPIC)
    @JvmField val M_4_BLUEPRINT = registerBlueprint("m_4_blueprint", Rarity.RARE)
    @JvmField val AA_12_BLUEPRINT = registerBlueprint("aa_12_blueprint", LEGENDARY)
    @JvmField val HK_416_BLUEPRINT = registerBlueprint("hk_416_blueprint", Rarity.RARE)
    @JvmField val RPK_BLUEPRINT = registerBlueprint("rpk_blueprint", Rarity.EPIC)
    @JvmField val SKS_BLUEPRINT = registerBlueprint("sks_blueprint", Rarity.RARE)
    @JvmField val NTW_20_BLUEPRINT = registerBlueprint("ntw_20_blueprint", LEGENDARY)
    @JvmField val MP_5_BLUEPRINT = registerBlueprint("mp_5_blueprint", Rarity.RARE)
    @JvmField val VECTOR_BLUEPRINT = registerBlueprint("vector_blueprint", Rarity.EPIC)
    @JvmField val MINIGUN_BLUEPRINT = registerBlueprint("minigun_blueprint", LEGENDARY)
    @JvmField val MK_14_BLUEPRINT = registerBlueprint("mk_14_blueprint", Rarity.EPIC)
    @JvmField val SENTINEL_BLUEPRINT = registerBlueprint("sentinel_blueprint", Rarity.EPIC)
    @JvmField val M_60_BLUEPRINT = registerBlueprint("m_60_blueprint", Rarity.EPIC)
    @JvmField val SVD_BLUEPRINT = registerBlueprint("svd_blueprint", Rarity.EPIC)
    @JvmField val MARLIN_BLUEPRINT = registerBlueprint("marlin_blueprint", Rarity.COMMON)
    @JvmField val M_870_BLUEPRINT = registerBlueprint("m_870_blueprint", Rarity.RARE)
    @JvmField val AWM_BLUEPRINT = registerBlueprint("awm_blueprint", Rarity.EPIC)
    @JvmField val M_98B_BLUEPRINT = registerBlueprint("m_98b_blueprint", Rarity.EPIC)
    @JvmField val AK_47_BLUEPRINT = registerBlueprint("ak_47_blueprint", Rarity.RARE)
    @JvmField val AK_12_BLUEPRINT = registerBlueprint("ak_12_blueprint", Rarity.RARE)
    @JvmField val DEVOTION_BLUEPRINT = registerBlueprint("devotion_blueprint", Rarity.EPIC)
    @JvmField val TASER_BLUEPRINT = registerBlueprint("taser_blueprint", Rarity.COMMON)
    @JvmField val M_1911_BLUEPRINT = registerBlueprint("m_1911_blueprint", Rarity.COMMON)
    @JvmField val QBZ_95_BLUEPRINT = registerBlueprint("qbz_95_blueprint", Rarity.RARE)
    @JvmField val QBZ_191_BLUEPRINT = registerBlueprint("qbz_191_blueprint", Rarity.EPIC)
    @JvmField val K_98_BLUEPRINT = registerBlueprint("k_98_blueprint", Rarity.RARE)
    @JvmField val MOSIN_NAGANT_BLUEPRINT = registerBlueprint("mosin_nagant_blueprint", Rarity.RARE)
    @JvmField val IGLA_BLUEPRINT = registerBlueprint("igla_9k38_blueprint", Rarity.EPIC)
    @JvmField val JAVELIN_BLUEPRINT = registerBlueprint("javelin_blueprint", LEGENDARY)
    @JvmField val M_2_HB_BLUEPRINT = registerBlueprint("m_2_hb_blueprint", Rarity.RARE)
    @JvmField val SECONDARY_CATACLYSM_BLUEPRINT = registerBlueprint("secondary_cataclysm_blueprint", VIRTUAL)
    @JvmField val INSIDIOUS_BLUEPRINT = registerBlueprint("insidious_blueprint", Rarity.EPIC)
    @JvmField val QL_1031_BLUEPRINT = registerBlueprint("ql_1031_blueprint", VIRTUAL)
    @JvmField val SUPER_STAR_SHOOTER_BLUEPRINT = registerBlueprint("super_star_shooter_blueprint", SUPERB)

    @JvmField val MK_42_BLUEPRINT = registerBlueprint("mk_42_blueprint", LEGENDARY)
    @JvmField val MLE_1934_BLUEPRINT = registerBlueprint("mle_1934_blueprint", LEGENDARY)
    @JvmField val BL_132_BLUEPRINT = registerBlueprint("bl_132_blueprint", LEGENDARY)
    @JvmField val HPJ_11_BLUEPRINT = registerBlueprint("hpj_11_blueprint", LEGENDARY)
    @JvmField val ANNIHILATOR_BLUEPRINT = registerBlueprint("annihilator_blueprint", LEGENDARY)
    // @formatter:on

    /**
     * Block
     */

    private fun <T : Block> registerBlock(block: DeferredHolder<Block, T>) =
        registerBlock(block.id.path) { BlockItem(block.get(), Properties()) }

    private fun registerBlock(id: String, block: () -> BlockItem): DeferredHolder<Item, BlockItem> =
        BLOCKS.register(id, block)

    @JvmField
    val BLOCKS: DeferredRegister<Item> = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID)

    // @formatter:off
    @JvmField val GALENA_ORE = registerBlock(ModBlocks.GALENA_ORE)
    @JvmField val DEEPSLATE_GALENA_ORE = registerBlock(ModBlocks.DEEPSLATE_GALENA_ORE)
    @JvmField val SCHEELITE_ORE = registerBlock(ModBlocks.SCHEELITE_ORE)
    @JvmField val DEEPSLATE_SCHEELITE_ORE = registerBlock(ModBlocks.DEEPSLATE_SCHEELITE_ORE)
    @JvmField val SILVER_ORE = registerBlock(ModBlocks.SILVER_ORE)
    @JvmField val DEEPSLATE_SILVER_ORE = registerBlock(ModBlocks.DEEPSLATE_SILVER_ORE)
    @JvmField val RAW_GALENA_BLOCK = registerBlock(ModBlocks.RAW_GALENA_BLOCK)
    @JvmField val RAW_SCHEELITE_BLOCK = registerBlock(ModBlocks.RAW_SCHEELITE_BLOCK)
    @JvmField val RAW_SILVER_BLOCK = registerBlock(ModBlocks.RAW_SILVER_BLOCK)
    @JvmField val JUMP_PAD = registerBlock(ModBlocks.JUMP_PAD)
    @JvmField val SANDBAG = registerBlock(ModBlocks.SANDBAG)
    @JvmField val BARBED_WIRE = registerBlock(ModBlocks.BARBED_WIRE)
    @JvmField val DRAGON_TEETH = registerBlock(ModBlocks.DRAGON_TEETH)
    @JvmField val REFORGING_TABLE = registerBlock(ModBlocks.REFORGING_TABLE)
    @JvmField val CHARGING_STATION = registerBlock("charging_station") { ChargingStationBlockItem() }
    @JvmField val CREATIVE_CHARGING_STATION = registerBlock("creative_charging_station") { CreativeChargingStationBlockItem() }
    @JvmField val LEAD_BLOCK = registerBlock(ModBlocks.LEAD_BLOCK)
    @JvmField val STEEL_BLOCK = registerBlock(ModBlocks.STEEL_BLOCK)
    @JvmField val TUNGSTEN_BLOCK = registerBlock(ModBlocks.TUNGSTEN_BLOCK)
    @JvmField val SILVER_BLOCK = registerBlock(ModBlocks.SILVER_BLOCK)
    @JvmField val CEMENTED_CARBIDE_BLOCK = registerBlock(ModBlocks.CEMENTED_CARBIDE_BLOCK)
    @JvmField val FUMO_25 = registerBlock(ModBlocks.FUMO_25)
    @JvmField val VEHICLE_DEPLOYER = registerBlock(ModBlocks.VEHICLE_DEPLOYER.id.path) {
        BlockItem(ModBlocks.VEHICLE_DEPLOYER.get(), Properties().stacksTo(1).rarity(Rarity.EPIC))
    }
    @JvmField val AIRCRAFT_CATAPULT = registerBlock(ModBlocks.AIRCRAFT_CATAPULT)
    @JvmField val SUPERB_ITEM_INTERFACE = registerBlock(ModBlocks.SUPERB_ITEM_INTERFACE)
    @JvmField val CREATIVE_SUPERB_ITEM_INTERFACE = registerBlock(ModBlocks.CREATIVE_SUPERB_ITEM_INTERFACE.id.path) {
        BlockItem(ModBlocks.CREATIVE_SUPERB_ITEM_INTERFACE.get(), Properties().rarity(Rarity.EPIC))
    }
    @JvmField val VEHICLE_ASSEMBLING_TABLE = registerBlock("vehicle_assembling_table") { VehicleAssemblingTableBlockItem() }
    @JvmField val BLUEPRINT_RESEARCH_TABLE = registerBlock("blueprint_research_table") { BlueprintResearchTableBlockItem() }
    @JvmField val BIOGAS_GENERATOR = registerBlock(ModBlocks.BIOGAS_GENERATOR)
    // @formatter:on

    /**
     * Vehicle
     */
    private fun registerVehicle(id: String) = registerVehicle(id) { Item(Properties()) }
    private fun <T : Item> registerVehicle(id: String, item: () -> T): DeferredHolder<Item, T> =
        VEHICLES.register(id, item)

    // @formatter:off
    @JvmField val VEHICLES: DeferredRegister<Item> = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID)

    @JvmField val CONTAINER = registerVehicle("container") { ContainerBlockItem() }
    @JvmField val SMALL_CONTAINER = registerVehicle("small_container") { SmallContainerBlockItem() }
    @JvmField val LUCKY_CONTAINER = registerVehicle("lucky_container") { LuckyContainerBlockItem() }
    // @formatter:on

    @JvmRecord
    data class Materials(
        val name: String,
        val barrel: DeferredHolder<Item, Item>,
        val action: DeferredHolder<Item, Item>,
        val spring: DeferredHolder<Item, Item>,
        val trigger: DeferredHolder<Item, Item>,
    )

    private fun registerMaterials(name: String): Materials {
        return Materials(
            name,
            registerItem(name + "_barrel"),
            registerItem(name + "_action"),
            registerItem(name + "_spring"),
            registerItem(name + "_trigger"),
        )
    }

    /**
     * Perk Items
     */

    private fun <T : Item> registerPerkItem(id: String, item: () -> T): DeferredHolder<Item, T> =
        PERKS.register(id, item)

    @JvmField
    val PERK_ITEMS: MutableMap<DeferredHolder<Perk, out Perk>, DeferredHolder<Item, out PerkItem<*>>> =
        mutableMapOf()

    @JvmField
    val PERKS: DeferredRegister<Item> = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID)

    /**
     * 单独注册，用于Tab图标，不要删
     */
    // @formatter:off
    @JvmField var AP_BULLET: DeferredHolder<Item, out PerkItem<*>>? = null
    @JvmField var INTELLIGENT_CHIP: DeferredHolder<Item, out PerkItem<*>>? = null
    // @formatter:on

    private fun registerPerkItems() {
        ModPerks.AMMO_PERKS.entries.forEach { registerSinglePerkItem(it) }
        ModPerks.FUNC_PERKS.entries.forEach { registerSinglePerkItem(it) }
        ModPerks.DAMAGE_PERKS.entries.forEach { registerSinglePerkItem(it) }

        AP_BULLET = PERK_ITEMS[ModPerks.AP_BULLET]
        INTELLIGENT_CHIP = PERK_ITEMS[ModPerks.INTELLIGENT_CHIP]
    }

    private fun registerSinglePerkItem(perk: DeferredHolder<Perk, out Perk>) {
        PERK_ITEMS[perk] = registerPerkItem(perk.id.path) { PerkItem(perk) }
    }

    // @formatter:off
    @JvmField val SHORTCUT_PACK = registerPerkItem("shortcut_pack") { ShortcutPackItem() }
    @JvmField val EMPTY_PERK = registerPerkItem("empty_perk") { Item(Properties()) }
    // @formatter:on

    fun registerDispenserBehavior() {
        val list = mutableListOf<DeferredHolder<Item, out Item>>()
        list.addAll(AMMO.entries)
        list.addAll(ITEMS.entries)

        for (i in list) {
            val item = i.get()
            if (item is ProjectileItem) {
                DispenserBlock.registerProjectileBehavior(item)
            }
            if (item is DispenserLaunchable) {
                DispenserBlock.registerBehavior(item, item.getLaunchBehavior())
            }
        }
    }

    fun register(bus: IEventBus) {
        ITEMS.register(bus)
        GUNS.register(bus)
        AMMO.register(bus)
        BLOCKS.register(bus)
        VEHICLES.register(bus)
        registerPerkItems()
        PERKS.register(bus)
    }
}
