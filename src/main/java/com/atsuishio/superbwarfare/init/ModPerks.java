package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.compat.CompatHolder;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.ammo.*;
import com.atsuishio.superbwarfare.perk.damage.*;
import com.atsuishio.superbwarfare.perk.functional.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("unused")
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public class ModPerks {

    public static final ResourceKey<Registry<Perk>> PERK_KEY = ResourceKey.createRegistryKey(Mod.loc("perk"));

    @SubscribeEvent
    public static void registry(NewRegistryEvent event) {
        event.create(new RegistryBuilder<Perk>().setName(Mod.loc("perk")));
    }

    /**
     * Ammo Perks
     */
    public static final DeferredRegister<Perk> AMMO_PERKS = DeferredRegister.create(Mod.loc("perk"), Mod.MODID);

    public static final RegistryObject<Perk> AP_BULLET = AMMO_PERKS.register("ap_bullet", APBullet::new);
    public static final RegistryObject<Perk> JHP_BULLET = AMMO_PERKS.register("jhp_bullet", JHPBullet::new);
    public static final RegistryObject<Perk> HE_BULLET = AMMO_PERKS.register("he_bullet", HEBullet::new);
    public static final RegistryObject<Perk> SILVER_BULLET = AMMO_PERKS.register("silver_bullet", SilverBullet::new);
    public static final RegistryObject<Perk> POISONOUS_BULLET = AMMO_PERKS.register("poisonous_bullet",
            () -> new AmmoPerk(new AmmoPerk.Builder("poisonous_bullet", Perk.Type.AMMO).bypassArmorRate(0.0f).damageRate(1.0f).speedRate(1.0f).rgb(48, 131, 6)
                    .mobEffect(() -> MobEffects.POISON)));
    public static final RegistryObject<Perk> BEAST_BULLET = AMMO_PERKS.register("beast_bullet", BeastBullet::new);
    public static final RegistryObject<Perk> LONGER_WIRE = AMMO_PERKS.register("longer_wire", LongerWire::new);
    public static final RegistryObject<Perk> INCENDIARY_BULLET = AMMO_PERKS.register("incendiary_bullet", IncendiaryBullet::new);
    public static final RegistryObject<Perk> MICRO_MISSILE = AMMO_PERKS.register("micro_missile", MicroMissile::new);
    public static final RegistryObject<Perk> CUPID_ARROW = AMMO_PERKS.register("cupid_arrow", CupidArrow::new);
    public static final RegistryObject<Perk> RIOT_BULLET = AMMO_PERKS.register("riot_bullet", RiotBullet::new);
    public static final RegistryObject<Perk> PHASE_PENETRATING_BULLET = AMMO_PERKS.register("phase_penetrating_bullet", PhasePenetratingBullet::new);

    /**
     * Functional Perks
     */
    public static final DeferredRegister<Perk> FUNC_PERKS = DeferredRegister.create(Mod.loc("perk"), Mod.MODID);

    public static final RegistryObject<Perk> HEAL_CLIP = FUNC_PERKS.register("heal_clip", HealClip::new);
    public static final RegistryObject<Perk> FOURTH_TIMES_CHARM = FUNC_PERKS.register("fourth_times_charm", FourthTimesCharm::new);
    public static final RegistryObject<Perk> SUBSISTENCE = FUNC_PERKS.register("subsistence", Subsistence::new);
    public static final RegistryObject<Perk> FIELD_DOCTOR = FUNC_PERKS.register("field_doctor", FieldDoctor::new);
    public static final RegistryObject<Perk> REGENERATION = FUNC_PERKS.register("regeneration", Regeneration::new);
    public static final RegistryObject<Perk> TURBO_CHARGER = FUNC_PERKS.register("turbo_charger", TurboCharger::new);
    public static final RegistryObject<Perk> POWERFUL_ATTRACTION = FUNC_PERKS.register("powerful_attraction", PowerfulAttraction::new);
    public static final RegistryObject<Perk> INTELLIGENT_CHIP = FUNC_PERKS.register("intelligent_chip", () -> new Perk("intelligent_chip", Perk.Type.FUNCTIONAL));

    /**
     * Damage Perks
     */
    public static final DeferredRegister<Perk> DAMAGE_PERKS = DeferredRegister.create(Mod.loc("perk"), Mod.MODID);

    public static final RegistryObject<Perk> KILL_CLIP = DAMAGE_PERKS.register("kill_clip", KillClip::new);
    public static final RegistryObject<Perk> GUTSHOT_STRAIGHT = DAMAGE_PERKS.register("gutshot_straight", GutshotStraight::new);
    public static final RegistryObject<Perk> KILLING_TALLY = DAMAGE_PERKS.register("killing_tally", KillingTally::new);
    public static final RegistryObject<Perk> HEAD_SEEKER = DAMAGE_PERKS.register("head_seeker", HeadSeeker::new);
    public static final RegistryObject<Perk> MONSTER_HUNTER = DAMAGE_PERKS.register("monster_hunter", MonsterHunter::new);
    public static final RegistryObject<Perk> VOLT_OVERLOAD = DAMAGE_PERKS.register("volt_overload", VoltOverload::new);
    public static final RegistryObject<Perk> DESPERADO = DAMAGE_PERKS.register("desperado", Desperado::new);
    public static final RegistryObject<Perk> VORPAL_WEAPON = DAMAGE_PERKS.register("vorpal_weapon", VorpalWeapon::new);
    public static final RegistryObject<Perk> MAGNIFICENT_HOWL = DAMAGE_PERKS.register("magnificent_howl", MagnificentHowl::new);
    public static final RegistryObject<Perk> FIREFLY = DAMAGE_PERKS.register("firefly", Firefly::new);
    public static final RegistryObject<Perk> FAIR_MEANS = DAMAGE_PERKS.register("fair_means", FairMeans::new);

    public static void registerCompatPerks() {
        if (ModList.get().isLoaded(CompatHolder.DMV)) {
            AMMO_PERKS.register("blade_bullet", BladeBullet::new);
            AMMO_PERKS.register("bread_bullet", BreadBullet::new);
        }
        if (ModList.get().isLoaded(CompatHolder.VRC)) {
            AMMO_PERKS.register("curse_flame_bullet", () -> new AmmoPerk(new AmmoPerk.Builder("curse_flame_bullet", Perk.Type.AMMO)
                    .bypassArmorRate(0.0f).damageRate(1.2f).speedRate(0.9f).rgb(0xB1, 0xC1, 0xF2).mobEffect(() -> CompatHolder.VRC_CURSE_FLAME)));
            AMMO_PERKS.register("butterfly_bullet", () -> new AmmoPerk(new AmmoPerk.Builder("butterfly_bullet", Perk.Type.AMMO)
                    .bypassArmorRate(0.0f)));
        }
    }

    public static void register(IEventBus bus) {
        registerCompatPerks();
        AMMO_PERKS.register(bus);
        FUNC_PERKS.register(bus);
        DAMAGE_PERKS.register(bus);
    }
}
