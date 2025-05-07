package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.ammo.*;
import com.atsuishio.superbwarfare.perk.damage.*;
import com.atsuishio.superbwarfare.perk.functional.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
@SuppressWarnings("unused")
public class ModPerks {

    @SubscribeEvent
    public static void registry(NewRegistryEvent event) {
        event.create(new RegistryBuilder<Perk>(ResourceKey.createRegistryKey(Mod.loc("perk"))));
    }

    /**
     * Ammo Perks
     */
    public static final DeferredRegister<Perk> AMMO_PERKS = DeferredRegister.create(Mod.loc("perk"), Mod.MODID);

    public static final DeferredHolder<Perk, Perk> AP_BULLET = AMMO_PERKS.register("ap_bullet", APBullet::new);
    public static final DeferredHolder<Perk, Perk> JHP_BULLET = AMMO_PERKS.register("jhp_bullet", JHPBullet::new);
    public static final DeferredHolder<Perk, Perk> HE_BULLET = AMMO_PERKS.register("he_bullet", HEBullet::new);
    public static final DeferredHolder<Perk, Perk> SILVER_BULLET = AMMO_PERKS.register("silver_bullet", SilverBullet::new);
    public static final DeferredHolder<Perk, Perk> POISONOUS_BULLET = AMMO_PERKS.register("poisonous_bullet",
            () -> new AmmoPerk(new AmmoPerk.Builder("poisonous_bullet", Perk.Type.AMMO).bypassArmorRate(0.0f).damageRate(1.0f).speedRate(1.0f).rgb(48, 131, 6)
                    .mobEffect(MobEffects.POISON::value)));
    public static final DeferredHolder<Perk, Perk> BEAST_BULLET = AMMO_PERKS.register("beast_bullet", BeastBullet::new);
    public static final DeferredHolder<Perk, Perk> LONGER_WIRE = AMMO_PERKS.register("longer_wire", () -> new Perk("longer_wire", Perk.Type.AMMO));
    public static final DeferredHolder<Perk, Perk> INCENDIARY_BULLET = AMMO_PERKS.register("incendiary_bullet", IncendiaryBullet::new);
    public static final DeferredHolder<Perk, Perk> MICRO_MISSILE = AMMO_PERKS.register("micro_missile",
            () -> new AmmoPerk(new AmmoPerk.Builder("micro_missile", Perk.Type.AMMO).speedRate(1.2f)));

    /**
     * Functional Perks
     */
    public static final DeferredRegister<Perk> FUNC_PERKS = DeferredRegister.create(Mod.loc("perk"), Mod.MODID);

    public static final DeferredHolder<Perk, Perk> HEAL_CLIP = FUNC_PERKS.register("heal_clip", HealClip::new);
    public static final DeferredHolder<Perk, Perk> FOURTH_TIMES_CHARM = FUNC_PERKS.register("fourth_times_charm", FourthTimesCharm::new);
    public static final DeferredHolder<Perk, Perk> SUBSISTENCE = FUNC_PERKS.register("subsistence", Subsistence::new);
    public static final DeferredHolder<Perk, Perk> FIELD_DOCTOR = FUNC_PERKS.register("field_doctor", FieldDoctor::new);
    public static final DeferredHolder<Perk, Perk> REGENERATION = FUNC_PERKS.register("regeneration", Regeneration::new);
    public static final DeferredHolder<Perk, Perk> TURBO_CHARGER = FUNC_PERKS.register("turbo_charger", () -> new Perk("turbo_charger", Perk.Type.FUNCTIONAL));
    public static final DeferredHolder<Perk, Perk> POWERFUL_ATTRACTION = FUNC_PERKS.register("powerful_attraction", PowerfulAttraction::new);
    public static final DeferredHolder<Perk, Perk> INTELLIGENT_CHIP = FUNC_PERKS.register("intelligent_chip", () -> new Perk("intelligent_chip", Perk.Type.FUNCTIONAL));

    /**
     * Damage Perks
     */
    public static final DeferredRegister<Perk> DAMAGE_PERKS = DeferredRegister.create(Mod.loc("perk"), Mod.MODID);

    public static final DeferredHolder<Perk, Perk> KILL_CLIP = DAMAGE_PERKS.register("kill_clip", KillClip::new);
    public static final DeferredHolder<Perk, Perk> GUTSHOT_STRAIGHT = DAMAGE_PERKS.register("gutshot_straight", GutshotStraight::new);
    public static final DeferredHolder<Perk, Perk> KILLING_TALLY = DAMAGE_PERKS.register("killing_tally", KillingTally::new);
    public static final DeferredHolder<Perk, Perk> HEAD_SEEKER = DAMAGE_PERKS.register("head_seeker", HeadSeeker::new);
    public static final DeferredHolder<Perk, Perk> MONSTER_HUNTER = DAMAGE_PERKS.register("monster_hunter", MonsterHunter::new);
    public static final DeferredHolder<Perk, Perk> VOLT_OVERLOAD = DAMAGE_PERKS.register("volt_overload", () -> new Perk("volt_overload", Perk.Type.DAMAGE));
    public static final DeferredHolder<Perk, Perk> DESPERADO = DAMAGE_PERKS.register("desperado", Desperado::new);
    public static final DeferredHolder<Perk, Perk> VORPAL_WEAPON = DAMAGE_PERKS.register("vorpal_weapon", VorpalWeapon::new);

//    public static void registerCompatPerks() {
//        if (ModList.get().isLoaded(CompatHolder.DMV)) {
//            AMMO_PERKS.register("blade_bullet", BladeBullet::new);
//            AMMO_PERKS.register("bread_bullet", BreadBullet::new);
//        }
//        if (ModList.get().isLoaded(CompatHolder.VRC)) {
//            AMMO_PERKS.register("curse_flame_bullet", () -> new AmmoPerk(new AmmoPerk.Builder("curse_flame_bullet", Perk.Type.AMMO)
//                    .bypassArmorRate(0.0f).damageRate(1.2f).speedRate(0.9f).rgb(0xB1, 0xC1, 0xF2).mobEffect(() -> CompatHolder.VRC_CURSE_FLAME)));
//            AMMO_PERKS.register("butterfly_bullet", () -> new AmmoPerk(new AmmoPerk.Builder("butterfly_bullet", Perk.Type.AMMO)
//                    .bypassArmorRate(0.0f)));
//        }
//    }

    public static void register(IEventBus bus) {
//        registerCompatPerks();
        AMMO_PERKS.register(bus);
        FUNC_PERKS.register(bus);
        DAMAGE_PERKS.register(bus);
    }
}
