package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import net.minecraft.sounds.SoundEvent
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

@Suppress("unused")
object ModSounds {

    val REGISTRY: DeferredRegister<SoundEvent> = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Mod.MODID)

    // @formatter:off
    @JvmField val SHOCK = register("shock")
    @JvmField val ELECTRIC = register("electric")
    @JvmField val MELEE_HIT = register("melee_hit")

    @JvmField val TRIGGER_CLICK = register("trigger_click")
    @JvmField val HIT = register("hit")
    @JvmField val TARGET_DOWN = register("targetdown")
    @JvmField val INDICATION = register("indication")
    @JvmField val INDICATION_VEHICLE = register("indication_vehicle")
    @JvmField val JUMP = register("jump")
    @JvmField val DOUBLE_JUMP = register("doublejump")

    @JvmField val MINI_EXPLOSION = register("mini_explosion")
    @JvmField val EXPLOSION_CLOSE = register("explosion_close")
    @JvmField val EXPLOSION_FAR = register("explosion_far")
    @JvmField val EXPLOSION_VERY_FAR = register("explosion_very_far")
    @JvmField val HUGE_EXPLOSION_CLOSE = register("huge_explosion_close")
    @JvmField val HUGE_EXPLOSION_FAR = register("huge_explosion_far")
    @JvmField val HUGE_EXPLOSION_VERY_FAR = register("huge_explosion_very_far")
    @JvmField val EPIC_EXPLOSION_CLOSE = register("epic_explosion_close")
    @JvmField val EPIC_EXPLOSION_FAR = register("epic_explosion_far")
    @JvmField val EPIC_EXPLOSION_VERY_FAR = register("epic_explosion_very_far")
    @JvmField val EXPLOSION_WATER = register("explosion_water")
    @JvmField val EXPLOSION_AIR = register("explosion_air")

    @JvmField val OUCH = register("ouch")
    @JvmField val STEP = register("step")
    @JvmField val GROWL = register("growl")
    @JvmField val IDLE = register("idle")
    @JvmField val HENG = register("heng")

    @JvmField val STEEL_COIL_MOVE = register("steel_coil_move")

    @JvmField val LAND = register("land")
    @JvmField val HIT_WATER = register("hit_water")
    @JvmField val HEADSHOT = register("headshot")

    @JvmField val MORTAR_FIRE = register("mortar_fire")

    @JvmField val FIRE_RATE = register("firerate")

    @JvmField val CANNON_ZOOM_IN = register("cannon_zoom_in")
    @JvmField val CANNON_ZOOM_OUT = register("cannon_zoom_out")

    @JvmField val BULLET_SUPPLY = register("bullet_supply")
    @JvmField val ADJUST_FOV = register("adjust_fov")
    @JvmField val GRENADE_PULL = register("grenade_pull")
    @JvmField val GRENADE_THROW = register("grenade_throw")

    @JvmField val EDIT_MODE = register("edit_mode")
    @JvmField val EDIT = register("edit")
    @JvmField val SHELL_CASING_NORMAL = register("shell_casing_normal")
    @JvmField val SHELL_CASING_SHOTGUN = register("shell_casing_shotgun")
    @JvmField val SHELL_CASING_50CAL = register("shell_casing_50cal")
    @JvmField val OPEN = register("open")
    @JvmField val ANNIHILATOR_RELOAD = register("annihilator_reload")

    @JvmField val RADAR_SEARCH_START = register("radar_search_start")
    @JvmField val RADAR_SEARCH_IDLE = register("radar_search_idle")
    @JvmField val RADAR_SEARCH_END = register("radar_search_end")

    @JvmField val INTO_CANNON = register("into_cannon")
    @JvmField val LOW_HEALTH = register("low_health")
    @JvmField val NO_HEALTH = register("no_health")

    @JvmField val LOCKING_WARNING = register("locking_warning")
    @JvmField val LOCKED_WARNING = register("locked_warning")
    @JvmField val MISSILE_WARNING = register("missile_warning")

    @JvmField val LUNGE_MINE_GROWL = register("lunge_mine_growl")

    @JvmField val TURRET_TURN = register("turret_turn")
    @JvmField val C4_BEEP = register("c4_beep")
    @JvmField val C4_FINAL = register("c4_final")
    @JvmField val C4_THROW = register("c4_throw")
    @JvmField val C4_DETONATOR_CLICK = register("c4_detonator_click")

    @JvmField val SMOKE_FIRE = register("smoke_fire")
    @JvmField val ROCKET_FLY = register("rocket_fly")
    @JvmField val SHELL_FLY = register("shell_fly")
    @JvmField val ROCKET_ENGINE = register("rocket_engine")

    @JvmField val BOMB_RELEASE = register("bomb_release")
    @JvmField val MISSILE_START = register("missile_start")

    // Guns
    // Common Gun Sounds
    @JvmField val OVERHEAT = register("overheat")

    // bocek
    @JvmField val BOCEK_ZOOM_FIRE_1P = register("bocek_zoom_fire_1p")
    @JvmField val BOCEK_ZOOM_FIRE_3P = register("bocek_zoom_fire_3p")
    @JvmField val BOCEK_SHATTER_CAP_FIRE_1P = register("bocek_shatter_cap_fire_1p")
    @JvmField val BOCEK_SHATTER_CAP_FIRE_3P = register("bocek_shatter_cap_fire_3p")
    @JvmField val BOCEK_PULL_1P = register("bocek_pull_1p")
    @JvmField val BOCEK_PULL_3P = register("bocek_pull_3p")


    @JvmField val IGLA_FIRE_1P = register("igla_9k38_fire_1p")
    @JvmField val IGLA_FIRE_3P = register("igla_9k38_fire_3p")
    @JvmField val IGLA_FAR = register("igla_9k38_far")

    @JvmField val JAVELIN_FIRE_1P = register("javelin_fire_1p")
    @JvmField val JAVELIN_FIRE_3P = register("javelin_fire_3p")
    @JvmField val JAVELIN_FAR = register("javelin_far")

    @JvmField val MINIGUN_ROTATE = register("minigun_rotate")

    @JvmField val QL_1031_CHARGE = register("ql_1031_charge")
    @JvmField val REPAIRING = register("repairing")

    @JvmField val RPG_FIRE_3P = register("rpg_fire_3p")

    @JvmField val SECONDARY_CATACLYSM_FIRE_1P_CHARGE = register("secondary_cataclysm_fire_1p_charge")
    @JvmField val SECONDARY_CATACLYSM_FIRE_3P_CHARGE = register("secondary_cataclysm_fire_3p_charge")
    @JvmField val SECONDARY_CATACLYSM_FAR_CHARGE = register("secondary_cataclysm_far_charge")
    @JvmField val SECONDARY_CATACLYSM_VERYFAR_CHARGE = register("secondary_cataclysm_veryfar_charge")

    @JvmField val SENTINEL_CHARGE_FIRE_1P = register("sentinel_charge_fire_1p")
    @JvmField val SENTINEL_CHARGE_FIRE_3P = register("sentinel_charge_fire_3p")
    @JvmField val SENTINEL_CHARGE_FAR = register("sentinel_charge_far")
    @JvmField val SENTINEL_CHARGE_VERYFAR = register("sentinel_charge_veryfar")
    @JvmField val SENTINEL_CHARGE = register("sentinel_charge")

    @JvmField val STAR_RECOVER = register("star_recover")

    // Vehicles
    // Common Vehicle Sounds
    @JvmField val MISSILE_LOCKING = register("missile_locking")
    @JvmField val MISSILE_LOCKED = register("missile_locked")

    @JvmField val SMALL_ROCKET_FIRE_3P = register("small_rocket_fire_3p")
    @JvmField val DECOY_RELEASE = register("decoy_release")
    @JvmField val DECOY_RELEASE_FIRST = register("decoy_release_first")
    @JvmField val DECOY_RELOAD = register("decoy_reload")

    @JvmField val WHEEL_VEHICLE_STEP = register("wheel_vehicle_step")
    @JvmField val WHEEL_VEHICLE_SKIP = register("wheel_vehicle_skip")
    @JvmField val TRACK_VEHICLE_STEP = register("track_vehicle_step")
    @JvmField val TRACK_VEHICLE_SKIP = register("track_vehicle_skip")
    @JvmField val VEHICLE_SWIM = register("vehicle_swim")
    @JvmField val VEHICLE_STRIKE = register("vehicle_strike")
    @JvmField val TURRET_BURN_START = register("turret_burn_start")
    @JvmField val TURRET_BURN = register("turret_burn")
    @JvmField val HELI_CRASH = register("heli_crash")

    // drone
    @JvmField val DRONE_ENGINE = register("drone_engine")

    @JvmField val WHEEL_CHAIR_JUMP = register("wheel_chair_jump")

    @JvmField val DPS_GENERATOR_EVOLVE = register("dps_generator_evolve")
    @JvmField val STEEL_PIPE_HIT = register("steel_pipe_hit")
    @JvmField val STEEL_PIPE_DROP = register("steel_pipe_drop")
    @JvmField val SM0KE_GRENADE_RELEASE = register("smoke_grenade_release")

    @JvmField val HAND_WHEEL_ROT = register("hand_wheel_rot")
    @JvmField val MEDIUM_ROCKET_FIRE = register("medium_rocket_fire")
    @JvmField val TYPE_63_RELOAD = register("ty63_reload")

    @JvmField val PARACHUTE_OPEN = register("parachute_open")
    @JvmField val PARACHUTE_CLOSE = register("parachute_close")

    @JvmField val PTKM_1R_DEPLOY = register("ptkm_1r_deploy")

    @JvmField val KNIFE_FLESH = register("knife_flesh")
    @JvmField val NIGHT_VISION_ACTIVATE = register("night_vision_activate")

    @JvmField val STUKA = register("stuka")

    // GPWS

    @JvmField val GPWS_PULL_UP = register("pull_up")
    @JvmField val GPWS_SINK_RATE = register("sink_rate")
    @JvmField val GPWS_TERRAIN = register("terrain")
    @JvmField val GPWS_TERRAIN_AHEAD = register("terrain_ahead")
    @JvmField val GPWS_TOO_LOW_GEAR = register("too_low_gear")
    @JvmField val GPWS_TOO_LOW_TERRAIN = register("too_low_terrain")

    @JvmField val HAPPIEST_GHAST_DOOR_OPEN = register("happiest_ghast_door_open")
    @JvmField val HAPPIEST_GHAST_DOOR_CLOSE = register("happiest_ghast_door_close")
    // @formatter:on

    fun register(name: String): RegistryObject<SoundEvent> =
        REGISTRY.register(name) { SoundEvent.createVariableRangeEvent(Mod.loc(name)) }
}

