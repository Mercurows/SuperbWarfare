package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.perk.AmmoPerk
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.ammo.*
import com.atsuishio.superbwarfare.perk.damage.*
import com.atsuishio.superbwarfare.perk.functional.*
import com.atsuishio.superbwarfare.perk.js.JsPerk
import com.atsuishio.superbwarfare.perk.js.PerkDescriptor
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.world.effect.MobEffects
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModList
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NewRegistryEvent
import net.neoforged.neoforge.registries.RegistryBuilder
import java.nio.file.Files

@EventBusSubscriber
@Suppress("unused")
object ModPerks {
    @JvmField
    val LOCATION = loc("perk")

    @JvmField
    val PERK_KEY: ResourceKey<Registry<Perk>> = ResourceKey.createRegistryKey(LOCATION)

    @JvmField
    val PERK_REGISTRY: Registry<Perk> = RegistryBuilder<Perk>(ResourceKey.createRegistryKey(LOCATION))
        .sync(true).defaultKey(loc("ap_bullet")).create()

    @SubscribeEvent
    fun registry(event: NewRegistryEvent) {
        event.register(PERK_REGISTRY)
    }

    /**
     * Ammo Perks
     */
    @JvmField
    val AMMO_PERKS: DeferredRegister<Perk> = DeferredRegister.create(LOCATION, Mod.MODID)
    private val registeredIds = mutableSetOf<String>()
    private val autoRegistryObjects = mutableMapOf<String, DeferredHolder<Perk, Perk>>()
    private fun registerAmmoPerk(id: String, perk: () -> Perk): DeferredHolder<Perk, Perk> {
        registeredIds.add(id)
        return AMMO_PERKS.register(id, perk)
    }

    // @formatter:off
    lateinit var AP_BULLET: DeferredHolder<Perk, Perk>
    lateinit var JHP_BULLET: DeferredHolder<Perk, Perk>
    lateinit var HE_BULLET: DeferredHolder<Perk, Perk>
    lateinit var SILVER_BULLET: DeferredHolder<Perk, Perk>
    lateinit var POISONOUS_BULLET: DeferredHolder<Perk, Perk>
    lateinit var BEAST_BULLET: DeferredHolder<Perk, Perk>
    lateinit var LONGER_WIRE: DeferredHolder<Perk, Perk>
    lateinit var INCENDIARY_BULLET: DeferredHolder<Perk, Perk>
    lateinit var MICRO_MISSILE: DeferredHolder<Perk, Perk>
    lateinit var CUPID_ARROW: DeferredHolder<Perk, Perk>
    lateinit var RIOT_BULLET: DeferredHolder<Perk, Perk>
    lateinit var PHASE_PENETRATING_BULLET: DeferredHolder<Perk, Perk>
    lateinit var BLADE_BULLET: DeferredHolder<Perk, Perk>
    lateinit var PHOSPHORUS_FLAME_BULLET: DeferredHolder<Perk, Perk>
    // @formatter:on

    /**
     * Functional Perks
     */
    @JvmField
    val FUNC_PERKS: DeferredRegister<Perk> = DeferredRegister.create(LOCATION, Mod.MODID)
    private fun registerFuncPerk(id: String, perk: () -> Perk): DeferredHolder<Perk, Perk> {
        registeredIds.add(id)
        return FUNC_PERKS.register(id, perk)
    }

    // @formatter:off
    lateinit var HEAL_CLIP: DeferredHolder<Perk, Perk>
    lateinit var FOURTH_TIMES_CHARM: DeferredHolder<Perk, Perk>
    lateinit var SUBSISTENCE: DeferredHolder<Perk, Perk>
    lateinit var FIELD_DOCTOR: DeferredHolder<Perk, Perk>
    lateinit var REGENERATION: DeferredHolder<Perk, Perk>
    lateinit var TURBO_CHARGER: DeferredHolder<Perk, Perk>
    lateinit var POWERFUL_ATTRACTION: DeferredHolder<Perk, Perk>
    lateinit var INTELLIGENT_CHIP: DeferredHolder<Perk, Perk>
    lateinit var BACKPACK_LINKED_MAGAZINE: DeferredHolder<Perk, Perk>
    lateinit var POWERFUL_COOLER: DeferredHolder<Perk, Perk>
    lateinit var CAST_NO_SHADOWS: DeferredHolder<Perk, Perk>
    // @formatter:on

    /**
     * Damage Perks
     */
    @JvmField
    val DAMAGE_PERKS: DeferredRegister<Perk> = DeferredRegister.create(LOCATION, Mod.MODID)
    private fun registerDamagePerk(id: String, perk: () -> Perk): DeferredHolder<Perk, Perk> {
        registeredIds.add(id)
        return DAMAGE_PERKS.register(id, perk)
    }

    // @formatter:off
    lateinit var KILL_CLIP: DeferredHolder<Perk, Perk>
    lateinit var GUTSHOT_STRAIGHT: DeferredHolder<Perk, Perk>
    lateinit var KILLING_TALLY: DeferredHolder<Perk, Perk>
    lateinit var HEAD_SEEKER: DeferredHolder<Perk, Perk>
    lateinit var MONSTER_HUNTER: DeferredHolder<Perk, Perk>
    lateinit var VOLT_OVERLOAD: DeferredHolder<Perk, Perk>
    lateinit var DESPERADO: DeferredHolder<Perk, Perk>
    lateinit var VORPAL_WEAPON: DeferredHolder<Perk, Perk>
    lateinit var MAGNIFICENT_HOWL: DeferredHolder<Perk, Perk>
    lateinit var FIREFLY: DeferredHolder<Perk, Perk>
    lateinit var FAIR_MEANS: DeferredHolder<Perk, Perk>
    lateinit var HIGH_IMPACT_RESERVES: DeferredHolder<Perk, Perk>
    lateinit var ONE_TWO_PUNCH: DeferredHolder<Perk, Perk>
    lateinit var BRAIN_STORM: DeferredHolder<Perk, Perk>
    lateinit var BATTLE_OF_WITS: DeferredHolder<Perk, Perk>
    // @formatter:on

    fun register(bus: IEventBus) {
        autoRegisterFromJsons()
        registerHardcoded()
        AMMO_PERKS.register(bus)
        FUNC_PERKS.register(bus)
        DAMAGE_PERKS.register(bus)
    }

    private fun autoRegisterFromJsons() {
        try {
            val modFile = ModList.get().getModFileById(Mod.MODID).file
            val perksDir = modFile.findResource("data/${Mod.MODID}/sbw/perks")
            Files.list(perksDir).use { stream ->
                stream.filter { it.fileName.toString().endsWith(".json") }
                    .forEach { path ->
                        val id = path.fileName.toString().substringBeforeLast(".json")
                        if (id in registeredIds) return@forEach
                        val descriptor = parsePerkJson(path) ?: return@forEach
                        val perk = JsPerk(id, descriptor)
                        val ro: DeferredHolder<Perk, Perk> = when (descriptor.perkType) {
                            Perk.Type.AMMO -> registerAmmoPerk(id) { perk }
                            Perk.Type.FUNCTIONAL -> registerFuncPerk(id) { perk }
                            Perk.Type.DAMAGE -> registerDamagePerk(id) { perk }
                        }
                        autoRegistryObjects[id] = ro
                        Mod.LOGGER.debug("Auto-registered perk '{}' from JSON", id)
                    }
            }
        } catch (e: Exception) {
            Mod.LOGGER.warn("Failed to auto-discover perk JSONs: {}", e.toString())
        }
    }

    private fun parsePerkJson(path: java.nio.file.Path): PerkDescriptor? {
        return try {
            Files.newBufferedReader(path).use { reader ->
                val element = JsonParser.parseReader(reader)
                PerkDescriptor.CODEC.parse(JsonOps.INSTANCE, element)
                    .resultOrPartial { error ->
                        Mod.LOGGER.error(
                            "Failed to parse perk JSON '{}': {}",
                            path.fileName,
                            error
                        )
                    }
                    .orElse(null)
            }
        } catch (e: Exception) {
            Mod.LOGGER.error("Failed to load perk JSON: {}", path, e)
            null
        }
    }

    private fun registerHardcoded() {
        // Ammo Perks
        AP_BULLET = autoRegistryObjects["ap_bullet"] ?: registerAmmoPerk("ap_bullet") { APBullet }
        JHP_BULLET = autoRegistryObjects["jhp_bullet"] ?: registerAmmoPerk("jhp_bullet") { JHPBullet }
        HE_BULLET = autoRegistryObjects["he_bullet"] ?: registerAmmoPerk("he_bullet") { HEBullet }
        SILVER_BULLET = autoRegistryObjects["silver_bullet"] ?: registerAmmoPerk("silver_bullet") { SilverBullet }
        POISONOUS_BULLET = autoRegistryObjects["poisonous_bullet"] ?: registerAmmoPerk("poisonous_bullet") {
            AmmoPerk(
                AmmoPerk.Builder("poisonous_bullet", Perk.Type.AMMO).bypassArmorRate(0.0).damageRate(1.0)
                    .speedRate(1.0).rgb(48, 131, 6)
                    .mobEffect(MobEffects.POISON)
            )
        }
        BEAST_BULLET = autoRegistryObjects["beast_bullet"] ?: registerAmmoPerk("beast_bullet") { BeastBullet }
        LONGER_WIRE = autoRegistryObjects["longer_wire"] ?: registerAmmoPerk("longer_wire") { LongerWire }
        INCENDIARY_BULLET =
            autoRegistryObjects["incendiary_bullet"] ?: registerAmmoPerk("incendiary_bullet") { IncendiaryBullet }
        MICRO_MISSILE = autoRegistryObjects["micro_missile"] ?: registerAmmoPerk("micro_missile") { MicroMissile }
        CUPID_ARROW = autoRegistryObjects["cupid_arrow"] ?: registerAmmoPerk("cupid_arrow") { CupidArrow }
        RIOT_BULLET = autoRegistryObjects["riot_bullet"] ?: registerAmmoPerk("riot_bullet") { RiotBullet }
        PHASE_PENETRATING_BULLET = autoRegistryObjects["phase_penetrating_bullet"]
            ?: registerAmmoPerk("phase_penetrating_bullet") { PhasePenetratingBullet }
        BLADE_BULLET = autoRegistryObjects["blade_bullet"] ?: registerAmmoPerk("blade_bullet") { BladeBullet }
        PHOSPHORUS_FLAME_BULLET = autoRegistryObjects["phosphorus_flame_bullet"]
            ?: registerAmmoPerk("phosphorus_flame_bullet") { PhosphorusFlameBullet }

        // Functional Perks
        HEAL_CLIP = autoRegistryObjects["heal_clip"] ?: registerFuncPerk("heal_clip") { HealClip }
        FOURTH_TIMES_CHARM =
            autoRegistryObjects["fourth_times_charm"] ?: registerFuncPerk("fourth_times_charm") { FourthTimesCharm }
        SUBSISTENCE = autoRegistryObjects["subsistence"] ?: registerFuncPerk("subsistence") { Subsistence }
        FIELD_DOCTOR = autoRegistryObjects["field_doctor"] ?: registerFuncPerk("field_doctor") { FieldDoctor }
        REGENERATION = autoRegistryObjects["regeneration"] ?: registerFuncPerk("regeneration") { Regeneration }
        TURBO_CHARGER = autoRegistryObjects["turbo_charger"] ?: registerFuncPerk("turbo_charger") { TurboCharger }
        POWERFUL_ATTRACTION =
            autoRegistryObjects["powerful_attraction"] ?: registerFuncPerk("powerful_attraction") { PowerfulAttraction }
        INTELLIGENT_CHIP = autoRegistryObjects["intelligent_chip"] ?: registerFuncPerk("intelligent_chip") {
            Perk(
                "intelligent_chip",
                Perk.Type.FUNCTIONAL
            )
        }
        BACKPACK_LINKED_MAGAZINE = autoRegistryObjects["backpack_linked_magazine"]
            ?: registerFuncPerk("backpack_linked_magazine") { BackpackLinkedMagazine }
        POWERFUL_COOLER =
            autoRegistryObjects["powerful_cooler"] ?: registerFuncPerk("powerful_cooler") { PowerfulCooler }
        CAST_NO_SHADOWS =
            autoRegistryObjects["cast_no_shadows"] ?: registerFuncPerk("cast_no_shadows") { CastNoShadows }

        // Damage Perks
        KILL_CLIP = autoRegistryObjects["kill_clip"] ?: registerDamagePerk("kill_clip") { KillClip }
        GUTSHOT_STRAIGHT =
            autoRegistryObjects["gutshot_straight"] ?: registerDamagePerk("gutshot_straight") { GutshotStraight }
        KILLING_TALLY = autoRegistryObjects["killing_tally"] ?: registerDamagePerk("killing_tally") { KillingTally }
        HEAD_SEEKER = autoRegistryObjects["head_seeker"] ?: registerDamagePerk("head_seeker") { HeadSeeker }
        MONSTER_HUNTER = autoRegistryObjects["monster_hunter"] ?: registerDamagePerk("monster_hunter") { MonsterHunter }
        VOLT_OVERLOAD = autoRegistryObjects["volt_overload"] ?: registerDamagePerk("volt_overload") { VoltOverload }
        DESPERADO = autoRegistryObjects["desperado"] ?: registerDamagePerk("desperado") { Desperado }
        VORPAL_WEAPON = autoRegistryObjects["vorpal_weapon"] ?: registerDamagePerk("vorpal_weapon") { VorpalWeapon }
        MAGNIFICENT_HOWL =
            autoRegistryObjects["magnificent_howl"] ?: registerDamagePerk("magnificent_howl") { MagnificentHowl }
        FIREFLY = autoRegistryObjects["firefly"] ?: registerDamagePerk("firefly") { Firefly }
        FAIR_MEANS = autoRegistryObjects["fair_means"] ?: registerDamagePerk("fair_means") { FairMeans }
        HIGH_IMPACT_RESERVES = autoRegistryObjects["high_impact_reserves"]
            ?: registerDamagePerk("high_impact_reserves") { HighImpactReserves }
        ONE_TWO_PUNCH = autoRegistryObjects["one_two_punch"] ?: registerDamagePerk("one_two_punch") { OneTwoPunch }
        BRAIN_STORM = autoRegistryObjects["brain_storm"] ?: registerDamagePerk("brain_storm") { BrainStorm }
        BATTLE_OF_WITS = autoRegistryObjects["battle_of_wits"] ?: registerDamagePerk("battle_of_wits") { BattleOfWits }
    }
}
