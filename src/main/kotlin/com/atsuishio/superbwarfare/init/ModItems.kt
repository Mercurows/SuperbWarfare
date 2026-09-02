package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity
import com.atsuishio.superbwarfare.item.*
import com.atsuishio.superbwarfare.item.ammo.*
import com.atsuishio.superbwarfare.item.armor.*
import com.atsuishio.superbwarfare.item.attachment.AttachmentItem
import com.atsuishio.superbwarfare.item.blockitem.BlueprintResearchTableBlockItem
import com.atsuishio.superbwarfare.item.blockitem.ChargingStationBlockItem
import com.atsuishio.superbwarfare.item.blockitem.CreativeChargingStationBlockItem
import com.atsuishio.superbwarfare.item.blockitem.VehicleAssemblingTableBlockItem
import com.atsuishio.superbwarfare.item.container.ContainerBlockItem
import com.atsuishio.superbwarfare.item.container.LuckyContainerBlockItem
import com.atsuishio.superbwarfare.item.container.SmallContainerBlockItem
import com.atsuishio.superbwarfare.item.curio.*
import com.atsuishio.superbwarfare.item.food.CrustItem
import com.atsuishio.superbwarfare.item.gun.EmptyGunItem
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
import com.atsuishio.superbwarfare.item.gun.special.*
import com.atsuishio.superbwarfare.item.gun.vehicle.VehicleGunItem
import com.atsuishio.superbwarfare.item.material.*
import com.atsuishio.superbwarfare.item.misc.*
import com.atsuishio.superbwarfare.item.projectile.*
import com.atsuishio.superbwarfare.item.weapon.*
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.tiers.ModItemTier
import com.atsuishio.superbwarfare.tools.camelToSnake
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.*
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.SwordItem.createAttributes
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DispenserBlock
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.DeferredSpawnEggItem
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

private typealias ItemRegister = DeferredRegister<Item>

private typealias ItemRegistry<T> = DeferredHolder<Item, T>
private typealias BlockRegistry<T> = DeferredHolder<Block, T>
private typealias PerkRegistry<T> = DeferredHolder<Perk, T>

private typealias SpawnEggItem = DeferredSpawnEggItem

@Suppress("unused")
object ModItems {

    val LEGENDARY: Rarity by lazy { ModRarities.LEGENDARY }
    val SUPERB: Rarity by lazy { ModRarities.SUPERB }
    val VIRTUAL: Rarity by lazy { ModRarities.VIRTUAL }

    /**
     * 返回 [cls] 的单例（object）实例；否则通过无参构造函数创建新实例。
     * 支持 object 或 class 两种物品类写法，失败时给出明确的注册错误信息。
     */
    private fun <T : Any> getInstanceOrCreate(cls: KClass<T>): T {
        return cls.objectInstance ?: try {
            cls.createInstance()
        } catch (e: Exception) {
            throw IllegalStateException(
                "Cannot create instance of ${cls.simpleName} for item registration: " +
                        "the class must be an object or have a public no-arg constructor, " +
                        "or pass an explicit supplier instead.",
                e
            )
        }
    }

    /**
     * guns
     */
    private inline fun <reified T : GunItem> registerGun(
        id: String = T::class.java.simpleName.substringBeforeLast("Item").camelToSnake(),
        noinline gunSupplier: () -> T = { getInstanceOrCreate(T::class) }
    ) = registerItem(id, GUNS, gunSupplier)

    @JvmField
    val GUNS: ItemRegister = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID)

    // @formatter:off
    @JvmField val REPAIR_TOOL = registerGun<RepairToolItem>("repair_tool")
    @JvmField val TASER = registerGun<TaserItem>("taser")
    @JvmField val GLOCK_17 = registerGun<Glock17Item>("glock_17")
    @JvmField val GLOCK_18 = registerGun<Glock18Item>("glock_18")
    @JvmField val MP_443 = registerGun<Mp443Item>("mp_443")
    @JvmField val M_1911 = registerGun<M1911Item>("m_1911")
    @JvmField val HOMEMADE_SHOTGUN = registerGun<HomemadeShotgunItem>("homemade_shotgun")
    @JvmField val NAIL_GUN = registerGun<NailGunItem>("nail_gun")
    @JvmField val TRACHELIUM = registerGun<TracheliumItem>("trachelium")
    @JvmField val MP_5 = registerGun<Mp5Item>("mp_5")
    @JvmField val VECTOR = registerGun<VectorItem>("vector")
    @JvmField val AK_47 = registerGun<AK47Item>("ak_47")
    @JvmField val AK_47_V2 = registerGun<AK47V2Item>("ak_47_v2")
    @JvmField val AK_12 = registerGun<AK12Item>("ak_12")
    @JvmField val SKS = registerGun<SksItem>("sks")
    @JvmField val M_4 = registerGun<M4Item>("m_4")
    @JvmField val HK_416 = registerGun<Hk416Item>("hk_416")
    @JvmField val QBZ_95 = registerGun<Qbz95Item>("qbz_95")
    @JvmField val QBZ_191 = registerGun<Qbz191Item>("qbz_191")
    @JvmField val INSIDIOUS = registerGun<InsidiousItem>("insidious")
    @JvmField val MK_14 = registerGun<Mk14Item>("mk_14")
    @JvmField val QL_1031 = registerGun<Ql1031Item>("ql_1031")
    @JvmField val MARLIN = registerGun<MarlinItem>("marlin")
    @JvmField val K_98 = registerGun<K98Item>("k_98")
    @JvmField val MOSIN_NAGANT = registerGun<MosinNagantItem>("mosin_nagant")
    @JvmField val SVD = registerGun<SvdItem>("svd")
    @JvmField val AWM = registerGun<AwmItem>("awm")
    @JvmField val M_98B = registerGun<M98bItem>("m_98b")
    @JvmField val SENTINEL = registerGun<SentinelItem>("sentinel")
    @JvmField val HUNTING_RIFLE = registerGun<HuntingRifleItem>("hunting_rifle")
    @JvmField val NTW_20 = registerGun<Ntw20Item>("ntw_20")
    @JvmField val M_870 = registerGun<M870Item>("m_870")
    @JvmField val AA_12 = registerGun<Aa12Item>("aa_12")
    @JvmField val DEVOTION = registerGun<DevotionItem>("devotion")
    @JvmField val RPK = registerGun<RpkItem>("rpk")
    @JvmField val M_60 = registerGun<M60Item>("m_60")
    @JvmField val M_2_HB = registerGun<M2HBItem>("m_2_hb")
    @JvmField val MINIGUN = registerGun<MinigunItem>("minigun")
    @JvmField val M_79 = registerGun<M79Item>("m_79")
    @JvmField val SECONDARY_CATACLYSM = registerGun<SecondaryCataclysmItem>("secondary_cataclysm")
    @JvmField val RPG = registerGun<RpgItem>("rpg")
    @JvmField val JAVELIN = registerGun<JavelinItem>("javelin")
    @JvmField val IGLA_9K38 = registerGun<IglaItem>("igla_9k38")
    @JvmField val BOCEK = registerGun<BocekItem>("bocek")
    @JvmField val SUPER_STAR_SHOOTER = registerGun<SuperStarShooterItem>("super_star_shooter")
    @JvmField val REFORGING = registerGun<ReforgingItem>("reforging")

    @JvmField val BEAST_GUN_TEST = registerGun<BeastGunTestItem>("beast_gun_test")

    @JvmField val VEHICLE_GUN = registerGun<VehicleGunItem>("vehicle_gun")
    @JvmField val EMPTY_GUN = registerGun<EmptyGunItem>("empty_gun")
    // @formatter:on

    /**
     * Ammo
     */
    private fun registerAmmo(id: String) = registerAmmo(id) { Item(Properties()) }

    @JvmName("registerAmmoByClass")
    private inline fun <reified T : Item> registerAmmo(
        id: String = T::class.java.simpleName.substringBeforeLast("Item").camelToSnake(),
        noinline ammo: () -> T = { getInstanceOrCreate(T::class) }
    ): ItemRegistry<T> = AMMO.register(id, ammo)

    @JvmField
    val AMMO: ItemRegister = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID)

    // @formatter:off
    @JvmField val HANDGUN_AMMO = registerAmmo("handgun_ammo") { AmmoSupplierItem(Ammo.HANDGUN, 1, Properties()) }
    @JvmField val RIFLE_AMMO = registerAmmo("rifle_ammo") { AmmoSupplierItem(Ammo.RIFLE, 1, Properties()) }
    @JvmField val SNIPER_AMMO = registerAmmo("sniper_ammo") { AmmoSupplierItem(Ammo.SNIPER, 1, Properties()) }
    @JvmField val SHOTGUN_AMMO = registerAmmo("shotgun_ammo") { AmmoSupplierItem(Ammo.SHOTGUN, 1, Properties()) }
    @JvmField val HEAVY_AMMO = registerAmmo("heavy_ammo") { AmmoSupplierItem(Ammo.HEAVY, 1, Properties()) }
    @JvmField val HANDGUN_AMMO_BOX = registerAmmo<HandgunAmmoBoxItem>("handgun_ammo_box")
    @JvmField val RIFLE_AMMO_BOX = registerAmmo<RifleAmmoBoxItem>("rifle_ammo_box")
    @JvmField val SNIPER_AMMO_BOX = registerAmmo<SniperAmmoBoxItem>("sniper_ammo_box")
    @JvmField val SHOTGUN_AMMO_BOX = registerAmmo<ShotgunAmmoBoxItem>("shotgun_ammo_box")
    @JvmField val CREATIVE_AMMO_BOX = registerAmmo<CreativeAmmoBoxItem>("creative_ammo_box")
    @JvmField val AMMO_BOX = registerAmmo<AmmoBoxItem>("ammo_box")
    @JvmField val TASER_ELECTRODE = registerAmmo("taser_electrode")
    @JvmField val GRENADE_40MM = registerAmmo("grenade_40mm")
    @JvmField val FLYING_FLARE_AMMO = registerAmmo("flying_flare_ammo")
    @JvmField val VEHICLE_SMOKE_AMMO = registerAmmo("vehicle_smoke_ammo")

    @JvmField val MORTAR_SHELL = registerAmmo<MortarShellItem>("mortar_shell")
    @JvmField val MORTAR_SHELL_WP = registerAmmo<MortarShellItem>("mortar_shell_wp")
    @JvmField val MORTAR_SHELL_SMOKE = registerAmmo<SmokeMortarShellItem>("mortar_shell_smoke")
    @JvmField val POTION_MORTAR_SHELL = registerAmmo<PotionMortarShellItem>("potion_mortar_shell")
    @JvmField val RPG_ROCKET_STANDARD = registerAmmo<RpgRocketStandardItem>("rpg_rocket_standard")
    @JvmField val RPG_ROCKET_TBG = registerAmmo<RpgRocketTBGItem>("rpg_rocket_tbg")
    @JvmField val JAVELIN_MISSILE = registerAmmo("javelin_missile") { Item(Properties().stacksTo(4)) }
    @JvmField val LUNGE_MINE = registerAmmo<LungeMine>("lunge_mine")
    @JvmField val SMALL_SHELL_AP = registerAmmo("small_shell_ap")
    @JvmField val SMALL_SHELL_HE = registerAmmo("small_shell_he")
    @JvmField val SMALL_SHELL_GS = registerAmmo("small_shell_gs")
    @JvmField val SMALL_SHELL_AA = registerAmmo("small_shell_aa")
    @JvmField val MEDIUM_SHELL_AP = registerAmmo("medium_shell_ap")
    @JvmField val MEDIUM_SHELL_HE = registerAmmo("medium_shell_he")
    @JvmField val MEDIUM_SHELL_GS = registerAmmo("medium_shell_gs")
    @JvmField val MEDIUM_SHELL_AA = registerAmmo("medium_shell_aa")
    @JvmField val LARGE_SHELL_AP = registerAmmo("large_shell_ap") { Item(Properties().rarity(Rarity.RARE)) }
    @JvmField val LARGE_SHELL_HE = registerAmmo("large_shell_he") { Item(Properties().rarity(Rarity.RARE)) }
    @JvmField val LARGE_SHELL_CM = registerAmmo("large_shell_cm") { Item(Properties().rarity(Rarity.RARE)) }
    @JvmField val LARGE_SHELL_GS = registerAmmo("large_shell_gs") { Item(Properties().rarity(Rarity.RARE)) }
    @JvmField val LARGE_SHELL_WP = registerAmmo("large_shell_wp") { Item(Properties().rarity(Rarity.RARE)) }
    @JvmField val HAND_GRENADE = registerAmmo<HandGrenade>("hand_grenade")
    @JvmField val RGO_GRENADE = registerAmmo<RgoGrenade>("rgo_grenade")
    @JvmField val M18_SMOKE_GRENADE = registerAmmo<M18SmokeGrenadeItem>("m18_smoke_grenade")
    @JvmField val CLAYMORE_MINE = registerAmmo<ClaymoreMineItem>("claymore_mine")
    @JvmField val TM_62 = registerAmmo<Tm62Item>("tm_62")
    @JvmField val PTKM_1R = registerAmmo<Ptkm1rItem>("ptkm_1r")
    @JvmField val C4_BOMB = registerAmmo<C4BombItem>("c4_bomb")
    @JvmField val BLU_43_MINE = registerAmmo<Blu43MineItem>("blu_43_mine")
    @JvmField val EDD = registerAmmo<EDDItem>("edd")
    @JvmField val SMALL_ROCKET = registerAmmo("small_rocket") { Item(Properties().stacksTo(16)) }
    @JvmField val MEDIUM_ROCKET_AP =
        registerAmmo("medium_rocket_ap") { MediumRocketItem(500f, 6f, 100f, 0f, 0, MediumRocketEntity.Type.AP, 0) }
    @JvmField val MEDIUM_ROCKET_HE =
        registerAmmo("medium_rocket_he") { MediumRocketItem(200f, 12f, 200f, 0.2f, 40, MediumRocketEntity.Type.HE, 0) }
    @JvmField val MEDIUM_ROCKET_CM =
        registerAmmo("medium_rocket_cm") { MediumRocketItem(300f, 12f, 300f, 0f, 0, MediumRocketEntity.Type.CM, 20) }
    @JvmField val MEDIUM_ANTI_AIR_MISSILE = registerAmmo("medium_anti_air_missile") { Item(Properties().stacksTo(4)) }
    @JvmField val MEDIUM_ANTI_GROUND_MISSILE = registerAmmo("medium_anti_ground_missile") { Item(Properties().stacksTo(4)) }
    @JvmField val LARGE_ANTI_AIR_MISSILE = registerAmmo("large_anti_air_missile") { Item(Properties().stacksTo(2)) }
    @JvmField val LARGE_ANTI_GROUND_MISSILE = registerAmmo("large_anti_ground_missile") { Item(Properties().stacksTo(2)) }
    @JvmField val EXTRA_LARGE_ANTI_GROUND_MISSILE = registerAmmo("extra_large_anti_ground_missile") { Item(Properties().stacksTo(1)) }
    @JvmField val SWARM_DRONE = registerAmmo("swarm_drone") { Item(Properties().stacksTo(14)) }
    @JvmField val SMALL_AERIAL_BOMB = registerAmmo("small_aerial_bomb") { Item(Properties().stacksTo(4)) }
    @JvmField val MEDIUM_AERIAL_BOMB = registerAmmo("medium_aerial_bomb") { Item(Properties().stacksTo(2)) }
    @JvmField val LARGE_AERIAL_BOMB = registerAmmo("large_aerial_bomb") { Item(Properties().stacksTo(1)) }
    // @formatter:on

    /**
     * items
     */
    private fun registerItem(id: String) = registerItem(id) { Item(Properties()) }

    @JvmName("registerItemByClass")
    private inline fun <reified T : Item> registerItem(
        id: String = T::class.java.simpleName.substringBeforeLast("Item").camelToSnake(),
        register: ItemRegister = ITEMS,
        noinline itemSupplier: () -> T = { getInstanceOrCreate(T::class) },
    ): ItemRegistry<T> {
        return register.register(id, itemSupplier)
    }

    private fun registerBlueprint(id: String, rarity: Rarity) = registerItem(id) { BlueprintItem(rarity) }

    @JvmField
    val ITEMS: ItemRegister = DeferredRegister.create(Registries.ITEM, Mod.MODID)

    // @formatter:off
    @JvmField val SENPAI_SPAWN_EGG = registerItem("senpai_spawn_egg") {
        SpawnEggItem(
            Supplier { ModEntities.SENPAI.value() },
            -11584987,
            -14014413,
            Properties()
        )
    }
    @JvmField val CREEPING_SENPAI_SPAWN_EGG = registerItem("creeping_senpai_spawn_egg") {
        SpawnEggItem(ModEntities.CREEPING_SENPAI, -11584987, 3288889, Properties())
    }
    @JvmField val STEEL_COIL_SPAWN_EGG = registerItem("steel_coil_spawn_egg") {
        SpawnEggItem(ModEntities.STEEL_COIL, 0, 0xc0c0c0, Properties())
    }
    @JvmField val ANCIENT_CPU = registerItem("ancient_cpu") { Item(Properties().rarity(Rarity.RARE)) }
    @JvmField val PROPELLER = registerItem("propeller")
    @JvmField val LARGE_PROPELLER = registerItem("large_propeller")
    @JvmField val MOTOR = registerItem("motor")
    @JvmField val LARGE_MOTOR = registerItem("large_motor")
    @JvmField val WHEEL = registerItem("wheel")
    @JvmField val TRACK = registerItem("track")
    @JvmField val DRONE = registerItem<DroneItem>("drone")

    @JvmField val MONITOR = registerItem<MonitorItem>("monitor")
    @JvmField val ARTILLERY_INDICATOR = registerItem<ArtilleryIndicatorItem>("artillery_indicator")

    @JvmField val DETONATOR = registerItem<DetonatorItem>("detonator")
    @JvmField val TARGET_DEPLOYER = registerItem<TargetDeployerItem>("target_deployer")
    @JvmField val DPS_GENERATOR_DEPLOYER = registerItem<DPSGeneratorDeployerItem>("dps_generator_deployer")
    @JvmField val KNIFE = registerItem("knife") {
        SwordItem(
            ModItemTier.STEEL,
            CustomDamageProperty(1600).attributes(createAttributes(ModItemTier.STEEL, 4, -1.8f))
        )
    }
    @JvmField val HAMMER = registerItem("hammer") { HammerItem(Tiers.IRON, 11, -3.2f, Properties().durability(400)) }
    @JvmField val GOLDEN_HAMMER = registerItem("golden_hammer") { HammerItem(Tiers.GOLD, 11, -3.2f, Properties().durability(150)) }
    @JvmField val STEEL_HAMMER = registerItem("steel_hammer") { HammerItem(ModItemTier.STEEL, 9, -3.2f, Properties().durability(600)) }
    @JvmField val DIAMOND_HAMMER = registerItem("diamond_hammer") { HammerItem(Tiers.DIAMOND, 12, -3.2f, Properties().durability(1500)) }
    @JvmField val CEMENTED_CARBIDE_HAMMER = registerItem("cemented_carbide_hammer") { HammerItem(ModItemTier.CEMENTED_CARBIDE, 8, -3.2f, Properties().durability(2000)) }
    @JvmField val NETHERITE_HAMMER = registerItem<NetheriteHammerItem>("netherite_hammer")
    @JvmField val CEMENTED_CARBIDE_SWORD = registerItem("cemented_carbide_sword") { SwordItem(ModItemTier.CEMENTED_CARBIDE, Properties().attributes(createAttributes(ModItemTier.CEMENTED_CARBIDE, -2, -2.4f))) }
    @JvmField val CEMENTED_CARBIDE_PICKAXE = registerItem("cemented_carbide_pickaxe") { PickaxeItem(ModItemTier.CEMENTED_CARBIDE, Properties().attributes(createAttributes(ModItemTier.CEMENTED_CARBIDE, -4, -2.8f))) }
    @JvmField val CEMENTED_CARBIDE_AXE = registerItem("cemented_carbide_axe") { AxeItem(ModItemTier.CEMENTED_CARBIDE, Properties().attributes(createAttributes(ModItemTier.CEMENTED_CARBIDE,  0f, -3.0f))) }
    @JvmField val CEMENTED_CARBIDE_SHOVEL = registerItem("cemented_carbide_shovel") { ShovelItem(ModItemTier.CEMENTED_CARBIDE, Properties().attributes(createAttributes(ModItemTier.CEMENTED_CARBIDE, -3.5f, -3.0f))) }
    @JvmField val CEMENTED_CARBIDE_HOE = registerItem("cemented_carbide_hoe") { HoeItem(ModItemTier.CEMENTED_CARBIDE, Properties().attributes(createAttributes(ModItemTier.CEMENTED_CARBIDE, -8, 0.0f))) }

    @JvmField val T_BATON = registerItem<TBatonItem>("t_baton")
    @JvmField val ELECTRIC_BATON = registerItem<ElectricBatonItem>("electric_baton")
    @JvmField val STEEL_PIPE = registerItem<SteelPipeItem>("steel_pipe")
    @JvmField val CROWBAR = registerItem<CrowbarItem>("crowbar")
    @JvmField val MILITARY_SHOVEL = registerItem<MilitaryShovelItem>("military_shovel")
    @JvmField val DEFUSER = registerItem<DefuserItem>("defuser")
    @JvmField val ARMOR_PLATE = registerItem<ArmorPlateItem>("armor_plate")

    @JvmField val RU_HELMET_6B47 = registerItem<RuHelmet6b47Item>("ru_helmet_6b47")
    @JvmField val RU_CHEST_6B43 = registerItem<RuChest6b43Item>("ru_chest_6b43")
    @JvmField val US_HELMET_PASGT = registerItem<UsHelmetPasgtItem>("us_helmet_pasgt")
    @JvmField val US_CHEST_IOTV = registerItem<UsChestIotvItem>("us_chest_iotv")
    @JvmField val GE_HELMET_M_35 = registerItem<GeHelmetM35Item>("ge_helmet_m_35")
    @JvmField val PARACHUTE = registerItem<ParachuteItem>("parachute")
    @JvmField val THERMAL_IMAGING_GOGGLES = registerItem<ThermalImagingGogglesItem>("thermal_imaging_goggles")
    @JvmField val HANDSOME_GOGGLES = registerItem<HandsomeGogglesItem>("handsome_goggles")
    @JvmField val TACTICAL_TERMINAL = registerItem<TacticalTerminalItem>("tactical_terminal")

    @JvmField val SONIC_ABSORBER = registerItem<SonicAbsorberItem>("sonic_absorber")

    @JvmField val CRUST = registerItem<CrustItem>("crust")

    @JvmField val MORTAR_DEPLOYER = registerItem<MortarDeployerItem>("mortar_deployer")
    @JvmField val MORTAR_BARREL = registerItem("mortar_barrel")
    @JvmField val MORTAR_BASE_PLATE = registerItem("mortar_base_plate")
    @JvmField val MORTAR_BIPOD = registerItem("mortar_bipod")
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
    @JvmField val STEEL_PLATE = registerItem("steel_plate")
    @JvmField val ENGINEERING_PLASTIC = registerItem("engineering_plastic")
    @JvmField val STEEL_INGOT = registerItem("steel_ingot")
    @JvmField val LEAD_INGOT = registerItem("lead_ingot")
    @JvmField val SILVER_INGOT = registerItem("silver_ingot")
    @JvmField val TUNGSTEN_INGOT = registerItem("tungsten_ingot")
    @JvmField val URANIUM_INGOT = registerItem("uranium_ingot")
    @JvmField val CEMENTED_CARBIDE_INGOT = registerItem("cemented_carbide_ingot")
    @JvmField val SULFUR = registerItem("sulfur")
    @JvmField val NITER = registerItem("niter")
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
    @JvmField val RAW_URANIUM = registerItem("raw_uranium")
    @JvmField val SLIME_COVERED_LEATHER = registerItem("slime_covered_leather")
    @JvmField val DOG_TAG = registerItem<DogTagItem>("dog_tag")
    @JvmField val IFF = registerItem<IffItem>("iff")
    @JvmField val CELL = registerItem("cell") { BatteryItem(24000, Properties()) }
    @JvmField val BATTERY = registerItem("battery") { BatteryItem(100000, Properties()) }
    @JvmField val SMALL_BATTERY_PACK = registerItem("small_battery_pack") { BatteryItem(500000, Properties()) }
    @JvmField val MEDIUM_BATTERY_PACK = registerItem("medium_battery_pack") { BatteryItem(5000000, Properties()) }
    @JvmField val LARGE_BATTERY_PACK = registerItem("large_battery_pack") { BatteryItem(20000000, Properties()) }
    @JvmField val LASER_UNIT = registerItem("laser_unit")
    @JvmField val BEAST = registerItem<BeastItem>("beast")
    @JvmField val TRANSCRIPT = registerItem<TranscriptItem>("transcript")
    @JvmField val FIRING_PARAMETERS = registerItem<FiringParametersItem>("firing_parameters")
    @JvmField val MEDICAL_KIT = registerItem<MedicalKitItem>("medical_kit")
    @JvmField val VEHICLE_DAMAGE_ANALYZER = registerItem<VehicleDamageAnalyzerItem>("vehicle_damage_analyzer")
    @JvmField val VEHICLE_RESET_KIT = registerItem<VehicleResetKitItem>("vehicle_reset_kit")
    @JvmField val SKIN_SPRAY = registerItem<SkinSprayItem>("skin_spray")
    @JvmField val VEHICLE_KEY = registerItem<VehicleKeyItem>("vehicle_key")
    @JvmField val CREATIVE_VEHICLE_KEY = registerItem<CreativeVehicleKeyItem>("creative_vehicle_key")
    @JvmField val TOWLINE = registerItem<TowlineItem>("towline")
    @JvmField val TOW_BAR = registerItem<TowBarItem>("tow_bar")
    @JvmField val CATAPULT_SHUTTLE = registerItem<CatapultShuttleItem>("catapult_shuttle")

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

    @JvmName("registerBlockByClass")
    private inline fun <reified T : BlockItem> registerBlock(
        id: String = T::class.java.simpleName.substringBeforeLast("BlockItem").camelToSnake(),
        noinline block: () -> T = { getInstanceOrCreate(T::class) }
    ): ItemRegistry<T> = BLOCKS.register(id, block)

    private fun <T : Block> registerBlock(block: BlockRegistry<T>) =
        registerBlock(block.id.path) { BlockItem(block.get(), Properties()) }

    @JvmField
    val BLOCKS: ItemRegister = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID)

    // @formatter:off
    @JvmField val GALENA_ORE = registerBlock(ModBlocks.GALENA_ORE)
    @JvmField val DEEPSLATE_GALENA_ORE = registerBlock(ModBlocks.DEEPSLATE_GALENA_ORE)
    @JvmField val SCHEELITE_ORE = registerBlock(ModBlocks.SCHEELITE_ORE)
    @JvmField val DEEPSLATE_SCHEELITE_ORE = registerBlock(ModBlocks.DEEPSLATE_SCHEELITE_ORE)
    @JvmField val SILVER_ORE = registerBlock(ModBlocks.SILVER_ORE)
    @JvmField val DEEPSLATE_SILVER_ORE = registerBlock(ModBlocks.DEEPSLATE_SILVER_ORE)
    @JvmField val URANIUM_ORE = registerBlock(ModBlocks.URANIUM_ORE)
    @JvmField val DEEPSLATE_URANIUM_ORE = registerBlock(ModBlocks.DEEPSLATE_URANIUM_ORE)
    @JvmField val SULFUR_ORE = registerBlock(ModBlocks.SULFUR_ORE)
    @JvmField val DEEPSLATE_SULFUR_ORE = registerBlock(ModBlocks.DEEPSLATE_SULFUR_ORE)
    @JvmField val NITER_ORE = registerBlock(ModBlocks.NITER_ORE)
    @JvmField val DEEPSLATE_NITER_ORE = registerBlock(ModBlocks.DEEPSLATE_NITER_ORE)
    @JvmField val RAW_GALENA_BLOCK = registerBlock(ModBlocks.RAW_GALENA_BLOCK)
    @JvmField val RAW_SCHEELITE_BLOCK = registerBlock(ModBlocks.RAW_SCHEELITE_BLOCK)
    @JvmField val RAW_SILVER_BLOCK = registerBlock(ModBlocks.RAW_SILVER_BLOCK)
    @JvmField val RAW_URANIUM_BLOCK = registerBlock(ModBlocks.RAW_URANIUM_BLOCK)
    @JvmField val JUMP_PAD = registerBlock(ModBlocks.JUMP_PAD)
    @JvmField val SANDBAG = registerBlock(ModBlocks.SANDBAG)
    @JvmField val BARBED_WIRE = registerBlock(ModBlocks.BARBED_WIRE)
    @JvmField val DRAGON_TEETH = registerBlock(ModBlocks.DRAGON_TEETH)
    @JvmField val REFORGING_TABLE = registerBlock(ModBlocks.REFORGING_TABLE)
    @JvmField val CHARGING_STATION = registerBlock<ChargingStationBlockItem>("charging_station")
    @JvmField val CREATIVE_CHARGING_STATION = registerBlock<CreativeChargingStationBlockItem>("creative_charging_station")
    @JvmField val LEAD_BLOCK = registerBlock(ModBlocks.LEAD_BLOCK)
    @JvmField val STEEL_BLOCK = registerBlock(ModBlocks.STEEL_BLOCK)
    @JvmField val TUNGSTEN_BLOCK = registerBlock(ModBlocks.TUNGSTEN_BLOCK)
    @JvmField val SILVER_BLOCK = registerBlock(ModBlocks.SILVER_BLOCK)
    @JvmField val URANIUM_BLOCK = registerBlock(ModBlocks.URANIUM_BLOCK)
    @JvmField val CEMENTED_CARBIDE_BLOCK = registerBlock(ModBlocks.CEMENTED_CARBIDE_BLOCK)
    @JvmField val SULFUR_BLOCK = registerBlock(ModBlocks.SULFUR_BLOCK)
    @JvmField val NITER_BLOCK = registerBlock(ModBlocks.NITER_BLOCK)
    @JvmField val FUMO_25 = registerBlock(ModBlocks.FUMO_25)
    @JvmField val VEHICLE_DEPLOYER = registerBlock(ModBlocks.VEHICLE_DEPLOYER.id.path) {
        BlockItem(ModBlocks.VEHICLE_DEPLOYER.get(), Properties().stacksTo(1).rarity(Rarity.EPIC))
    }
    @JvmField val AIRCRAFT_CATAPULT = registerBlock(ModBlocks.AIRCRAFT_CATAPULT)
    @JvmField val CATAPULT_CONTROLLER = registerBlock(ModBlocks.CATAPULT_CONTROLLER)
    @JvmField val SUPERB_ITEM_INTERFACE = registerBlock(ModBlocks.SUPERB_ITEM_INTERFACE)
    @JvmField val CREATIVE_SUPERB_ITEM_INTERFACE = registerBlock(ModBlocks.CREATIVE_SUPERB_ITEM_INTERFACE.id.path) {
        BlockItem(ModBlocks.CREATIVE_SUPERB_ITEM_INTERFACE.get(), Properties().rarity(Rarity.EPIC))
    }
    @JvmField val VEHICLE_ASSEMBLING_TABLE = registerBlock<VehicleAssemblingTableBlockItem>("vehicle_assembling_table")
    @JvmField val BLUEPRINT_RESEARCH_TABLE = registerBlock<BlueprintResearchTableBlockItem>("blueprint_research_table")
    @JvmField val BIOGAS_GENERATOR = registerBlock(ModBlocks.BIOGAS_GENERATOR)
    // @formatter:on

    /**
     * Vehicle
     */
    private fun registerVehicle(id: String) = registerVehicle(id) { Item(Properties()) }

    @JvmName("registerVehicleByClass")
    private inline fun <reified T : Item> registerVehicle(
        id: String = T::class.java.simpleName.substringBeforeLast("Item").camelToSnake(),
        noinline item: () -> T = { getInstanceOrCreate(T::class) }
    ): ItemRegistry<T> = VEHICLES.register(id, item)

    // @formatter:off
    @JvmField val VEHICLES: ItemRegister = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID)

    @JvmField val CONTAINER = registerVehicle<ContainerBlockItem>("container")
    @JvmField val SMALL_CONTAINER = registerVehicle<SmallContainerBlockItem>("small_container")
    @JvmField val LUCKY_CONTAINER = registerVehicle<LuckyContainerBlockItem>("lucky_container")
    // @formatter:on

    @JvmRecord
    data class Materials(
        val name: String,
        val barrel: ItemRegistry<Item>,
        val action: ItemRegistry<Item>,
        val spring: ItemRegistry<Item>,
        val trigger: ItemRegistry<Item>,
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

    private fun <T : Item> registerPerkItem(id: String, item: () -> T): ItemRegistry<T> =
        PERKS.register(id, item)

    @JvmField
    val PERK_ITEMS: MutableMap<PerkRegistry<out Perk>, ItemRegistry<out PerkItem>> =
        mutableMapOf()

    @JvmField
    val PERKS: ItemRegister = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID)

    /**
     * 单独注册，用于Tab图标，不要删
     */
    // @formatter:off
    @JvmField var AP_BULLET: ItemRegistry<out PerkItem>? = null
    @JvmField var INTELLIGENT_CHIP: ItemRegistry<out PerkItem>? = null
    // @formatter:on

    private fun registerPerkItems() {
        ModPerks.AMMO_PERKS.entries.forEach { registerSinglePerkItem(it) }
        ModPerks.FUNC_PERKS.entries.forEach { registerSinglePerkItem(it) }
        ModPerks.DAMAGE_PERKS.entries.forEach { registerSinglePerkItem(it) }

        AP_BULLET = PERK_ITEMS[ModPerks.AP_BULLET]
        INTELLIGENT_CHIP = PERK_ITEMS[ModPerks.INTELLIGENT_CHIP]
    }

    private fun registerSinglePerkItem(perk: PerkRegistry<out Perk>) {
        PERK_ITEMS[perk] = registerPerkItem(perk.id.path) { PerkItem { perk.get() } }
    }

    // @formatter:off
    @JvmField val SHORTCUT_PACK = registerPerkItem("shortcut_pack") { ShortcutPackItem() }
    @JvmField val EMPTY_PERK = registerPerkItem("empty_perk") { Item(Properties()) }
    // @formatter:on

    /**
     * Attachment Items
     */
    @JvmField
    val ATTACHMENTS: ItemRegister = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID)

    private fun registerAttachment(id: String, rarity: Rarity = Rarity.COMMON): ItemRegistry<out Item> {
        return ATTACHMENTS.register(id, Supplier { AttachmentItem("${Mod.MODID}:$id", rarity) })
    }

    // @formatter:off
    @JvmField val OEM_STOCK_STANDARD = registerAttachment("oem_stock_standard")
    @JvmField val MAGAZINE_EXTEND = registerAttachment("magazine_extend", Rarity.RARE)
    @JvmField val MAGAZINE_EXTEND_PRO = registerAttachment("magazine_extend_pro", Rarity.EPIC)
    @JvmField val MEOWLENCER = registerAttachment("meowlencer", Rarity.RARE)
    @JvmField val HISSILENCER = registerAttachment("hissilencer", Rarity.RARE)
    @JvmField val RU_SILENCER = registerAttachment("ru_silencer")
    @JvmField val AR_SILENCER = registerAttachment("ar_silencer")
    // @formatter:on

    fun registerDispenserBehavior() {
        val list = mutableListOf<ItemRegistry<out Item>>()
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
        ATTACHMENTS.register(bus)
    }
}
