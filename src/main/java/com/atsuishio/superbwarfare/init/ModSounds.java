package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("unused")
public class ModSounds {

    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Mod.MODID);

    public static final RegistryObject<SoundEvent> SHOCK = REGISTRY.register("shock", () -> SoundEvent.createVariableRangeEvent(Mod.loc("shock")));
    public static final RegistryObject<SoundEvent> ELECTRIC = REGISTRY.register("electric", () -> SoundEvent.createVariableRangeEvent(Mod.loc("electric")));
    public static final RegistryObject<SoundEvent> MELEE_HIT = REGISTRY.register("melee_hit", () -> SoundEvent.createVariableRangeEvent(Mod.loc("melee_hit")));

    public static final RegistryObject<SoundEvent> TRIGGER_CLICK = REGISTRY.register("trigger_click", () -> SoundEvent.createVariableRangeEvent(Mod.loc("trigger_click")));
    public static final RegistryObject<SoundEvent> HIT = REGISTRY.register("hit", () -> SoundEvent.createVariableRangeEvent(Mod.loc("hit")));
    public static final RegistryObject<SoundEvent> TARGET_DOWN = REGISTRY.register("targetdown", () -> SoundEvent.createVariableRangeEvent(Mod.loc("targetdown")));
    public static final RegistryObject<SoundEvent> INDICATION = REGISTRY.register("indication", () -> SoundEvent.createVariableRangeEvent(Mod.loc("indication")));
    public static final RegistryObject<SoundEvent> INDICATION_VEHICLE = REGISTRY.register("indication_vehicle", () -> SoundEvent.createVariableRangeEvent(Mod.loc("indication_vehicle")));
    public static final RegistryObject<SoundEvent> JUMP = REGISTRY.register("jump", () -> SoundEvent.createVariableRangeEvent(Mod.loc("jump")));
    public static final RegistryObject<SoundEvent> DOUBLE_JUMP = REGISTRY.register("doublejump", () -> SoundEvent.createVariableRangeEvent(Mod.loc("doublejump")));

    public static final RegistryObject<SoundEvent> MINI_EXPLOSION = REGISTRY.register("mini_explosion", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mini_explosion")));
    public static final RegistryObject<SoundEvent> EXPLOSION_CLOSE = REGISTRY.register("explosion_close", () -> SoundEvent.createVariableRangeEvent(Mod.loc("explosion_close")));
    public static final RegistryObject<SoundEvent> EXPLOSION_FAR = REGISTRY.register("explosion_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("explosion_far")));
    public static final RegistryObject<SoundEvent> EXPLOSION_VERY_FAR = REGISTRY.register("explosion_very_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("explosion_very_far")));
    public static final RegistryObject<SoundEvent> HUGE_EXPLOSION_CLOSE = REGISTRY.register("huge_explosion_close", () -> SoundEvent.createVariableRangeEvent(Mod.loc("huge_explosion_close")));
    public static final RegistryObject<SoundEvent> HUGE_EXPLOSION_FAR = REGISTRY.register("huge_explosion_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("huge_explosion_far")));
    public static final RegistryObject<SoundEvent> HUGE_EXPLOSION_VERY_FAR = REGISTRY.register("huge_explosion_very_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("huge_explosion_very_far")));
    public static final RegistryObject<SoundEvent> EXPLOSION_WATER = REGISTRY.register("explosion_water", () -> SoundEvent.createVariableRangeEvent(Mod.loc("explosion_water")));
    public static final RegistryObject<SoundEvent> EXPLOSION_AIR = REGISTRY.register("explosion_air", () -> SoundEvent.createVariableRangeEvent(Mod.loc("explosion_air")));

    public static final RegistryObject<SoundEvent> OUCH = REGISTRY.register("ouch", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ouch")));
    public static final RegistryObject<SoundEvent> STEP = REGISTRY.register("step", () -> SoundEvent.createVariableRangeEvent(Mod.loc("step")));
    public static final RegistryObject<SoundEvent> GROWL = REGISTRY.register("growl", () -> SoundEvent.createVariableRangeEvent(Mod.loc("growl")));
    public static final RegistryObject<SoundEvent> IDLE = REGISTRY.register("idle", () -> SoundEvent.createVariableRangeEvent(Mod.loc("idle")));
    public static final RegistryObject<SoundEvent> HENG = REGISTRY.register("heng", () -> SoundEvent.createVariableRangeEvent(Mod.loc("heng")));

    public static final RegistryObject<SoundEvent> LAND = REGISTRY.register("land", () -> SoundEvent.createVariableRangeEvent(Mod.loc("land")));
    public static final RegistryObject<SoundEvent> HIT_WATER = REGISTRY.register("hit_water", () -> SoundEvent.createVariableRangeEvent(Mod.loc("hit_water")));
    public static final RegistryObject<SoundEvent> HEADSHOT = REGISTRY.register("headshot", () -> SoundEvent.createVariableRangeEvent(Mod.loc("headshot")));

    public static final RegistryObject<SoundEvent> MORTAR_FIRE = REGISTRY.register("mortar_fire", () -> SoundEvent.createVariableRangeEvent(Mod.loc("mortar_fire")));

    public static final RegistryObject<SoundEvent> FIRE_RATE = REGISTRY.register("firerate", () -> SoundEvent.createVariableRangeEvent(Mod.loc("firerate")));

    public static final RegistryObject<SoundEvent> CANNON_ZOOM_IN = REGISTRY.register("cannon_zoom_in", () -> SoundEvent.createVariableRangeEvent(Mod.loc("cannon_zoom_in")));
    public static final RegistryObject<SoundEvent> CANNON_ZOOM_OUT = REGISTRY.register("cannon_zoom_out", () -> SoundEvent.createVariableRangeEvent(Mod.loc("cannon_zoom_out")));

    public static final RegistryObject<SoundEvent> BULLET_SUPPLY = REGISTRY.register("bullet_supply", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bullet_supply")));
    public static final RegistryObject<SoundEvent> ADJUST_FOV = REGISTRY.register("adjust_fov", () -> SoundEvent.createVariableRangeEvent(Mod.loc("adjust_fov")));
    public static final RegistryObject<SoundEvent> GRENADE_PULL = REGISTRY.register("grenade_pull", () -> SoundEvent.createVariableRangeEvent(Mod.loc("grenade_pull")));
    public static final RegistryObject<SoundEvent> GRENADE_THROW = REGISTRY.register("grenade_throw", () -> SoundEvent.createVariableRangeEvent(Mod.loc("grenade_throw")));

    public static final RegistryObject<SoundEvent> EDIT_MODE = REGISTRY.register("edit_mode", () -> SoundEvent.createVariableRangeEvent(Mod.loc("edit_mode")));
    public static final RegistryObject<SoundEvent> EDIT = REGISTRY.register("edit", () -> SoundEvent.createVariableRangeEvent(Mod.loc("edit")));
    public static final RegistryObject<SoundEvent> SHELL_CASING_NORMAL = REGISTRY.register("shell_casing_normal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("shell_casing_normal")));
    public static final RegistryObject<SoundEvent> SHELL_CASING_SHOTGUN = REGISTRY.register("shell_casing_shotgun", () -> SoundEvent.createVariableRangeEvent(Mod.loc("shell_casing_shotgun")));
    public static final RegistryObject<SoundEvent> SHELL_CASING_50CAL = REGISTRY.register("shell_casing_50cal", () -> SoundEvent.createVariableRangeEvent(Mod.loc("shell_casing_50cal")));
    public static final RegistryObject<SoundEvent> OPEN = REGISTRY.register("open", () -> SoundEvent.createVariableRangeEvent(Mod.loc("open")));
    public static final RegistryObject<SoundEvent> ANNIHILATOR_RELOAD = REGISTRY.register("annihilator_reload", () -> SoundEvent.createVariableRangeEvent(Mod.loc("annihilator_reload")));

    public static final RegistryObject<SoundEvent> RADAR_SEARCH_START = REGISTRY.register("radar_search_start", () -> SoundEvent.createVariableRangeEvent(Mod.loc("radar_search_start")));
    public static final RegistryObject<SoundEvent> RADAR_SEARCH_IDLE = REGISTRY.register("radar_search_idle", () -> SoundEvent.createVariableRangeEvent(Mod.loc("radar_search_idle")));
    public static final RegistryObject<SoundEvent> RADAR_SEARCH_END = REGISTRY.register("radar_search_end", () -> SoundEvent.createVariableRangeEvent(Mod.loc("radar_search_end")));

    public static final RegistryObject<SoundEvent> INTO_CANNON = REGISTRY.register("into_cannon", () -> SoundEvent.createVariableRangeEvent(Mod.loc("into_cannon")));
    public static final RegistryObject<SoundEvent> LOW_HEALTH = REGISTRY.register("low_health", () -> SoundEvent.createVariableRangeEvent(Mod.loc("low_health")));
    public static final RegistryObject<SoundEvent> NO_HEALTH = REGISTRY.register("no_health", () -> SoundEvent.createVariableRangeEvent(Mod.loc("no_health")));

    public static final RegistryObject<SoundEvent> LOCKING_WARNING = REGISTRY.register("locking_warning", () -> SoundEvent.createVariableRangeEvent(Mod.loc("locking_warning")));
    public static final RegistryObject<SoundEvent> LOCKED_WARNING = REGISTRY.register("locked_warning", () -> SoundEvent.createVariableRangeEvent(Mod.loc("locked_warning")));
    public static final RegistryObject<SoundEvent> MISSILE_WARNING = REGISTRY.register("missile_warning", () -> SoundEvent.createVariableRangeEvent(Mod.loc("missile_warning")));

    public static final RegistryObject<SoundEvent> LUNGE_MINE_GROWL = REGISTRY.register("lunge_mine_growl", () -> SoundEvent.createVariableRangeEvent(Mod.loc("lunge_mine_growl")));

    public static final RegistryObject<SoundEvent> TURRET_TURN = REGISTRY.register("turret_turn", () -> SoundEvent.createVariableRangeEvent(Mod.loc("turret_turn")));
    public static final RegistryObject<SoundEvent> C4_BEEP = REGISTRY.register("c4_beep", () -> SoundEvent.createVariableRangeEvent(Mod.loc("c4_beep")));
    public static final RegistryObject<SoundEvent> C4_FINAL = REGISTRY.register("c4_final", () -> SoundEvent.createVariableRangeEvent(Mod.loc("c4_final")));
    public static final RegistryObject<SoundEvent> C4_THROW = REGISTRY.register("c4_throw", () -> SoundEvent.createVariableRangeEvent(Mod.loc("c4_throw")));
    public static final RegistryObject<SoundEvent> C4_DETONATOR_CLICK = REGISTRY.register("c4_detonator_click", () -> SoundEvent.createVariableRangeEvent(Mod.loc("c4_detonator_click")));

    public static final RegistryObject<SoundEvent> SMOKE_FIRE = REGISTRY.register("smoke_fire", () -> SoundEvent.createVariableRangeEvent(Mod.loc("smoke_fire")));
    public static final RegistryObject<SoundEvent> ROCKET_FLY = REGISTRY.register("rocket_fly", () -> SoundEvent.createVariableRangeEvent(Mod.loc("rocket_fly")));
    public static final RegistryObject<SoundEvent> SHELL_FLY = REGISTRY.register("shell_fly", () -> SoundEvent.createVariableRangeEvent(Mod.loc("shell_fly")));
    public static final RegistryObject<SoundEvent> ROCKET_ENGINE = REGISTRY.register("rocket_engine", () -> SoundEvent.createVariableRangeEvent(Mod.loc("rocket_engine")));

    public static final RegistryObject<SoundEvent> BOMB_RELEASE = REGISTRY.register("bomb_release", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bomb_release")));
    public static final RegistryObject<SoundEvent> MISSILE_START = REGISTRY.register("missile_start", () -> SoundEvent.createVariableRangeEvent(Mod.loc("missile_start")));

    // Guns
    // Common Gun Sounds
    public static final RegistryObject<SoundEvent> OVERHEAT = REGISTRY.register("overheat", () -> SoundEvent.createVariableRangeEvent(Mod.loc("overheat")));

    // bocek
    public static final RegistryObject<SoundEvent> BOCEK_ZOOM_FIRE_1P = REGISTRY.register("bocek_zoom_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bocek_zoom_fire_1p")));
    public static final RegistryObject<SoundEvent> BOCEK_ZOOM_FIRE_3P = REGISTRY.register("bocek_zoom_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bocek_zoom_fire_3p")));
    public static final RegistryObject<SoundEvent> BOCEK_SHATTER_CAP_FIRE_1P = REGISTRY.register("bocek_shatter_cap_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bocek_shatter_cap_fire_1p")));
    public static final RegistryObject<SoundEvent> BOCEK_SHATTER_CAP_FIRE_3P = REGISTRY.register("bocek_shatter_cap_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bocek_shatter_cap_fire_3p")));
    public static final RegistryObject<SoundEvent> BOCEK_PULL_1P = REGISTRY.register("bocek_pull_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bocek_pull_1p")));
    public static final RegistryObject<SoundEvent> BOCEK_PULL_3P = REGISTRY.register("bocek_pull_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("bocek_pull_3p")));


    public static final RegistryObject<SoundEvent> IGLA_FIRE_1P = REGISTRY.register("igla_9k38_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("igla_9k38_fire_1p")));
    public static final RegistryObject<SoundEvent> IGLA_FIRE_3P = REGISTRY.register("igla_9k38_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("igla_9k38_fire_3p")));
    public static final RegistryObject<SoundEvent> IGLA_FAR = REGISTRY.register("igla_9k38_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("igla_9k38_far")));

    public static final RegistryObject<SoundEvent> JAVELIN_FIRE_1P = REGISTRY.register("javelin_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("javelin_fire_1p")));
    public static final RegistryObject<SoundEvent> JAVELIN_FIRE_3P = REGISTRY.register("javelin_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("javelin_fire_3p")));
    public static final RegistryObject<SoundEvent> JAVELIN_FAR = REGISTRY.register("javelin_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("javelin_far")));

    public static final RegistryObject<SoundEvent> MINIGUN_ROTATE = REGISTRY.register("minigun_rotate", () -> SoundEvent.createVariableRangeEvent(Mod.loc("minigun_rotate")));

    public static final RegistryObject<SoundEvent> QL_1031_CHARGE = REGISTRY.register("ql_1031_charge", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ql_1031_charge")));
    public static final RegistryObject<SoundEvent> REPAIRING = REGISTRY.register("repairing", () -> SoundEvent.createVariableRangeEvent(Mod.loc("repairing")));

    public static final RegistryObject<SoundEvent> RPG_FIRE_3P = REGISTRY.register("rpg_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("rpg_fire_3p")));

    public static final RegistryObject<SoundEvent> SECONDARY_CATACLYSM_FIRE_1P_CHARGE = REGISTRY.register("secondary_cataclysm_fire_1p_charge", () -> SoundEvent.createVariableRangeEvent(Mod.loc("secondary_cataclysm_fire_1p_charge")));
    public static final RegistryObject<SoundEvent> SECONDARY_CATACLYSM_FIRE_3P_CHARGE = REGISTRY.register("secondary_cataclysm_fire_3p_charge", () -> SoundEvent.createVariableRangeEvent(Mod.loc("secondary_cataclysm_fire_3p_charge")));
    public static final RegistryObject<SoundEvent> SECONDARY_CATACLYSM_FAR_CHARGE = REGISTRY.register("secondary_cataclysm_far_charge", () -> SoundEvent.createVariableRangeEvent(Mod.loc("secondary_cataclysm_far_charge")));
    public static final RegistryObject<SoundEvent> SECONDARY_CATACLYSM_VERYFAR_CHARGE = REGISTRY.register("secondary_cataclysm_veryfar_charge", () -> SoundEvent.createVariableRangeEvent(Mod.loc("secondary_cataclysm_veryfar_charge")));

    public static final RegistryObject<SoundEvent> SENTINEL_CHARGE_FIRE_1P = REGISTRY.register("sentinel_charge_fire_1p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sentinel_charge_fire_1p")));
    public static final RegistryObject<SoundEvent> SENTINEL_CHARGE_FIRE_3P = REGISTRY.register("sentinel_charge_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sentinel_charge_fire_3p")));
    public static final RegistryObject<SoundEvent> SENTINEL_CHARGE_FAR = REGISTRY.register("sentinel_charge_far", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sentinel_charge_far")));
    public static final RegistryObject<SoundEvent> SENTINEL_CHARGE_VERYFAR = REGISTRY.register("sentinel_charge_veryfar", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sentinel_charge_veryfar")));
    public static final RegistryObject<SoundEvent> SENTINEL_CHARGE = REGISTRY.register("sentinel_charge", () -> SoundEvent.createVariableRangeEvent(Mod.loc("sentinel_charge")));

    public static final RegistryObject<SoundEvent> STAR_RECOVER = REGISTRY.register("star_recover", () -> SoundEvent.createVariableRangeEvent(Mod.loc("star_recover")));

    // Vehicles
    // Common Vehicle Sounds
    public static final RegistryObject<SoundEvent> MISSILE_LOCKING = REGISTRY.register("missile_locking", () -> SoundEvent.createVariableRangeEvent(Mod.loc("missile_locking")));
    public static final RegistryObject<SoundEvent> MISSILE_LOCKED = REGISTRY.register("missile_locked", () -> SoundEvent.createVariableRangeEvent(Mod.loc("missile_locked")));

    public static final RegistryObject<SoundEvent> SMALL_ROCKET_FIRE_3P = REGISTRY.register("small_rocket_fire_3p", () -> SoundEvent.createVariableRangeEvent(Mod.loc("small_rocket_fire_3p")));
    public static final RegistryObject<SoundEvent> DECOY_RELEASE = REGISTRY.register("decoy_release", () -> SoundEvent.createVariableRangeEvent(Mod.loc("decoy_release")));
    public static final RegistryObject<SoundEvent> DECOY_RELEASE_FIRST = REGISTRY.register("decoy_release_first", () -> SoundEvent.createVariableRangeEvent(Mod.loc("decoy_release_first")));
    public static final RegistryObject<SoundEvent> DECOY_RELOAD = REGISTRY.register("decoy_reload", () -> SoundEvent.createVariableRangeEvent(Mod.loc("decoy_reload")));

    public static final RegistryObject<SoundEvent> WHEEL_VEHICLE_STEP = REGISTRY.register("wheel_vehicle_step", () -> SoundEvent.createVariableRangeEvent(Mod.loc("wheel_vehicle_step")));
    public static final RegistryObject<SoundEvent> TRACK_VEHICLE_STEP = REGISTRY.register("track_vehicle_step", () -> SoundEvent.createVariableRangeEvent(Mod.loc("track_vehicle_step")));
    public static final RegistryObject<SoundEvent> VEHICLE_SWIM = REGISTRY.register("vehicle_swim", () -> SoundEvent.createVariableRangeEvent(Mod.loc("vehicle_swim")));
    public static final RegistryObject<SoundEvent> VEHICLE_STRIKE = REGISTRY.register("vehicle_strike", () -> SoundEvent.createVariableRangeEvent(Mod.loc("vehicle_strike")));


    // drone
    public static final RegistryObject<SoundEvent> DRONE_ENGINE = REGISTRY.register("drone_engine", () -> SoundEvent.createVariableRangeEvent(Mod.loc("drone_engine")));


    public static final RegistryObject<SoundEvent> WHEEL_CHAIR_JUMP = REGISTRY.register("wheel_chair_jump", () -> SoundEvent.createVariableRangeEvent(Mod.loc("wheel_chair_jump")));



    public static final RegistryObject<SoundEvent> DPS_GENERATOR_EVOLVE = REGISTRY.register("dps_generator_evolve", () -> SoundEvent.createVariableRangeEvent(Mod.loc("dps_generator_evolve")));
    public static final RegistryObject<SoundEvent> STEEL_PIPE_HIT = REGISTRY.register("steel_pipe_hit", () -> SoundEvent.createVariableRangeEvent(Mod.loc("steel_pipe_hit")));
    public static final RegistryObject<SoundEvent> STEEL_PIPE_DROP = REGISTRY.register("steel_pipe_drop", () -> SoundEvent.createVariableRangeEvent(Mod.loc("steel_pipe_drop")));
    public static final RegistryObject<SoundEvent> SM0KE_GRENADE_RELEASE = REGISTRY.register("smoke_grenade_release", () -> SoundEvent.createVariableRangeEvent(Mod.loc("smoke_grenade_release")));

    public static final RegistryObject<SoundEvent> HAND_WHEEL_ROT = REGISTRY.register("hand_wheel_rot", () -> SoundEvent.createVariableRangeEvent(Mod.loc("hand_wheel_rot")));
    public static final RegistryObject<SoundEvent> MEDIUM_ROCKET_FIRE = REGISTRY.register("medium_rocket_fire", () -> SoundEvent.createVariableRangeEvent(Mod.loc("medium_rocket_fire")));
    public static final RegistryObject<SoundEvent> TYPE_63_RELOAD = REGISTRY.register("ty63_reload", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ty63_reload")));

    public static final RegistryObject<SoundEvent> PARACHUTE_OPEN = REGISTRY.register("parachute_open", () -> SoundEvent.createVariableRangeEvent(Mod.loc("parachute_open")));
    public static final RegistryObject<SoundEvent> PARACHUTE_CLOSE = REGISTRY.register("parachute_close", () -> SoundEvent.createVariableRangeEvent(Mod.loc("parachute_close")));

    public static final RegistryObject<SoundEvent> PTKM_1R_DEPLOY = REGISTRY.register("ptkm_1r_deploy", () -> SoundEvent.createVariableRangeEvent(Mod.loc("ptkm_1r_deploy")));

    public static final RegistryObject<SoundEvent> TERRAIN = REGISTRY.register("terrain", () -> SoundEvent.createVariableRangeEvent(Mod.loc("terrain")));
    public static final RegistryObject<SoundEvent> PULL_UP = REGISTRY.register("pull_up", () -> SoundEvent.createVariableRangeEvent(Mod.loc("pull_up")));
}

