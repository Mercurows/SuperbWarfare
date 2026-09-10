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
     * 返回 [T] 上 @RegistryName 注解声明的注册名；
     * 类上没有该注解时直接抛出异常，避免静默生成一个错误的注册名。
     */
    private inline fun <reified T : Any> registryNameOf(): String {
        return T::class.java.getAnnotation(RegistryName::class.java)?.value
            ?: error(
                "No @RegistryName annotation found on ${T::class.java.name}. " +
                        "Annotate the class with @RegistryName(\"...\") or pass the registry name explicitly."
            )
    }

    /**
     * guns
     */
    private inline fun <reified T : GunItem> registerGun(
        id: String? = null,
        noinline gunSupplier: () -> T = { getInstanceOrCreate(T::class) }
    ) = registerItem(id ?: registryNameOf<T>(), GUNS, gunSupplier)

    @JvmField
    val GUNS: ItemRegister = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID)

    // @formatter:off
    @JvmField val REPAIR_TOOL = registerGun<RepairToolItem>()
    @JvmField val TASER = registerGun<TaserItem>()
    @JvmField val GLOCK_17 = registerGun<Glock17Item>()
    @JvmField val GLOCK_18 = registerGun<Glock18Item>()
    @JvmField val MP_443 = registerGun<Mp443Item>()
    @JvmField val M_1911 = registerGun<M1911Item>()
    @JvmField val HOMEMADE_SHOTGUN = registerGun<HomemadeShotgunItem>()
    @JvmField val NAIL_GUN = registerGun<NailGunItem>()
    @JvmField val TRACHELIUM = registerGun<TracheliumItem>()
    @JvmField val MP_5 = registerGun<Mp5Item>()
    @JvmField val VECTOR = registerGun<VectorItem>()
    @JvmField val AK_47 = registerGun<AK47Item>()
    @JvmField val AK_12 = registerGun<AK12Item>()
    @JvmField val SKS = registerGun<SksItem>()
    @JvmField val M_4 = registerGun<M4Item>()
    @JvmField val HK_416 = registerGun<Hk416Item>()
    @JvmField val QBZ_95 = registerGun<Qbz95Item>()
    @JvmField val QBZ_191 = registerGun<Qbz191Item>()
    @JvmField val INSIDIOUS = registerGun<InsidiousItem>()
    @JvmField val MK_14 = registerGun<Mk14Item>()
    @JvmField val QL_1031 = registerGun<Ql1031Item>()
    @JvmField val MARLIN = registerGun<MarlinItem>()
    @JvmField val K_98 = registerGun<K98Item>()
    @JvmField val MOSIN_NAGANT = registerGun<MosinNagantItem>()
    @JvmField val SVD = registerGun<SvdItem>()
    @JvmField val AWM = registerGun<AwmItem>()
    @JvmField val M_98B = registerGun<M98bItem>()
    @JvmField val SENTINEL = registerGun<SentinelItem>()
    @JvmField val HUNTING_RIFLE = registerGun<HuntingRifleItem>()
    @JvmField val NTW_20 = registerGun<Ntw20Item>()
    @JvmField val M_870 = registerGun<M870Item>()
    @JvmField val AA_12 = registerGun<Aa12Item>()
    @JvmField val DEVOTION = registerGun<DevotionItem>()
    @JvmField val RPK = registerGun<RpkItem>()
    @JvmField val M_60 = registerGun<M60Item>()
    @JvmField val M_2_HB = registerGun<M2HBItem>()
    @JvmField val MINIGUN = registerGun<MinigunItem>()
    @JvmField val M_79 = registerGun<M79Item>()
    @JvmField val SECONDARY_CATACLYSM = registerGun<SecondaryCataclysmItem>()
    @JvmField val RPG = registerGun<RpgItem>()
    @JvmField val JAVELIN = registerGun<JavelinItem>()
    @JvmField val IGLA_9K38 = registerGun<IglaItem>()
    @JvmField val BOCEK = registerGun<BocekItem>()
    @JvmField val SUPER_STAR_SHOOTER = registerGun<SuperStarShooterItem>()
    @JvmField val REFORGING = registerGun<ReforgingItem>()

    @JvmField val BEAST_GUN_TEST = registerGun<BeastGunTestItem>()

    @JvmField val VEHICLE_GUN = registerGun<VehicleGunItem>()
    @JvmField val EMPTY_GUN = registerGun<EmptyGunItem>()
    // @formatter:on

    /**
     * Ammo
     */
    private fun registerAmmo(id: String) = registerAmmo(id) { Item(Properties()) }

    @JvmName("registerAmmoByClass")
    private inline fun <reified T : Item> registerAmmo(
        id: String? = null,
        noinline ammo: () -> T = { getInstanceOrCreate(T::class) }
    ): ItemRegistry<T> = AMMO.register(id ?: registryNameOf<T>(), ammo)

    @JvmField
    val AMMO: ItemRegister = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID)

    // @formatter:off
    @JvmField val HANDGUN_AMMO = registerAmmo("handgun_ammo") { AmmoSupplierItem(Ammo.HANDGUN, 1, Properties()) }
    @JvmField val RIFLE_AMMO = registerAmmo("rifle_ammo") { AmmoSupplierItem(Ammo.RIFLE, 1, Properties()) }
    @JvmField val SNIPER_AMMO = registerAmmo("sniper_ammo") { AmmoSupplierItem(Ammo.SNIPER, 1, Properties()) }
    @JvmField val SHOTGUN_AMMO = registerAmmo("shotgun_ammo") { AmmoSupplierItem(Ammo.SHOTGUN, 1, Properties()) }
    @JvmField val HEAVY_AMMO = registerAmmo("heavy_ammo") { AmmoSupplierItem(Ammo.HEAVY, 1, Properties()) }
    @JvmField val HANDGUN_AMMO_BOX = registerAmmo<HandgunAmmoBoxItem>()
    @JvmField val RIFLE_AMMO_BOX = registerAmmo<RifleAmmoBoxItem>()
    @JvmField val SNIPER_AMMO_BOX = registerAmmo<SniperAmmoBoxItem>()
    @JvmField val SHOTGUN_AMMO_BOX = registerAmmo<ShotgunAmmoBoxItem>()
    @JvmField val CREATIVE_AMMO_BOX = registerAmmo<CreativeAmmoBoxItem>()
    @JvmField val AMMO_BOX = registerAmmo<AmmoBoxItem>()
    @JvmField val TASER_ELECTRODE = registerAmmo("taser_electrode")
    @JvmField val GRENADE_40MM = registerAmmo("grenade_40mm")
    @JvmField val FLYING_FLARE_AMMO = registerAmmo("flying_flare_ammo")
    @JvmField val VEHICLE_SMOKE_AMMO = registerAmmo("vehicle_smoke_ammo")

    @JvmField val MORTAR_SHELL = registerAmmo<MortarShellItem>()
    @JvmField val MORTAR_SHELL_WP = registerAmmo<MortarShellItem>("mortar_shell_wp")
    @JvmField val MORTAR_SHELL_SMOKE = registerAmmo<SmokeMortarShellItem>()
    @JvmField val POTION_MORTAR_SHELL = registerAmmo<PotionMortarShellItem>()
    @JvmField val RPG_ROCKET_STANDARD = registerAmmo<RpgRocketStandardItem>()
    @JvmField val RPG_ROCKET_TBG = registerAmmo<RpgRocketTBGItem>()
    @JvmField val JAVELIN_MISSILE = registerAmmo("javelin_missile") { Item(Properties().stacksTo(4)) }
    @JvmField val LUNGE_MINE = registerAmmo<LungeMineItem>()
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
    @JvmField val HAND_GRENADE = registerAmmo<HandGrenadeItem>()
    @JvmField val RGO_GRENADE = registerAmmo<RgoGrenadeItem>()
    @JvmField val M18_SMOKE_GRENADE = registerAmmo<M18SmokeGrenadeItem>()
    @JvmField val CLAYMORE_MINE = registerAmmo<ClaymoreMineItem>()
    @JvmField val TM_62 = registerAmmo<Tm62Item>()
    @JvmField val PTKM_1R = registerAmmo<Ptkm1rItem>()
    @JvmField val C4_BOMB = registerAmmo<C4BombItem>()
    @JvmField val BLU_43_MINE = registerAmmo<Blu43MineItem>()
    @JvmField val EDD = registerAmmo<EDDItem>()
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
        id: String? = null,
        register: ItemRegister = ITEMS,
        noinline itemSupplier: () -> T = { getInstanceOrCreate(T::class) },
    ): ItemRegistry<T> {
        return register.register(id ?: registryNameOf<T>(), itemSupplier)
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
    @JvmField val DRONE = registerItem<DroneItem>()

    @JvmField val MONITOR = registerItem<MonitorItem>()
    @JvmField val ARTILLERY_INDICATOR = registerItem<ArtilleryIndicatorItem>()

    @JvmField val DETONATOR = registerItem<DetonatorItem>()
    @JvmField val TARGET_DEPLOYER = registerItem<TargetDeployerItem>()
    @JvmField val DPS_GENERATOR_DEPLOYER = registerItem<DPSGeneratorDeployerItem>()
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
    @JvmField val NETHERITE_HAMMER = registerItem<NetheriteHammerItem>()
    @JvmField val CEMENTED_CARBIDE_SWORD = registerItem("cemented_carbide_sword") { SwordItem(ModItemTier.CEMENTED_CARBIDE, Properties().attributes(createAttributes(ModItemTier.CEMENTED_CARBIDE, -2, -2.4f))) }
    @JvmField val CEMENTED_CARBIDE_PICKAXE = registerItem("cemented_carbide_pickaxe") { PickaxeItem(ModItemTier.CEMENTED_CARBIDE, Properties().attributes(createAttributes(ModItemTier.CEMENTED_CARBIDE, -4, -2.8f))) }
    @JvmField val CEMENTED_CARBIDE_AXE = registerItem("cemented_carbide_axe") { AxeItem(ModItemTier.CEMENTED_CARBIDE, Properties().attributes(createAttributes(ModItemTier.CEMENTED_CARBIDE,  0f, -3.0f))) }
    @JvmField val CEMENTED_CARBIDE_SHOVEL = registerItem("cemented_carbide_shovel") { ShovelItem(ModItemTier.CEMENTED_CARBIDE, Properties().attributes(createAttributes(ModItemTier.CEMENTED_CARBIDE, -3.5f, -3.0f))) }
    @JvmField val CEMENTED_CARBIDE_HOE = registerItem("cemented_carbide_hoe") { HoeItem(ModItemTier.CEMENTED_CARBIDE, Properties().attributes(createAttributes(ModItemTier.CEMENTED_CARBIDE, -8, 0.0f))) }

    @JvmField val T_BATON = registerItem<TBatonItem>()
    @JvmField val ELECTRIC_BATON = registerItem<ElectricBatonItem>()
    @JvmField val STEEL_PIPE = registerItem<SteelPipeItem>()
    @JvmField val CROWBAR = registerItem<CrowbarItem>()
    @JvmField val MILITARY_SHOVEL = registerItem<MilitaryShovelItem>()
    @JvmField val DEFUSER = registerItem<DefuserItem>()
    @JvmField val ARMOR_PLATE = registerItem<ArmorPlateItem>()

    @JvmField val RU_HELMET_6B47 = registerItem<RuHelmet6b47Item>()
    @JvmField val RU_CHEST_6B43 = registerItem<RuChest6b43Item>()
    @JvmField val US_HELMET_PASGT = registerItem<UsHelmetPasgtItem>()
    @JvmField val US_CHEST_IOTV = registerItem<UsChestIotvItem>()
    @JvmField val GE_HELMET_M_35 = registerItem<GeHelmetM35Item>()
    @JvmField val PARACHUTE = registerItem<ParachuteItem>()
    @JvmField val THERMAL_IMAGING_GOGGLES = registerItem<ThermalImagingGogglesItem>()
    @JvmField val HANDSOME_GOGGLES = registerItem<HandsomeGogglesItem>()
    @JvmField val TACTICAL_TERMINAL = registerItem<TacticalTerminalItem>()

    @JvmField val SONIC_ABSORBER = registerItem<SonicAbsorberItem>()

    @JvmField val CRUST = registerItem<CrustItem>()

    @JvmField val MORTAR_DEPLOYER = registerItem<MortarDeployerItem>()
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
    @JvmField val DOG_TAG = registerItem<DogTagItem>()
    @JvmField val IFF = registerItem<IffItem>()
    @JvmField val CELL = registerItem("cell") { BatteryItem(24000, Properties()) }
    @JvmField val BATTERY = registerItem("battery") { BatteryItem(100000, Properties()) }
    @JvmField val SMALL_BATTERY_PACK = registerItem("small_battery_pack") { BatteryItem(500000, Properties()) }
    @JvmField val MEDIUM_BATTERY_PACK = registerItem("medium_battery_pack") { BatteryItem(5000000, Properties()) }
    @JvmField val LARGE_BATTERY_PACK = registerItem("large_battery_pack") { BatteryItem(20000000, Properties()) }
    @JvmField val LASER_UNIT = registerItem("laser_unit")
    @JvmField val BEAST = registerItem<BeastItem>()
    @JvmField val TRANSCRIPT = registerItem<TranscriptItem>()
    @JvmField val FIRING_PARAMETERS = registerItem<FiringParametersItem>()
    @JvmField val MEDICAL_KIT = registerItem<MedicalKitItem>()
    @JvmField val VEHICLE_DAMAGE_ANALYZER = registerItem<VehicleDamageAnalyzerItem>()
    @JvmField val VEHICLE_RESET_KIT = registerItem<VehicleResetKitItem>()
    @JvmField val SKIN_SPRAY = registerItem<SkinSprayItem>()
    @JvmField val VEHICLE_KEY = registerItem<VehicleKeyItem>()
    @JvmField val CREATIVE_VEHICLE_KEY = registerItem<CreativeVehicleKeyItem>()
    @JvmField val TOWLINE = registerItem<TowlineItem>()
    @JvmField val TOW_BAR = registerItem<TowBarItem>()
    @JvmField val CATAPULT_SHUTTLE = registerItem<CatapultShuttleItem>()

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

    @JvmField val COMMON_ACCESSORY_KIT = registerItem("common_accessory_kit") { MaterialPackItem(Rarity.COMMON) }
    @JvmField val RARE_ACCESSORY_KIT = registerItem("rare_accessory_kit") { MaterialPackItem(Rarity.RARE) }
    @JvmField val EPIC_ACCESSORY_KIT = registerItem("epic_accessory_kit") { MaterialPackItem(Rarity.EPIC) }
    @JvmField val LEGENDARY_ACCESSORY_KIT = registerItem("legendary_accessory_kit") { MaterialPackItem(LEGENDARY) }
    @JvmField val SUPERB_ACCESSORY_KIT = registerItem("superb_accessory_kit") { MaterialPackItem(SUPERB) }
    @JvmField val VIRTUAL_ACCESSORY_KIT = registerItem("virtual_accessory_kit") { MaterialPackItem(VIRTUAL) }

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
    @JvmField val HUNTING_RIFLE_BLUEPRINT = registerBlueprint("hunting_rifle_blueprint", Rarity.EPIC)
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
        id: String? = null,
        noinline block: () -> T = { getInstanceOrCreate(T::class) }
    ): ItemRegistry<T> = BLOCKS.register(id ?: registryNameOf<T>(), block)

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
    @JvmField val CHARGING_STATION = registerBlock<ChargingStationBlockItem>()
    @JvmField val CREATIVE_CHARGING_STATION = registerBlock<CreativeChargingStationBlockItem>()
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
    @JvmField val VEHICLE_ASSEMBLING_TABLE = registerBlock<VehicleAssemblingTableBlockItem>()
    @JvmField val BLUEPRINT_RESEARCH_TABLE = registerBlock<BlueprintResearchTableBlockItem>()
    @JvmField val BIOGAS_GENERATOR = registerBlock(ModBlocks.BIOGAS_GENERATOR)
    // @formatter:on

    /**
     * Vehicle
     */
    private fun registerVehicle(id: String) = registerVehicle(id) { Item(Properties()) }

    @JvmName("registerVehicleByClass")
    private inline fun <reified T : Item> registerVehicle(
        id: String? = null,
        noinline item: () -> T = { getInstanceOrCreate(T::class) }
    ): ItemRegistry<T> = VEHICLES.register(id ?: registryNameOf<T>(), item)

    // @formatter:off
    @JvmField val VEHICLES: ItemRegister = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID)

    @JvmField val CONTAINER = registerVehicle<ContainerBlockItem>()
    @JvmField val SMALL_CONTAINER = registerVehicle<SmallContainerBlockItem>()
    @JvmField val LUCKY_CONTAINER = registerVehicle<LuckyContainerBlockItem>()
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

    // Stock
    // @formatter:off
    @JvmField val OEM_STOCK_LIGHT = registerAttachment("oem_stock_light")
    @JvmField val OEM_STOCK_HEAVY = registerAttachment("oem_stock_heavy")
    @JvmField val STOCK_UBR = registerAttachment("stock_ubr")
    @JvmField val STOCK_PRS = registerAttachment("stock_prs")
    @JvmField val STOCK_AR_TACTICAL = registerAttachment("stock_ar_tactical")
    @JvmField val STOCK_ZENIT_PT_1 = registerAttachment("stock_zenit_pt_1")
    // @formatter:on

    // Magazine
    // @formatter:off
    @JvmField val MAGAZINE_EXTEND = registerAttachment("magazine_extend", Rarity.RARE)
    @JvmField val MAGAZINE_EXTEND_PRO = registerAttachment("magazine_extend_pro", Rarity.EPIC)
    // @formatter:on

    // Barrel
    // @formatter:off
    @JvmField val MEOWLENCER = registerAttachment("meowlencer", Rarity.RARE)
    @JvmField val HISSILENCER = registerAttachment("hissilencer", Rarity.RARE)
    @JvmField val SILAOWUNCER = registerAttachment("silaowuncer", Rarity.RARE)
    @JvmField val RU_SILENCER = registerAttachment("ru_silencer")
    @JvmField val SILENCER_50_CAL = registerAttachment("silencer_50_cal")
    @JvmField val AR_SILENCER = registerAttachment("ar_silencer")
    @JvmField val HANDGUN_SILENCER = registerAttachment("handgun_silencer")
    @JvmField val OSPREY_SILENCER = registerAttachment("osprey_silencer", Rarity.RARE)
    @JvmField val LOUDSPEAKER = registerAttachment("loudspeaker", ModRarities.LEGENDARY)
    @JvmField val OSPREY_SILENCER_TRACHELIUM = registerAttachment("osprey_silencer_trachelium", ModRarities.VIRTUAL)
    @JvmField val SILENCER_QINGLAN = registerAttachment("silencer_qinglan", ModRarities.VIRTUAL)
    // @formatter:on

    // Scope / sight
    // @formatter:off
    @JvmField val SCOPE_COYOTE = registerAttachment("scope_coyote")
    @JvmField val SCOPE_HANDGUN_RED_DOT = registerAttachment("scope_handgun_red_dot")
    @JvmField val SCOPE_EOTECH_551 = registerAttachment("scope_eotech_551")
    @JvmField val SCOPE_OKP_7 = registerAttachment("scope_okp_7")
    @JvmField val SCOPE_PKAS = registerAttachment("scope_pkas")
    @JvmField val SCOPE_ACOG = registerAttachment("scope_acog", Rarity.RARE)
    @JvmField val SCOPE_1P_78 = registerAttachment("scope_1p_78", Rarity.RARE)
    @JvmField val SCOPE_HAMR = registerAttachment("scope_hamr", Rarity.EPIC)
    @JvmField val SCOPE_LPVO = registerAttachment("scope_lpvo", Rarity.EPIC)
    @JvmField val SCOPE_SNIPER = registerAttachment("scope_sniper", Rarity.EPIC)
    // @formatter:on

    // Grip
    // @formatter:off
    @JvmField val VERTICAL_GRIP = registerAttachment("vertical_grip")
    @JvmField val TRIANGLE_GRIP = registerAttachment("triangle_grip")
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
