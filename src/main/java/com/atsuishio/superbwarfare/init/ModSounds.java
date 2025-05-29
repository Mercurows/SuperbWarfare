package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public class ModSounds {

    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Mod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> TASER_FIRE_1P = REGISTRY.register("taser_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("taser_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TASER_FIRE_3P = REGISTRY.register("taser_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("taser_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TASER_RELOAD_EMPTY = REGISTRY.register("taser_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("taser_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> SHOCK = REGISTRY.register("shock", () -> SoundEvent.createVariableRangeEvent(Mod.loc("shock")));
    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC = REGISTRY.register("electric", () -> SoundEvent.createVariableRangeEvent(Mod.loc("electric")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MELEE_HIT = REGISTRY.register("melee_hit", () -> SoundEvent.createVariableRangeEvent(Mod.loc("melee_hit")));

    public static final DeferredHolder<SoundEvent, SoundEvent> TRACHELIUM_FIRE_1P = REGISTRY.register("trachelium_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("trachelium_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TRACHELIUM_FIRE_3P = REGISTRY.register("trachelium_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("trachelium_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TRACHELIUM_FAR = REGISTRY.register("trachelium_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("trachelium_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TRACHELIUM_VERYFAR = REGISTRY.register("trachelium_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("trachelium_veryfar")));

    public static final DeferredHolder<SoundEvent, SoundEvent> TRACHELIUM_FIRE_1P_S = REGISTRY.register("trachelium_fire_1p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("trachelium_fire_1p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TRACHELIUM_FIRE_3P_S = REGISTRY.register("trachelium_fire_3p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("trachelium_fire_3p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TRACHELIUM_FAR_S = REGISTRY.register("trachelium_far_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("trachelium_far_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TRACHELIUM_RELOAD_EMPTY = REGISTRY.register("trachelium_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("trachelium_reload_empty")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TRACHELIUM_BOLT = REGISTRY.register("trachelium_bolt", () -> SoundEvent.createVariableRangeEvent(Mod.loc("trachelium_bolt")));

    public static final DeferredHolder<SoundEvent, SoundEvent> TRIGGER_CLICK = REGISTRY.register("triggerclick", () -> SoundEvent.createVariableRangeEvent(Mod.loc("triggerclick")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HIT = REGISTRY.register("hit", () -> SoundEvent.createVariableRangeEvent(Mod.loc("hit")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TARGET_DOWN = REGISTRY.register("targetdown", () -> SoundEvent.createVariableRangeEvent(Mod.loc("targetdown")));
    public static final DeferredHolder<SoundEvent, SoundEvent> INDICATION = REGISTRY.register("indication", () -> SoundEvent.createVariableRangeEvent(Mod.loc("indication")));
    public static final DeferredHolder<SoundEvent, SoundEvent> INDICATION_VEHICLE = REGISTRY.register("indication_vehicle", () -> SoundEvent.createVariableRangeEvent(Mod.loc("indication_vehicle")));
    public static final DeferredHolder<SoundEvent, SoundEvent> JUMP = REGISTRY.register("jump", () -> SoundEvent.createVariableRangeEvent(Mod.loc("jump")));
    public static final DeferredHolder<SoundEvent, SoundEvent> DOUBLE_JUMP = REGISTRY.register("doublejump", () -> SoundEvent.createVariableRangeEvent(Mod.loc("doublejump")));

    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_CLOSE = REGISTRY.register("explosion_close", () -> SoundEvent.createVariableRangeEvent(Mod.loc("explosion_close")));
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_FAR = REGISTRY.register("explosion_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("explosion_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_VERY_FAR = REGISTRY.register("explosion_very_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("explosion_very_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HUGE_EXPLOSION_CLOSE = REGISTRY.register("huge_explosion_close", () -> SoundEvent.createVariableRangeEvent(Mod.loc("huge_explosion_close")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HUGE_EXPLOSION_FAR = REGISTRY.register("huge_explosion_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("huge_explosion_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HUGE_EXPLOSION_VERY_FAR = REGISTRY.register("huge_explosion_very_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("huge_explosion_very_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_WATER = REGISTRY.register("explosion_water", () -> SoundEvent.createVariableRangeEvent(Mod.loc("explosion_water")));

    public static final DeferredHolder<SoundEvent, SoundEvent> HUNTING_RIFLE_FIRE_1P = REGISTRY.register("hunting_rifle_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("hunting_rifle_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HUNTING_RIFLE_FIRE_3P = REGISTRY.register("hunting_rifle_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("hunting_rifle_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HUNTING_RIFLE_FAR = REGISTRY.register("hunting_rifle_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("hunting_rifle_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HUNTING_RIFLE_VERYFAR = REGISTRY.register("hunting_rifle_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("hunting_rifle_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HUNTING_RIFLE_RELOAD_EMPTY = REGISTRY.register("hunting_rifle_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("hunting_rifle_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> OUCH = REGISTRY.register("ouch", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ouch")));
    public static final DeferredHolder<SoundEvent, SoundEvent> STEP = REGISTRY.register("step", () -> SoundEvent.createVariableRangeEvent(Mod.loc("step")));
    public static final DeferredHolder<SoundEvent, SoundEvent> GROWL = REGISTRY.register("growl", () -> SoundEvent.createVariableRangeEvent(Mod.loc("growl")));
    public static final DeferredHolder<SoundEvent, SoundEvent> IDLE = REGISTRY.register("idle", () -> SoundEvent.createVariableRangeEvent(Mod.loc("idle")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HENG = REGISTRY.register("heng", () -> SoundEvent.createVariableRangeEvent(Mod.loc("heng")));

    public static final DeferredHolder<SoundEvent, SoundEvent> M_79_FIRE_1P = REGISTRY.register("m_79_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_79_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_79_FIRE_3P = REGISTRY.register("m_79_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_79_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_79_FAR = REGISTRY.register("m_79_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_79_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_79_VERYFAR = REGISTRY.register("m_79_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_79_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_79_RELOAD_EMPTY = REGISTRY.register("m_79_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_79_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> SKS_FIRE_1P = REGISTRY.register("sks_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sks_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SKS_FIRE_3P = REGISTRY.register("sks_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sks_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SKS_RELOAD_NORMAL = REGISTRY.register("sks_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sks_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SKS_RELOAD_EMPTY = REGISTRY.register("sks_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sks_reload_empty")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SKS_FAR = REGISTRY.register("sks_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sks_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SKS_VERYFAR = REGISTRY.register("sks_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sks_veryfar")));

    public static final DeferredHolder<SoundEvent, SoundEvent> HOMEMADE_SHOTGUN_FIRE_1P = REGISTRY.register("homemade_shotgun_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("homemade_shotgun_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HOMEMADE_SHOTGUN_FIRE_3P = REGISTRY.register("homemade_shotgun_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("homemade_shotgun_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HOMEMADE_SHOTGUN_FAR = REGISTRY.register("homemade_shotgun_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("homemade_shotgun_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HOMEMADE_SHOTGUN_VERYFAR = REGISTRY.register("homemade_shotgun_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("homemade_shotgun_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HOMEMADE_SHOTGUN_NORMAL = REGISTRY.register("homemade_shotgun_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("homemade_shotgun_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HOMEMADE_SHOTGUN_RELOAD_EMPTY = REGISTRY.register("homemade_shotgun_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("homemade_shotgun_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> AK_47_FIRE_1P = REGISTRY.register("ak_47_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_47_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AK_47_FIRE_3P = REGISTRY.register("ak_47_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_47_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AK_47_FIRE_1P_S = REGISTRY.register("ak_47_fire_1p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_47_fire_1p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AK_47_FIRE_3P_S = REGISTRY.register("ak_47_fire_3p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_47_fire_3p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AK_47_FAR = REGISTRY.register("ak_47_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_47_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AK_47_VERYFAR = REGISTRY.register("ak_47_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_47_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AK_47_FAR_S = REGISTRY.register("ak_47_far_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_12_far_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AK_47_VERYFAR_S = REGISTRY.register("ak_47_veryfar_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_12_veryfar_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AK_47_RELOAD_NORMAL = REGISTRY.register("ak_47_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_47_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AK_47_RELOAD_EMPTY = REGISTRY.register("ak_47_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_47_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> AK_12_FIRE_1P = REGISTRY.register("ak_12_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_12_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AK_12_FIRE_3P = REGISTRY.register("ak_12_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_12_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AK_12_FIRE_1P_S = REGISTRY.register("ak_12_fire_1p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_12_fire_1p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AK_12_FIRE_3P_S = REGISTRY.register("ak_12_fire_3p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_12_fire_3p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AK_12_FAR = REGISTRY.register("ak_12_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_12_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AK_12_VERYFAR = REGISTRY.register("ak_12_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_12_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AK_12_FAR_S = REGISTRY.register("ak_12_far_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_12_far_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AK_12_VERYFAR_S = REGISTRY.register("ak_12_veryfar_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_12_veryfar_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AK_12_RELOAD_NORMAL = REGISTRY.register("ak_12_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_12_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AK_12_RELOAD_EMPTY = REGISTRY.register("ak_12_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_12_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> LAND = REGISTRY.register("land", () -> SoundEvent.createVariableRangeEvent(Mod.loc("land")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HEADSHOT = REGISTRY.register("headshot", () -> SoundEvent.createVariableRangeEvent(Mod.loc("headshot")));

    public static final DeferredHolder<SoundEvent, SoundEvent> DEVOTION_FIRE_1P = REGISTRY.register("devotion_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("devotion_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> DEVOTION_FIRE_3P = REGISTRY.register("devotion_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("devotion_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> DEVOTION_FAR = REGISTRY.register("devotion_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("devotion_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> DEVOTION_VERYFAR = REGISTRY.register("devotion_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("devotion_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> DEVOTION_RELOAD_NORMAL = REGISTRY.register("devotion_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("devotion_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> DEVOTION_RELOAD_EMPTY = REGISTRY.register("devotion_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("devotion_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> RPG_FIRE_1P = REGISTRY.register("rpg_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("rpg_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> RPG_FIRE_3P = REGISTRY.register("rpg_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("rpg_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> RPG_FAR = REGISTRY.register("rpg_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("rpg_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> RPG_VERYFAR = REGISTRY.register("rpg_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("rpg_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> RPG_RELOAD_EMPTY = REGISTRY.register("rpg_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("rpg_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MORTAR_FIRE = REGISTRY.register("mortar_fire", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mortar_fire")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MORTAR_LOAD = REGISTRY.register("mortar_load", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mortar_load")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MORTAR_DISTANT = REGISTRY.register("mortar_distant", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mortar_distant")));

    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_RATE = REGISTRY.register("firerate", () -> SoundEvent.createVariableRangeEvent(Mod.loc("firerate")));

    public static final DeferredHolder<SoundEvent, SoundEvent> M_4_FIRE_1P = REGISTRY.register("m_4_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_4_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_4_FIRE_3P = REGISTRY.register("m_4_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_4_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_4_FIRE_1P_S = REGISTRY.register("m_4_fire_1p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_4_fire_1p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_4_FIRE_3P_S = REGISTRY.register("m_4_fire_3p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_4_fire_3p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_4_FAR = REGISTRY.register("m_4_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_4_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_4_VERYFAR = REGISTRY.register("m_4_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_4_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_4_RELOAD_NORMAL = REGISTRY.register("m_4_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_4_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_4_RELOAD_EMPTY = REGISTRY.register("m_4_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_4_reload_empty")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_4_FAR_S = REGISTRY.register("m_4_far_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_12_far_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_4_VERYFAR_S = REGISTRY.register("m_4_veryfar_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_12_veryfar_s")));

    public static final DeferredHolder<SoundEvent, SoundEvent> AA_12_FIRE_1P = REGISTRY.register("aa_12_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("aa_12_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AA_12_FIRE_3P = REGISTRY.register("aa_12_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("aa_12_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AA_12_FAR = REGISTRY.register("aa_12_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("aa_12_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AA_12_VERYFAR = REGISTRY.register("aa_12_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("aa_12_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AA_12_RELOAD_NORMAL = REGISTRY.register("aa_12_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("aa_12_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AA_12_RELOAD_EMPTY = REGISTRY.register("aa_12_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("aa_12_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BOCEK_ZOOM_FIRE_1P = REGISTRY.register("bocek_zoom_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bocek_zoom_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> BOCEK_ZOOM_FIRE_3P = REGISTRY.register("bocek_zoom_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bocek_zoom_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> BOCEK_SHATTER_CAP_FIRE_1P = REGISTRY.register("bocek_shatter_cap_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bocek_shatter_cap_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> BOCEK_SHATTER_CAP_FIRE_3P = REGISTRY.register("bocek_shatter_cap_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bocek_shatter_cap_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> BOCEK_PULL_1P = REGISTRY.register("bocek_pull_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bocek_pull_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> BOCEK_PULL_3P = REGISTRY.register("bocek_pull_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bocek_pull_3p")));

    public static final DeferredHolder<SoundEvent, SoundEvent> HK_416_FIRE_1P = REGISTRY.register("hk_416_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("hk_416_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HK_416_FIRE_3P = REGISTRY.register("hk_416_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("hk_416_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HK_416_FIRE_1P_S = REGISTRY.register("hk_416_fire_1p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("hk_416_fire_1p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HK_416_FIRE_3P_S = REGISTRY.register("hk_416_fire_3p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("hk_416_fire_3p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HK_416_FAR = REGISTRY.register("hk_416_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("hk_416_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HK_416_VERYFAR = REGISTRY.register("hk_416_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("hk_416_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HK_416_RELOAD_NORMAL = REGISTRY.register("hk_416_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("hk_416_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HK_416_RELOAD_EMPTY = REGISTRY.register("hk_416_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("hk_416_reload_empty")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HK_416_FAR_S = REGISTRY.register("hk_416_far_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_12_far_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HK_416_VERYFAR_S = REGISTRY.register("hk_416_veryfar_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_12_veryfar_s")));

    public static final DeferredHolder<SoundEvent, SoundEvent> RPK_FIRE_1P = REGISTRY.register("rpk_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("rpk_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> RPK_FIRE_3P = REGISTRY.register("rpk_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("rpk_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> RPK_FIRE_1P_S = REGISTRY.register("rpk_fire_1p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("rpk_fire_1p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> RPK_FIRE_3P_S = REGISTRY.register("rpk_fire_3p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("rpk_fire_3p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> RPK_FAR = REGISTRY.register("rpk_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("rpk_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> RPK_VERYFAR = REGISTRY.register("rpk_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("rpk_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> RPK_RELOAD_NORMAL = REGISTRY.register("rpk_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("rpk_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> RPK_RELOAD_EMPTY = REGISTRY.register("rpk_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("rpk_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> NTW_20_FIRE_1P = REGISTRY.register("ntw_20_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ntw_20_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> NTW_20_FIRE_3P = REGISTRY.register("ntw_20_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ntw_20_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> NTW_20_FAR = REGISTRY.register("ntw_20_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ntw_20_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> NTW_20_VERYFAR = REGISTRY.register("ntw_20_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ntw_20_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> NTW_20_RELOAD_NORMAL = REGISTRY.register("ntw_20_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ntw_20_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> NTW_20_RELOAD_EMPTY = REGISTRY.register("ntw_20_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ntw_20_reload_empty")));
    public static final DeferredHolder<SoundEvent, SoundEvent> NTW_20_BOLT = REGISTRY.register("ntw_20_bolt", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ntw_20_bolt")));

    public static final DeferredHolder<SoundEvent, SoundEvent> VECTOR_FIRE_1P = REGISTRY.register("vector_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("vector_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> VECTOR_FIRE_3P = REGISTRY.register("vector_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("vector_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> VECTOR_FAR = REGISTRY.register("vector_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("vector_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> VECTOR_VERYFAR = REGISTRY.register("vector_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("vector_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> VECTOR_FIRE_1P_S = REGISTRY.register("vector_fire_1p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("vector_fire_1p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> VECTOR_FIRE_3P_S = REGISTRY.register("vector_fire_3p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("vector_fire_3p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> VECTOR_FAR_S = REGISTRY.register("vector_far_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("vector_far_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> VECTOR_RELOAD_NORMAL = REGISTRY.register("vector_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("vector_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> VECTOR_RELOAD_EMPTY = REGISTRY.register("vector_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("vector_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_FIRE_1P = REGISTRY.register("minigun_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("minigun_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_FIRE_3P = REGISTRY.register("minigun_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("minigun_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_FAR = REGISTRY.register("minigun_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("minigun_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_VERYFAR = REGISTRY.register("minigun_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("minigun_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_ROT = REGISTRY.register("minigun_rot", () -> SoundEvent.createVariableRangeEvent(Mod.loc("minigun_rot")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_OVERHEAT = REGISTRY.register("minigun_overheat", () -> SoundEvent.createVariableRangeEvent(Mod.loc("minigun_overheat")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MK_14_FIRE_1P = REGISTRY.register("mk_14_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mk_14_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MK_14_FIRE_3P = REGISTRY.register("mk_14_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mk_14_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MK_14_FAR = REGISTRY.register("mk_14_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mk_14_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MK_14_VERYFAR = REGISTRY.register("mk_14_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mk_14_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MK_14_FIRE_1P_S = REGISTRY.register("mk_14_fire_1p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mk_14_fire_1p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MK_14_FIRE_3P_S = REGISTRY.register("mk_14_fire_3p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mk_14_fire_3p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MK_14_FAR_S = REGISTRY.register("mk_14_far_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mk_14_far_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MK_14_RELOAD_NORMAL = REGISTRY.register("mk_14_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mk_14_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MK_14_RELOAD_EMPTY = REGISTRY.register("mk_14_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mk_14_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_FIRE_1P = REGISTRY.register("sentinel_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sentinel_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_FIRE_3P = REGISTRY.register("sentinel_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sentinel_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_CHARGE_FIRE_1P = REGISTRY.register("sentinel_charge_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sentinel_charge_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_CHARGE_FIRE_3P = REGISTRY.register("sentinel_charge_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sentinel_charge_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_FAR = REGISTRY.register("sentinel_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sentinel_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_VERYFAR = REGISTRY.register("sentinel_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sentinel_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_CHARGE_FAR = REGISTRY.register("sentinel_charge_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sentinel_charge_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_CHARGE_VERYFAR = REGISTRY.register("sentinel_charge_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sentinel_charge_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_RELOAD_NORMAL = REGISTRY.register("sentinel_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sentinel_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_RELOAD_EMPTY = REGISTRY.register("sentinel_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sentinel_reload_empty")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_CHARGE = REGISTRY.register("sentinel_charge", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sentinel_charge")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTINEL_BOLT = REGISTRY.register("sentinel_bolt", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sentinel_bolt")));

    public static final DeferredHolder<SoundEvent, SoundEvent> M_60_FIRE_1P = REGISTRY.register("m_60_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_60_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_60_FIRE_3P = REGISTRY.register("m_60_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_60_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_60_FAR = REGISTRY.register("m_60_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_60_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_60_VERYFAR = REGISTRY.register("m_60_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_60_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_60_RELOAD_NORMAL = REGISTRY.register("m_60_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_60_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_60_RELOAD_EMPTY = REGISTRY.register("m_60_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_60_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> SVD_FIRE_1P = REGISTRY.register("svd_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("svd_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SVD_FIRE_3P = REGISTRY.register("svd_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("svd_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SVD_FAR = REGISTRY.register("svd_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("svd_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SVD_VERYFAR = REGISTRY.register("svd_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("svd_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SVD_FIRE_1P_S = REGISTRY.register("svd_fire_1p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("svd_fire_1p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SVD_FIRE_3P_S = REGISTRY.register("svd_fire_3p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("svd_fire_3p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SVD_FAR_S = REGISTRY.register("svd_far_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("svd_far_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SVD_RELOAD_NORMAL = REGISTRY.register("svd_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("svd_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SVD_RELOAD_EMPTY = REGISTRY.register("svd_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("svd_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> M_98B_FIRE_1P = REGISTRY.register("m_98b_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_98b_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_98B_FIRE_3P = REGISTRY.register("m_98b_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_98b_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_98B_FIRE_1P_S = REGISTRY.register("m_98b_fire_1p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_98b_fire_1p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_98B_FIRE_3P_S = REGISTRY.register("m_98b_fire_3p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_98b_fire_3p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_98B_FAR = REGISTRY.register("m_98b_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_98b_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_98B_VERYFAR = REGISTRY.register("m_98b_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_98b_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_98B_RELOAD_NORMAL = REGISTRY.register("m_98b_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_98b_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_98B_RELOAD_EMPTY = REGISTRY.register("m_98b_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_98b_reload_empty")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_98B_BOLT = REGISTRY.register("m_98b_bolt", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_98b_bolt")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MARLIN_FIRE_1P = REGISTRY.register("marlin_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("marlin_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MARLIN_FIRE_3P = REGISTRY.register("marlin_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("marlin_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MARLIN_FAR = REGISTRY.register("marlin_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("marlin_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MARLIN_VERYFAR = REGISTRY.register("marlin_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("marlin_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MARLIN_PREPARE = REGISTRY.register("marlin_prepare", () -> SoundEvent.createVariableRangeEvent(Mod.loc("marlin_prepare")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MARLIN_LOOP = REGISTRY.register("marlin_loop", () -> SoundEvent.createVariableRangeEvent(Mod.loc("marlin_loop")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MARLIN_END = REGISTRY.register("marlin_end", () -> SoundEvent.createVariableRangeEvent(Mod.loc("marlin_end")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MARLIN_BOLT = REGISTRY.register("marlin_bolt", () -> SoundEvent.createVariableRangeEvent(Mod.loc("marlin_bolt")));

    public static final DeferredHolder<SoundEvent, SoundEvent> M_870_FIRE_1P = REGISTRY.register("m_870_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_870_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_870_FIRE_3P = REGISTRY.register("m_870_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_870_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_870_FAR = REGISTRY.register("m_870_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_870_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_870_VERYFAR = REGISTRY.register("m_870_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_870_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_870_PREPARE_LOAD = REGISTRY.register("m_870_prepare_load", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_870_prepare_load")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_870_LOOP = REGISTRY.register("m_870_loop", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_870_loop")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_870_BOLT = REGISTRY.register("m_870_bolt", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_870_bolt")));

    public static final DeferredHolder<SoundEvent, SoundEvent> GLOCK_17_FIRE_1P = REGISTRY.register("glock_17_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("glock_17_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOCK_17_FIRE_3P = REGISTRY.register("glock_17_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("glock_17_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOCK_17_FAR = REGISTRY.register("glock_17_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("glock_17_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOCK_17_VERYFAR = REGISTRY.register("glock_17_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("glock_17_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOCK_17_RELOAD_NORMAL = REGISTRY.register("glock_17_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("glock_17_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOCK_17_RELOAD_EMPTY = REGISTRY.register("glock_17_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("glock_17_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> GLOCK_18_FIRE_1P = REGISTRY.register("glock_18_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("glock_17_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOCK_18_FIRE_3P = REGISTRY.register("glock_18_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("glock_17_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOCK_18_FAR = REGISTRY.register("glock_18_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("glock_17_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOCK_18_VERYFAR = REGISTRY.register("glock_18_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("glock_17_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOCK_18_RELOAD_NORMAL = REGISTRY.register("glock_18_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("glock_17_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOCK_18_RELOAD_EMPTY = REGISTRY.register("glock_18_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("glock_17_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MP_443_FIRE_1P = REGISTRY.register("mp_443_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mp_443_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MP_443_FIRE_3P = REGISTRY.register("mp_443_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mp_443_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MP_443_FAR = REGISTRY.register("mp_443_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("glock_17_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MP_443_VERYFAR = REGISTRY.register("mp_443_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("glock_17_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MP_443_RELOAD_NORMAL = REGISTRY.register("mp_443_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("glock_17_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MP_443_RELOAD_EMPTY = REGISTRY.register("mp_443_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("glock_17_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> M_1911_FIRE_1P = REGISTRY.register("m_1911_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_1911_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_1911_FIRE_3P = REGISTRY.register("m_1911_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_1911_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_1911_FAR = REGISTRY.register("m_1911_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_1911_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_1911_VERYFAR = REGISTRY.register("m_1911_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m_1911_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_1911_RELOAD_NORMAL = REGISTRY.register("m_1911_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("glock_17_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_1911_RELOAD_EMPTY = REGISTRY.register("m_1911_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("glock_17_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> QBZ_95_FIRE_1P = REGISTRY.register("qbz_95_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("qbz_95_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QBZ_95_FIRE_3P = REGISTRY.register("qbz_95_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("qbz_95_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QBZ_95_FAR = REGISTRY.register("qbz_95_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("qbz_95_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QBZ_95_VERYFAR = REGISTRY.register("qbz_95_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("qbz_95_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QBZ_95_RELOAD_NORMAL = REGISTRY.register("qbz_95_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("qbz_95_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QBZ_95_RELOAD_EMPTY = REGISTRY.register("qbz_95_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("qbz_95_reload_empty")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QBZ_95_FIRE_1P_S = REGISTRY.register("qbz_95_fire_1p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("qbz_95_fire_1p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QBZ_95_FIRE_3P_S = REGISTRY.register("qbz_95_fire_3p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("qbz_95_fire_3p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QBZ_95_FAR_S = REGISTRY.register("qbz_95_far_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_12_far_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QBZ_95_VERYFAR_S = REGISTRY.register("qbz_95_veryfar_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ak_12_veryfar_s")));

    public static final DeferredHolder<SoundEvent, SoundEvent> K_98_FIRE_1P = REGISTRY.register("k_98_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("k_98_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> K_98_FIRE_3P = REGISTRY.register("k_98_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("k_98_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> K_98_FAR = REGISTRY.register("k_98_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("k_98_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> K_98_VERYFAR = REGISTRY.register("k_98_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("k_98_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> K_98_RELOAD_EMPTY = REGISTRY.register("k_98_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("k_98_reload_empty")));
    public static final DeferredHolder<SoundEvent, SoundEvent> K_98_BOLT = REGISTRY.register("k_98_bolt", () -> SoundEvent.createVariableRangeEvent(Mod.loc("k_98_bolt")));
    public static final DeferredHolder<SoundEvent, SoundEvent> K_98_PREPARE = REGISTRY.register("k_98_prepare", () -> SoundEvent.createVariableRangeEvent(Mod.loc("k_98_prepare")));
    public static final DeferredHolder<SoundEvent, SoundEvent> K_98_LOOP = REGISTRY.register("k_98_loop", () -> SoundEvent.createVariableRangeEvent(Mod.loc("k_98_loop")));
    public static final DeferredHolder<SoundEvent, SoundEvent> K_98_END = REGISTRY.register("k_98_end", () -> SoundEvent.createVariableRangeEvent(Mod.loc("k_98_end")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MOSIN_NAGANT_FIRE_1P = REGISTRY.register("mosin_nagant_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mosin_nagant_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSIN_NAGANT_FIRE_3P = REGISTRY.register("mosin_nagant_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mosin_nagant_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSIN_NAGANT_FAR = REGISTRY.register("mosin_nagant_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mosin_nagant_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSIN_NAGANT_VERYFAR = REGISTRY.register("mosin_nagant_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mosin_nagant_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSIN_NAGANT_BOLT = REGISTRY.register("mosin_nagant_bolt", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mosin_nagant_bolt")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSIN_NAGANT_PREPARE = REGISTRY.register("mosin_nagant_prepare", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mosin_nagant_prepare")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSIN_NAGANT_PREPARE_EMPTY = REGISTRY.register("mosin_nagant_prepare_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mosin_nagant_prepare_empty")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSIN_NAGANT_LOOP = REGISTRY.register("mosin_nagant_loop", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mosin_nagant_loop")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSIN_NAGANT_END = REGISTRY.register("mosin_nagant_end", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mosin_nagant_end")));

    public static final DeferredHolder<SoundEvent, SoundEvent> JAVELIN_FIRE_1P = REGISTRY.register("javelin_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("javelin_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> JAVELIN_FIRE_3P = REGISTRY.register("javelin_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("javelin_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> JAVELIN_FAR = REGISTRY.register("javelin_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("javelin_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> JAVELIN_RELOAD_EMPTY = REGISTRY.register("javelin_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("javelin_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> JAVELIN_LOCK = REGISTRY.register("javelin_lock", () -> SoundEvent.createVariableRangeEvent(Mod.loc("javelin_lock")));
    public static final DeferredHolder<SoundEvent, SoundEvent> JAVELIN_LOCKON = REGISTRY.register("javelin_lockon", () -> SoundEvent.createVariableRangeEvent(Mod.loc("javelin_lockon")));

    public static final DeferredHolder<SoundEvent, SoundEvent> SECONDARY_CATACLYSM_FIRE_1P = REGISTRY.register("secondary_cataclysm_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("secondary_cataclysm_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SECONDARY_CATACLYSM_FIRE_3P = REGISTRY.register("secondary_cataclysm_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("secondary_cataclysm_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SECONDARY_CATACLYSM_FAR = REGISTRY.register("secondary_cataclysm_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("secondary_cataclysm_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SECONDARY_CATACLYSM_VERYFAR = REGISTRY.register("secondary_cataclysm_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("secondary_cataclysm_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SECONDARY_CATACLYSM_FIRE_1P_CHARGE = REGISTRY.register("secondary_cataclysm_fire_1p_charge", () -> SoundEvent.createVariableRangeEvent(Mod.loc("secondary_cataclysm_fire_1p_charge")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SECONDARY_CATACLYSM_FIRE_3P_CHARGE = REGISTRY.register("secondary_cataclysm_fire_3p_charge", () -> SoundEvent.createVariableRangeEvent(Mod.loc("secondary_cataclysm_fire_3p_charge")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SECONDARY_CATACLYSM_FAR_CHARGE = REGISTRY.register("secondary_cataclysm_far_charge", () -> SoundEvent.createVariableRangeEvent(Mod.loc("secondary_cataclysm_far_charge")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SECONDARY_CATACLYSM_VERYFAR_CHARGE = REGISTRY.register("secondary_cataclysm_veryfar_charge", () -> SoundEvent.createVariableRangeEvent(Mod.loc("secondary_cataclysm_veryfar_charge")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SECONDARY_CATACLYSM_PREPARE_LOAD = REGISTRY.register("secondary_cataclysm_prepare_load", () -> SoundEvent.createVariableRangeEvent(Mod.loc("secondary_cataclysm_prepare_load")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SECONDARY_CATACLYSM_LOOP = REGISTRY.register("secondary_cataclysm_loop", () -> SoundEvent.createVariableRangeEvent(Mod.loc("secondary_cataclysm_loop")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SECONDARY_CATACLYSM_END = REGISTRY.register("secondary_cataclysm_end", () -> SoundEvent.createVariableRangeEvent(Mod.loc("secondary_cataclysm_end")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MP_5_FIRE_1P = REGISTRY.register("mp_5_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mp_5_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MP_5_FIRE_3P = REGISTRY.register("mp_5_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mp_5_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MP_5_FAR = REGISTRY.register("mp_5_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mp_5_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MP_5_VERYFAR = REGISTRY.register("mp_5_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mp_5_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MP_5_RELOAD_NORMAL = REGISTRY.register("mp_5_reload_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mp_5_reload_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MP_5_RELOAD_EMPTY = REGISTRY.register("mp_5_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mp_5_reload_empty")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MP_5_FIRE_1P_S = REGISTRY.register("mp_5_fire_1p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mp_5_fire_1p_s")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MP_5_FIRE_3P_S = REGISTRY.register("mp_5_fire_3p_s", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mp_5_fire_3p_s")));

    public static final DeferredHolder<SoundEvent, SoundEvent> M_2_FIRE_1P = REGISTRY.register("m2_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m2_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_2_FIRE_3P = REGISTRY.register("m2_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m2_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_2_FAR = REGISTRY.register("m2_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m2_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> M_2_VERYFAR = REGISTRY.register("m2_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("m2_veryfar")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MK_42_FIRE_1P = REGISTRY.register("mk_42_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mk_42_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MK_42_FIRE_3P = REGISTRY.register("mk_42_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mk_42_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MK_42_FAR = REGISTRY.register("mk_42_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mk_42_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MK_42_VERYFAR = REGISTRY.register("mk_42_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mk_42_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_RELOAD = REGISTRY.register("cannon_reload", () -> SoundEvent.createVariableRangeEvent(Mod.loc("cannon_reload")));
    public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_ZOOM_IN = REGISTRY.register("cannon_zoom_in", () -> SoundEvent.createVariableRangeEvent(Mod.loc("cannon_zoom_in")));
    public static final DeferredHolder<SoundEvent, SoundEvent> CANNON_ZOOM_OUT = REGISTRY.register("cannon_zoom_out", () -> SoundEvent.createVariableRangeEvent(Mod.loc("cannon_zoom_out")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_SUPPLY = REGISTRY.register("bullet_supply", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bullet_supply")));
    public static final DeferredHolder<SoundEvent, SoundEvent> ADJUST_FOV = REGISTRY.register("adjust_fov", () -> SoundEvent.createVariableRangeEvent(Mod.loc("adjust_fov")));
    public static final DeferredHolder<SoundEvent, SoundEvent> DRONE_SOUND = REGISTRY.register("drone_sound", () -> SoundEvent.createVariableRangeEvent(Mod.loc("drone_sound")));
    public static final DeferredHolder<SoundEvent, SoundEvent> GRENADE_PULL = REGISTRY.register("grenade_pull", () -> SoundEvent.createVariableRangeEvent(Mod.loc("grenade_pull")));
    public static final DeferredHolder<SoundEvent, SoundEvent> GRENADE_THROW = REGISTRY.register("grenade_throw", () -> SoundEvent.createVariableRangeEvent(Mod.loc("grenade_throw")));

    public static final DeferredHolder<SoundEvent, SoundEvent> EDIT_MODE = REGISTRY.register("edit_mode", () -> SoundEvent.createVariableRangeEvent(Mod.loc("edit_mode")));
    public static final DeferredHolder<SoundEvent, SoundEvent> EDIT = REGISTRY.register("edit", () -> SoundEvent.createVariableRangeEvent(Mod.loc("edit")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SHELL_CASING_NORMAL = REGISTRY.register("shell_casing_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("shell_casing_normal")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SHELL_CASING_SHOTGUN = REGISTRY.register("shell_casing_shotgun", () -> SoundEvent.createVariableRangeEvent(Mod.loc("shell_casing_shotgun")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SHELL_CASING_50CAL = REGISTRY.register("shell_casing_50cal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("shell_casing_50cal")));

    public static final DeferredHolder<SoundEvent, SoundEvent> OPEN = REGISTRY.register("open", () -> SoundEvent.createVariableRangeEvent(Mod.loc("open")));

    public static final DeferredHolder<SoundEvent, SoundEvent> CHARGE_RIFLE_FIRE_1P = REGISTRY.register("charge_rifle_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("charge_rifle_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARGE_RIFLE_FIRE_3P = REGISTRY.register("charge_rifle_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("charge_rifle_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARGE_RIFLE_FIRE_BOOM_1P = REGISTRY.register("charge_rifle_fire_boom_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("charge_rifle_fire_boom_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARGE_RIFLE_FIRE_BOOM_3P = REGISTRY.register("charge_rifle_fire_boom_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("charge_rifle_fire_boom_3p")));

    public static final DeferredHolder<SoundEvent, SoundEvent> ANNIHILATOR_FIRE_1P = REGISTRY.register("annihilator_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("annihilator_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNIHILATOR_FIRE_3P = REGISTRY.register("annihilator_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("annihilator_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNIHILATOR_FAR = REGISTRY.register("annihilator_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("annihilator_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNIHILATOR_VERYFAR = REGISTRY.register("annihilator_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("annihilator_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNIHILATOR_RELOAD = REGISTRY.register("annihilator_reload", () -> SoundEvent.createVariableRangeEvent(Mod.loc("annihilator_reload")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BOAT_ENGINE = REGISTRY.register("boat_engine", () -> SoundEvent.createVariableRangeEvent(Mod.loc("boat_engine")));
    public static final DeferredHolder<SoundEvent, SoundEvent> VEHICLE_STRIKE = REGISTRY.register("vehicle_strike", () -> SoundEvent.createVariableRangeEvent(Mod.loc("vehicle_strike")));
    public static final DeferredHolder<SoundEvent, SoundEvent> WHEEL_CHAIR_ENGINE = REGISTRY.register("wheel_chair_engine", () -> SoundEvent.createVariableRangeEvent(Mod.loc("wheel_chair_engine")));
    public static final DeferredHolder<SoundEvent, SoundEvent> WHEEL_CHAIR_JUMP = REGISTRY.register("wheel_chair_jump", () -> SoundEvent.createVariableRangeEvent(Mod.loc("wheel_chair_jump")));

    public static final DeferredHolder<SoundEvent, SoundEvent> RADAR_SEARCH_START = REGISTRY.register("radar_search_start", () -> SoundEvent.createVariableRangeEvent(Mod.loc("radar_search_start")));
    public static final DeferredHolder<SoundEvent, SoundEvent> RADAR_SEARCH_IDLE = REGISTRY.register("radar_search_idle", () -> SoundEvent.createVariableRangeEvent(Mod.loc("radar_search_idle")));
    public static final DeferredHolder<SoundEvent, SoundEvent> RADAR_SEARCH_END = REGISTRY.register("radar_search_end", () -> SoundEvent.createVariableRangeEvent(Mod.loc("radar_search_end")));

    public static final DeferredHolder<SoundEvent, SoundEvent> HELICOPTER_ENGINE_START = REGISTRY.register("helicopter_engine_start", () -> SoundEvent.createVariableRangeEvent(Mod.loc("helicopter_engine_start")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HELICOPTER_ENGINE = REGISTRY.register("helicopter_engine", () -> SoundEvent.createVariableRangeEvent(Mod.loc("helicopter_engine")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HELICOPTER_CANNON_FIRE_1P = REGISTRY.register("heli_cannon_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("heli_cannon_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HELICOPTER_CANNON_FIRE_3P = REGISTRY.register("heli_cannon_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("heli_cannon_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HELICOPTER_CANNON_FAR = REGISTRY.register("heli_cannon_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("heli_cannon_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HELICOPTER_CANNON_VERYFAR = REGISTRY.register("heli_cannon_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("heli_cannon_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HELICOPTER_ROCKET_FIRE_1P = REGISTRY.register("heli_rocket_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("heli_rocket_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HELICOPTER_ROCKET_FIRE_3P = REGISTRY.register("heli_rocket_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("heli_rocket_fire_3p")));

    public static final DeferredHolder<SoundEvent, SoundEvent> INTO_CANNON = REGISTRY.register("into_cannon", () -> SoundEvent.createVariableRangeEvent(Mod.loc("into_cannon")));
    public static final DeferredHolder<SoundEvent, SoundEvent> INTO_MISSILE = REGISTRY.register("into_missile", () -> SoundEvent.createVariableRangeEvent(Mod.loc("into_missile")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MISSILE_RELOAD = REGISTRY.register("missile_reload", () -> SoundEvent.createVariableRangeEvent(Mod.loc("missile_reload")));

    public static final DeferredHolder<SoundEvent, SoundEvent> LOW_HEALTH = REGISTRY.register("low_health", () -> SoundEvent.createVariableRangeEvent(Mod.loc("low_health")));
    public static final DeferredHolder<SoundEvent, SoundEvent> NO_HEALTH = REGISTRY.register("no_health", () -> SoundEvent.createVariableRangeEvent(Mod.loc("no_health")));

    public static final DeferredHolder<SoundEvent, SoundEvent> LOCKING_WARNING = REGISTRY.register("locking_warning", () -> SoundEvent.createVariableRangeEvent(Mod.loc("locking_warning")));
    public static final DeferredHolder<SoundEvent, SoundEvent> LOCKED_WARNING = REGISTRY.register("locked_warning", () -> SoundEvent.createVariableRangeEvent(Mod.loc("locked_warning")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MISSILE_WARNING = REGISTRY.register("missile_warning", () -> SoundEvent.createVariableRangeEvent(Mod.loc("missile_warning")));

    public static final DeferredHolder<SoundEvent, SoundEvent> DECOY_FIRE = REGISTRY.register("decoy_fire", () -> SoundEvent.createVariableRangeEvent(Mod.loc("decoy_fire")));
    public static final DeferredHolder<SoundEvent, SoundEvent> DECOY_RELOAD = REGISTRY.register("decoy_reload", () -> SoundEvent.createVariableRangeEvent(Mod.loc("decoy_reload")));
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNGE_MINE_GROWL = REGISTRY.register("lunge_mine_growl", () -> SoundEvent.createVariableRangeEvent(Mod.loc("lunge_mine_growl")));
    public static final DeferredHolder<SoundEvent, SoundEvent> LAV_CANNON_FIRE_1P = REGISTRY.register("lav_cannon_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("lav_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> LAV_CANNON_FIRE_3P = REGISTRY.register("lav_cannon_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("lav_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> LAV_CANNON_FAR = REGISTRY.register("lav_cannon_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("lav_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> LAV_CANNON_VERYFAR = REGISTRY.register("lav_cannon_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("lav_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> LAV_ENGINE = REGISTRY.register("lav_engine", () -> SoundEvent.createVariableRangeEvent(Mod.loc("lav_engine")));
    public static final DeferredHolder<SoundEvent, SoundEvent> COAX_FIRE_1P = REGISTRY.register("coax_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("coax_fire_1p")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BMP_CANNON_FIRE_1P = REGISTRY.register("bmp_cannon_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bmp_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> BMP_CANNON_FIRE_3P = REGISTRY.register("bmp_cannon_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bmp_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> BMP_ENGINE = REGISTRY.register("bmp_engine", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bmp_engine")));
    public static final DeferredHolder<SoundEvent, SoundEvent> BMP_MISSILE_FIRE_1P = REGISTRY.register("bmp_missile_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bmp_missile_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> BMP_MISSILE_FIRE_3P = REGISTRY.register("bmp_missile_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bmp_missile_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> BMP_MISSILE_RELOAD = REGISTRY.register("bmp_missile_reload", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bmp_missile_reload")));

    public static final DeferredHolder<SoundEvent, SoundEvent> WHEEL_STEP = REGISTRY.register("wheel_step", () -> SoundEvent.createVariableRangeEvent(Mod.loc("wheel_step")));
    public static final DeferredHolder<SoundEvent, SoundEvent> LASER_TOWER_SHOOT = REGISTRY.register("laser_tower_shoot", () -> SoundEvent.createVariableRangeEvent(Mod.loc("laser_tower_shoot")));

    public static final DeferredHolder<SoundEvent, SoundEvent> YX_100_RELOAD = REGISTRY.register("yx_100_reload", () -> SoundEvent.createVariableRangeEvent(Mod.loc("yx_100_reload")));
    public static final DeferredHolder<SoundEvent, SoundEvent> YX_100_FIRE_1P = REGISTRY.register("yx_100_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("yx_100_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> YX_100_FIRE_3P = REGISTRY.register("yx_100_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("yx_100_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> YX_100_FAR = REGISTRY.register("yx_100_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("yx_100_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> YX_100_VERYFAR = REGISTRY.register("yx_100_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("yx_100_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> YX_100_ENGINE = REGISTRY.register("yx_100_engine", () -> SoundEvent.createVariableRangeEvent(Mod.loc("yx_100_engine")));

    public static final DeferredHolder<SoundEvent, SoundEvent> TURRET_TURN = REGISTRY.register("turret_turn", () -> SoundEvent.createVariableRangeEvent(Mod.loc("turret_turn")));
    public static final DeferredHolder<SoundEvent, SoundEvent> C4_BEEP = REGISTRY.register("c4_beep", () -> SoundEvent.createVariableRangeEvent(Mod.loc("c4_beep")));
    public static final DeferredHolder<SoundEvent, SoundEvent> C4_FINAL = REGISTRY.register("c4_final", () -> SoundEvent.createVariableRangeEvent(Mod.loc("c4_final")));
    public static final DeferredHolder<SoundEvent, SoundEvent> C4_THROW = REGISTRY.register("c4_throw", () -> SoundEvent.createVariableRangeEvent(Mod.loc("c4_throw")));
    public static final DeferredHolder<SoundEvent, SoundEvent> C4_DETONATOR_CLICK = REGISTRY.register("c4_detonator_click", () -> SoundEvent.createVariableRangeEvent(Mod.loc("c4_detonator_click")));

    public static final DeferredHolder<SoundEvent, SoundEvent> PRISM_FIRE_1P = REGISTRY.register("prism_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("prism_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> PRISM_FIRE_3P = REGISTRY.register("prism_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("prism_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> PRISM_FIRE_1P_2 = REGISTRY.register("prism_fire_1p_2", () -> SoundEvent.createVariableRangeEvent(Mod.loc("prism_fire_1p_2")));
    public static final DeferredHolder<SoundEvent, SoundEvent> PRISM_FIRE_3P_2 = REGISTRY.register("prism_fire_3p_2", () -> SoundEvent.createVariableRangeEvent(Mod.loc("prism_fire_3p_2")));
    public static final DeferredHolder<SoundEvent, SoundEvent> PRISM_ENGINE = REGISTRY.register("prism_engine", () -> SoundEvent.createVariableRangeEvent(Mod.loc("prism_engine")));

    public static final DeferredHolder<SoundEvent, SoundEvent> INSIDIOUS_FIRE_1P = REGISTRY.register("insidious_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("insidious_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> INSIDIOUS_FIRE_3P = REGISTRY.register("insidious_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("insidious_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> INSIDIOUS_FAR = REGISTRY.register("insidious_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("insidious_far")));
    public static final DeferredHolder<SoundEvent, SoundEvent> INSIDIOUS_VERYFAR = REGISTRY.register("insidious_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("insidious_veryfar")));
    public static final DeferredHolder<SoundEvent, SoundEvent> INSIDIOUS_RELOAD_EMPTY = REGISTRY.register("insidious_reload_empty", () -> SoundEvent.createVariableRangeEvent(Mod.loc("insidious_reload_empty")));

    public static final DeferredHolder<SoundEvent, SoundEvent> SMOKE_FIRE = REGISTRY.register("smoke_fire", () -> SoundEvent.createVariableRangeEvent(Mod.loc("smoke_fire")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HPJ_11_FIRE_3P = REGISTRY.register("hpj_11_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("hpj_11_fire_3p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TRACK_MOVE = REGISTRY.register("track_move", () -> SoundEvent.createVariableRangeEvent(Mod.loc("track_move")));
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_FLY = REGISTRY.register("rocket_fly", () -> SoundEvent.createVariableRangeEvent(Mod.loc("rocket_fly")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SHELL_FLY = REGISTRY.register("shell_fly", () -> SoundEvent.createVariableRangeEvent(Mod.loc("shell_fly")));
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_ENGINE = REGISTRY.register("rocket_engine", () -> SoundEvent.createVariableRangeEvent(Mod.loc("rocket_engine")));
    public static final DeferredHolder<SoundEvent, SoundEvent> VEHICLE_SWIM = REGISTRY.register("vehicle_swim", () -> SoundEvent.createVariableRangeEvent(Mod.loc("vehicle_swim")));
    public static final DeferredHolder<SoundEvent, SoundEvent> A_10_ENGINE = REGISTRY.register("a10_engine", () -> SoundEvent.createVariableRangeEvent(Mod.loc("a10_engine")));
    public static final DeferredHolder<SoundEvent, SoundEvent> A_10_FIRE = REGISTRY.register("a10_fire", () -> SoundEvent.createVariableRangeEvent(Mod.loc("a10_fire")));
    public static final DeferredHolder<SoundEvent, SoundEvent> BOMB_RELEASE = REGISTRY.register("bomb_release", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bomb_release")));
    public static final DeferredHolder<SoundEvent, SoundEvent> BOMB_RELOAD = REGISTRY.register("bomb_reload", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bomb_reload")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MISSILE_START = REGISTRY.register("missile_start", () -> SoundEvent.createVariableRangeEvent(Mod.loc("missile_start")));
    public static final DeferredHolder<SoundEvent, SoundEvent> JET_LOCK = REGISTRY.register("jet_lock", () -> SoundEvent.createVariableRangeEvent(Mod.loc("jet_lock")));
    public static final DeferredHolder<SoundEvent, SoundEvent> JET_LOCKON = REGISTRY.register("jet_lockon", () -> SoundEvent.createVariableRangeEvent(Mod.loc("jet_lockon")));
    public static final DeferredHolder<SoundEvent, SoundEvent> FLY_LOOP = REGISTRY.register("fly_loop", () -> SoundEvent.createVariableRangeEvent(Mod.loc("fly_loop")));

    public static final DeferredHolder<SoundEvent, SoundEvent> AURELIA_SCEPTRE_FIRE_1P = REGISTRY.register("aurelia_sceptre_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("aurelia_sceptre_fire_1p")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AURELIA_SCEPTRE_FIRE_3P = REGISTRY.register("aurelia_sceptre_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("aurelia_sceptre_fire_3p")));

    public static final DeferredHolder<SoundEvent, SoundEvent> DPS_GENERATOR_EVOLVE = REGISTRY.register("dps_generator_evolve", () -> SoundEvent.createVariableRangeEvent(Mod.loc("dps_generator_evolve")));
}

