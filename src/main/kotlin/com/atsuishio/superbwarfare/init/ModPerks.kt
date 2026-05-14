package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.perk.AmmoPerk
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.ammo.*
import com.atsuishio.superbwarfare.perk.damage.*
import com.atsuishio.superbwarfare.perk.functional.*
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.world.effect.MobEffects
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NewRegistryEvent
import net.neoforged.neoforge.registries.RegistryBuilder

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
    private fun registerAmmoPerk(id: String, perk: () -> Perk): DeferredHolder<Perk, Perk> =
        AMMO_PERKS.register(id, perk)

    // @formatter:off
    @JvmField val AP_BULLET = registerAmmoPerk("ap_bullet") { APBullet }
    @JvmField val JHP_BULLET = registerAmmoPerk("jhp_bullet") { JHPBullet }
    @JvmField val HE_BULLET = registerAmmoPerk("he_bullet") { HEBullet }
    @JvmField val SILVER_BULLET = registerAmmoPerk("silver_bullet") { SilverBullet }
    @JvmField val POISONOUS_BULLET = registerAmmoPerk("poisonous_bullet") {
        AmmoPerk(
            AmmoPerk.Builder("poisonous_bullet", Perk.Type.AMMO).bypassArmorRate(0.0).damageRate(1.0)
                .speedRate(1.0).rgb(48, 131, 6)
                .mobEffect(MobEffects.POISON)
        )
    }
    @JvmField val BEAST_BULLET = registerAmmoPerk("beast_bullet") { BeastBullet }
    @JvmField val LONGER_WIRE = registerAmmoPerk("longer_wire") { LongerWire }
    @JvmField val INCENDIARY_BULLET = registerAmmoPerk("incendiary_bullet") { IncendiaryBullet }
    @JvmField val MICRO_MISSILE = registerAmmoPerk("micro_missile") { MicroMissile }
    @JvmField val CUPID_ARROW = registerAmmoPerk("cupid_arrow") { CupidArrow }
    @JvmField val RIOT_BULLET = registerAmmoPerk("riot_bullet") { RiotBullet }
    @JvmField val PHASE_PENETRATING_BULLET = registerAmmoPerk("phase_penetrating_bullet") { PhasePenetratingBullet }
    @JvmField val BLADE_BULLET = registerAmmoPerk("blade_bullet") { BladeBullet }
    @JvmField val PHOSPHORUS_FLAME_BULLET = registerAmmoPerk("phosphorus_flame_bullet") { PhosphorusFlameBullet }
    // @formatter:on

    /**
     * Functional Perks
     */
    @JvmField
    val FUNC_PERKS: DeferredRegister<Perk> = DeferredRegister.create(LOCATION, Mod.MODID)
    private fun registerFuncPerk(id: String, perk: () -> Perk): DeferredHolder<Perk, Perk> =
        FUNC_PERKS.register(id, perk)

    // @formatter:off
    @JvmField val HEAL_CLIP = registerFuncPerk("heal_clip") { HealClip }
    @JvmField val FOURTH_TIMES_CHARM = registerFuncPerk("fourth_times_charm") { FourthTimesCharm }
    @JvmField val SUBSISTENCE = registerFuncPerk("subsistence") { Subsistence }
    @JvmField val FIELD_DOCTOR = registerFuncPerk("field_doctor") { FieldDoctor }
    @JvmField val REGENERATION = registerFuncPerk("regeneration") { Regeneration }
    @JvmField val TURBO_CHARGER = registerFuncPerk("turbo_charger") { TurboCharger }
    @JvmField val POWERFUL_ATTRACTION = registerFuncPerk("powerful_attraction") { PowerfulAttraction }
    @JvmField val INTELLIGENT_CHIP = registerFuncPerk("intelligent_chip") { Perk("intelligent_chip", Perk.Type.FUNCTIONAL) }
    @JvmField val BACKPACK_LINKED_MAGAZINE = registerFuncPerk("backpack_linked_magazine") { BackpackLinkedMagazine }
    @JvmField val POWERFUL_COOLER = registerFuncPerk("powerful_cooler") { PowerfulCooler }
    @JvmField val CAST_NO_SHADOWS = registerFuncPerk("cast_no_shadows") { CastNoShadows }
    // @formatter:on

    /**
     * Damage Perks
     */
    @JvmField
    val DAMAGE_PERKS: DeferredRegister<Perk> = DeferredRegister.create(LOCATION, Mod.MODID)
    private fun registerDamagePerk(id: String, perk: () -> Perk): DeferredHolder<Perk, Perk> =
        DAMAGE_PERKS.register(id, perk)

    // @formatter:off
    @JvmField val KILL_CLIP = registerDamagePerk("kill_clip") { KillClip }
    @JvmField val GUTSHOT_STRAIGHT = registerDamagePerk("gutshot_straight") { GutshotStraight }
    @JvmField val KILLING_TALLY = registerDamagePerk("killing_tally") { KillingTally }
    @JvmField val HEAD_SEEKER = registerDamagePerk("head_seeker") { HeadSeeker }
    @JvmField val MONSTER_HUNTER = registerDamagePerk("monster_hunter") { MonsterHunter }
    @JvmField val VOLT_OVERLOAD = registerDamagePerk("volt_overload") { VoltOverload }
    @JvmField val DESPERADO = registerDamagePerk("desperado") { Desperado }
    @JvmField val VORPAL_WEAPON = registerDamagePerk("vorpal_weapon") { VorpalWeapon }
    @JvmField val MAGNIFICENT_HOWL = registerDamagePerk("magnificent_howl") { MagnificentHowl }
    @JvmField val FIREFLY = registerDamagePerk("firefly") { Firefly }
    @JvmField val FAIR_MEANS = registerDamagePerk("fair_means") { FairMeans }
    @JvmField val HIGH_IMPACT_RESERVES = registerDamagePerk("high_impact_reserves") { HighImpactReserves }
    @JvmField val ONE_TWO_PUNCH = registerDamagePerk("one_two_punch") { OneTwoPunch }
    @JvmField val BRAIN_STORM = registerDamagePerk("brain_storm") { BrainStorm }
    @JvmField val BATTLE_OF_WITS = registerDamagePerk("battle_of_wits") { BattleOfWits }
    // @formatter:on

    fun register(bus: IEventBus) {
        AMMO_PERKS.register(bus)
        FUNC_PERKS.register(bus)
        DAMAGE_PERKS.register(bus)
    }
}
