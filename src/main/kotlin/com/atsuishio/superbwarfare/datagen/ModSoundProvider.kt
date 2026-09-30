package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.datagen.ModSoundProvider.Companion.subKey
import net.minecraft.data.PackOutput
import net.minecraftforge.common.data.ExistingFileHelper
import net.minecraftforge.common.data.SoundDefinition
import net.minecraftforge.common.data.SoundDefinitionsProvider

/**
 * 生成 assets/superbwarfare/sounds.json。
 *
 * 绝大多数音效用 [group] / [root] 一行带过（事件名和文件名相同），只有下面几种情况才需要 [register]：
 *  - 事件名和文件名不一致（例如 trachelium_reload_empty 用的是 trachelium_reload.ogg）；
 *  - 一个事件里随机播放多个音频文件；
 *  - 需要额外设置 stream / attenuationDistance / volume。
 *
 * 加新音频：把 .ogg 放进 src/main/resources/assets/superbwarfare/sounds/ 的对应目录，
 * 在下面补一行，然后跑 runData（Gradle -> Tasks -> forgegradle -> runData）。
 * 路径写错的话 datagen 会直接因为找不到 .ogg 报错，不会静默生成坏数据。
 *
 * 字幕翻译键统一是 subtitle.superbwarfare.<名字>，不传 subtitle 的音效不会显示字幕。
 */
class ModSoundProvider(output: PackOutput, existingFileHelper: ExistingFileHelper) :
    SoundDefinitionsProvider(output, Mod.MODID, existingFileHelper) {

    override fun registerSounds() {
        // ==================== 杂项 ====================
        // -------------------- sounds 根目录 --------------------
        root(
            "bullet_supply", "shock", "electric", "trigger_click", "targetdown", "indication", "indication_vehicle",
            "jump", "doublejump",
        )
        register("ouch", subtitle = subKey("ouch"))
        register("step", "step1", "step2", "step3", "step4")
        register("growl", subtitle = subKey("growl"))
        register("idle", "heng", "hengheng", "yarimasune", subtitle = subKey("idle"))
        root(
            "headshot", "mortar_fire", "mortar_distant", "mortar_load", "firerate", "adjust_fov", "grenade_throw",
            "grenade_pull",
        )
        register("heng", subtitle = subKey("heng"))
        root(
            "edit_mode", "edit", "open", "into_cannon", "into_missile", "missile_reload", "lunge_mine_growl",
            "laser_tower_shoot", "turret_turn", "smoke_fire",
        )
        register("rocket_fly") { attenuationDistance(128) }
        register("shell_fly") { attenuationDistance(96) }
        register("rocket_engine") { attenuationDistance(128) }
        root("bomb_release", "missile_start", "bomb_reload")
        register("dps_generator_evolve") { attenuationDistance(16).volume(0.7f) }
        register("melee_hit", "melee_hit_01", "melee_hit_02", "melee_hit_03", "melee_hit_04", "melee_hit_05")
        register("bayonet_hit", "bayonet_hit_1", "bayonet_hit_2", "bayonet_hit_3")
        root("steel_pipe_hit", "steel_pipe_drop", "smoke_grenade_release", "ptkm_1r_deploy", "night_vision_activate")
        register("car_horn") { stream(true).attenuationDistance(96) }
        register("steel_coil_move") { attenuationDistance(48) }

        // -------------------- annihilator --------------------
        group(
            "annihilator", "annihilator_fire_1p", "annihilator_fire_3p", "annihilator_far", "annihilator_veryfar",
            "annihilator_reload",
        )

        // -------------------- bl_132 --------------------
        register("bl_132_fire_1p", "bl_132/bl_132_fire_1p_01", "bl_132/bl_132_fire_1p_02", "bl_132/bl_132_fire_1p_03")
        register("bl_132_fire_3p", "bl_132/bl_132_fire_3p_01", "bl_132/bl_132_fire_3p_02", "bl_132/bl_132_fire_3p_03")
        register("bl_132_reload", "bl_132/bl_132_reload_01", "bl_132/bl_132_reload_02", "bl_132/bl_132_reload_03")

        // -------------------- bullet --------------------
        register("hit", "bullet/metal_01", "bullet/metal_02", "bullet/metal_03", "bullet/metal_04", "bullet/metal_05")
        register(
            "land", "bullet/bullet1", "bullet/bullet2", "bullet/bullet3", "bullet/bullet4", "bullet/bullet5",
            "bullet/bullet6", "bullet/bullet7", "bullet/bullet8", "bullet/bullet9", "bullet/bullet10",
            "bullet/bullet11", "bullet/bullet12", "bullet/bullet13", "bullet/bullet14", "bullet/bullet15",
            "bullet/bullet16",
        )
        register(
            "hit_water", "bullet/water_01", "bullet/water_02", "bullet/water_03", "bullet/water_04",
            "bullet/water_05", "bullet/water_06",
        )

        // -------------------- c4 --------------------
        group("c4", "c4_beep", "c4_final", "c4_throw", "c4_detonator_click")

        // -------------------- cannon --------------------
        register("cannon_reload", "cannon/cannon_reload_01", "cannon/cannon_reload_02", "cannon/cannon_reload_03")
        group("cannon", "cannon_zoom_in", "cannon_zoom_out")

        // -------------------- explosion --------------------
        register(
            "explosion_close", "explosion/explosion_close", "explosion/explosion_close2",
            "explosion/explosion_close3", subtitle = EXPLOSION_SUBTITLE,
        )
        register(
            "explosion_far", "explosion/explosion_far", "explosion/explosion_far2", "explosion/explosion_far3",
            subtitle = EXPLOSION_SUBTITLE,
        )
        register(
            "explosion_very_far", "explosion/explosion_very_far", "explosion/explosion_very_far2",
            subtitle = EXPLOSION_SUBTITLE,
        )
        register(
            "explosion_water", "explosion/explosion_water", "explosion/explosion_water2",
            "explosion/explosion_water3", subtitle = EXPLOSION_SUBTITLE,
        )
        register(
            "explosion_air", "explosion/air_explosion_01", "explosion/air_explosion_02",
            "explosion/air_explosion_03", "explosion/air_explosion_04", subtitle = EXPLOSION_SUBTITLE,
        )
        register(
            "huge_explosion_close", "explosion/huge_explosion_close", "explosion/huge_explosion_close2",
            subtitle = EXPLOSION_SUBTITLE,
        )
        register(
            "huge_explosion_far", "explosion/huge_explosion_far", "explosion/huge_explosion_far2",
            "explosion/huge_explosion_far3", subtitle = EXPLOSION_SUBTITLE,
        )
        register(
            "huge_explosion_very_far", "explosion/huge_explosion_very_far", "explosion/huge_explosion_very_far2",
            subtitle = EXPLOSION_SUBTITLE,
        )
        register(
            "epic_explosion_close", "explosion/epic_explosion_close_01", "explosion/epic_explosion_close_02",
            "explosion/epic_explosion_close_03", subtitle = EXPLOSION_SUBTITLE,
        )
        register(
            "epic_explosion_far", "explosion/epic_explosion_far_01", "explosion/epic_explosion_far_02",
            "explosion/epic_explosion_far_03", subtitle = EXPLOSION_SUBTITLE,
        )
        register(
            "epic_explosion_very_far", "explosion/epic_explosion_very_far_01",
            "explosion/epic_explosion_very_far_02", "explosion/epic_explosion_very_far_03",
            subtitle = EXPLOSION_SUBTITLE,
        )
        register(
            "mini_explosion", "explosion/mini_explosion_01", "explosion/mini_explosion_02",
            "explosion/mini_explosion_03", "explosion/mini_explosion_04", "explosion/mini_explosion_05",
            subtitle = EXPLOSION_SUBTITLE,
        )

        // -------------------- gpws --------------------
        group("gpws", "pull_up", "sink_rate", "terrain", "terrain_ahead", "too_low_gear", "too_low_terrain")

        // -------------------- hpj11 --------------------
        register("hpj_11_fire", "hpj11/hpj_11_fire") { stream(true).attenuationDistance(384) }

        // -------------------- laowu --------------------
        register("laowu", "laowu/laowu_1", "laowu/laowu_2", "laowu/laowu_3")
        register("laowu_single", "laowu/laowu_1s", "laowu/laowu_2s", "laowu/laowu_3s")

        // -------------------- miya --------------------
        register("miya", "miya/a", "miya/mi", "miya/nya", "miya/ya")

        // -------------------- mk_42 --------------------
        register(
            "mk_42_fire_1p", "mk_42/mk_42_fire_1p_1", "mk_42/mk_42_fire_1p_2", "mk_42/mk_42_fire_1p_3",
            "mk_42/mk_42_fire_1p_4",
        )
        group("mk_42", "mk_42_far", "mk_42_veryfar", "mk_42_fire_3p")

        // -------------------- parachute --------------------
        group("parachute", "parachute_open", "parachute_close")

        // -------------------- radar --------------------
        group("radar", "radar_search_start", "radar_search_idle", "radar_search_end")

        // -------------------- shells --------------------
        register(
            "shell_casing_normal", "shells/shell_casing_normal_1", "shells/shell_casing_normal_2",
            "shells/shell_casing_normal_3", "shells/shell_casing_normal_4", "shells/shell_casing_normal_5",
        )
        register(
            "shell_casing_shotgun", "shells/shell_casing_shotgun_1", "shells/shell_casing_shotgun_2",
            "shells/shell_casing_shotgun_3", "shells/shell_casing_shotgun_4", "shells/shell_casing_shotgun_5",
        )
        register(
            "shell_casing_50cal", "shells/shell_casing_50cal_1", "shells/shell_casing_50cal_2",
            "shells/shell_casing_50cal_3", "shells/shell_casing_50cal_4", "shells/shell_casing_50cal_5",
        )

        // -------------------- super_star --------------------
        register(
            "knife_flesh", "super_star/knife_flesh_01", "super_star/knife_flesh_02", "super_star/knife_flesh_03",
            "super_star/knife_flesh_04", "super_star/knife_flesh_05",
        )

        // -------------------- type63 --------------------
        register("hand_wheel_rot", "type63/rot1") { stream(true) }
        register("medium_rocket_fire", "type63/fire_1", "type63/fire_2", "type63/fire_3", "type63/fire_4")
        group("type63", "ty63_reload")

        // -------------------- warning --------------------
        group("warning", "low_health", "no_health")
        register("locking_warning", "warning/locking")
        register("locked_warning", "warning/locked")
        register("missile_warning", "warning/missile")

        // -------------------- waveforce_tower --------------------
        register(
            "waveforce_tower_fire", "waveforce_tower/waveforce_tower_fire_1",
            "waveforce_tower/waveforce_tower_fire_2", "waveforce_tower/waveforce_tower_fire_3",
            "waveforce_tower/waveforce_tower_fire_4",
        )

        // ==================== 枪械 ====================
        // -------------------- gun/common --------------------
        group("gun/common", "overheat", "common_grab_1", "common_grab_2", "common_grab_3")
        register(
            "reflections", "gun/common/reflections_1", "gun/common/reflections_2", "gun/common/reflections_3",
            "gun/common/reflections_4",
        )

        // -------------------- gun/aa_12 --------------------
        group(
            "gun/aa_12", "aa_12_fire_1p", "aa_12_fire_3p", "aa_12_fire_1p_s", "aa_12_fire_3p_s", "aa_12_far",
            "aa_12_veryfar", "aa_12_mag_out", "aa_12_mag_in", "aa_12_bolt_open", "aa_12_bolt_close",
        )

        // -------------------- gun/ak_12 --------------------
        group(
            "gun/ak_12", "ak_12_fire_1p", "ak_12_fire_3p", "ak_12_fire_1p_s", "ak_12_fire_3p_s", "ak_12_far",
            "ak_12_veryfar", "ak_12_mag_in", "ak_12_mag_out",
        )
        register("ak_12_mag_out_normal", "gun/ak_12/ak_12_mag_out")
        group("gun/ak_12", "ak_12_bolt", "ak_12_bolt_open", "ak_12_bolt_close")

        // -------------------- gun/ak_47 --------------------
        group(
            "gun/ak_47", "ak_47_fire_1p", "ak_47_fire_1p_s", "ak_47_fire_3p", "ak_47_fire_3p_s", "ak_47_far",
            "ak_47_veryfar", "ak_47_mag_in", "ak_47_mag_out", "ak_47_bolt",
        )

        // -------------------- gun/aurelia_sceptre --------------------
        group("gun/aurelia_sceptre", "aurelia_sceptre_fire_1p", "aurelia_sceptre_fire_3p")

        // -------------------- gun/awm --------------------
        group(
            "gun/awm", "awm_fire_1p", "awm_fire_3p", "awm_fire_1p_s", "awm_fire_3p_s", "awm_far", "awm_veryfar",
            "awm_bolt_open", "awm_bolt_close", "awm_mag_in", "awm_mag_out",
        )

        // -------------------- gun/bocek --------------------
        group(
            "gun/bocek", "bocek_zoom_fire_1p", "bocek_zoom_fire_3p", "bocek_shatter_cap_fire_1p",
            "bocek_shatter_cap_fire_3p", "bocek_pull_1p", "bocek_pull_3p",
        )

        // -------------------- gun/devotion --------------------
        group(
            "gun/devotion", "devotion_fire_1p", "devotion_fire_3p", "devotion_fire_1p_s", "devotion_fire_3p_s",
            "devotion_far", "devotion_veryfar", "devotion_mag_in", "devotion_mag_in_2", "devotion_mag_out",
            "devotion_bolt",
        )

        // -------------------- gun/glock_17 --------------------
        group(
            "gun/glock_17", "glock_17_fire_1p", "glock_17_fire_3p", "glock_17_fire_1p_s", "glock_17_fire_3p_s",
            "glock_17_far", "glock_17_veryfar", "glock_17_mag_in", "glock_17_mag_out", "glock_17_slide",
        )

        // -------------------- gun/gp_25 --------------------
        group("gun/gp_25", "gp_25_fire_1p", "gp_25_fire_3p", "gp_25_reload_1", "gp_25_reload_2")

        // -------------------- gun/hk_416 --------------------
        group(
            "gun/hk_416", "hk_416_fire_1p", "hk_416_fire_1p_s", "hk_416_fire_3p", "hk_416_fire_3p_s", "hk_416_far",
            "hk_416_veryfar", "hk_416_reload_normal", "hk_416_reload_empty",
        )

        // -------------------- gun/homemade_shotgun --------------------
        group(
            "gun/homemade_shotgun", "homemade_shotgun_fire_1p", "homemade_shotgun_fire_3p", "homemade_shotgun_far",
            "homemade_shotgun_veryfar", "homemade_shotgun_bullet_in", "homemade_shotgun_bullet_in_2",
            "homemade_shotgun_stick_in",
        )

        // -------------------- gun/hunting_rifle --------------------
        group(
            "gun/hunting_rifle", "hunting_rifle_fire_1p", "hunting_rifle_fire_3p", "hunting_rifle_fire_1p_s",
            "hunting_rifle_fire_3p_s", "hunting_rifle_far", "hunting_rifle_veryfar", "hunting_rifle_bolt_open",
            "hunting_rifle_bolt_close", "hunting_rifle_bullet_in",
        )

        // -------------------- gun/igla_9k38 --------------------
        group("gun/igla_9k38", "igla_9k38_fire_1p", "igla_9k38_fire_3p", "igla_9k38_far")
        register("igla_9k38_reload_empty", "gun/igla_9k38/igla_9k38_reload")
        group("gun/igla_9k38", "igla_9k38_locking", "igla_9k38_locked")

        // -------------------- gun/insidious --------------------
        group("gun/insidious", "insidious_fire_1p", "insidious_fire_3p", "insidious_far", "insidious_veryfar")
        register("insidious_reload_empty", "gun/insidious/insidious_reload")

        // -------------------- gun/javelin --------------------
        group("gun/javelin", "javelin_fire_1p", "javelin_fire_3p", "javelin_far")
        register("javelin_reload_empty", "gun/javelin/javelin_reload")
        group("gun/javelin", "javelin_locking", "javelin_locked")

        // -------------------- gun/k_98 --------------------
        group(
            "gun/k_98", "k_98_fire_1p", "k_98_fire_3p", "k_98_fire_1p_s", "k_98_fire_3p_s", "k_98_far",
            "k_98_veryfar", "k_98_loop", "k_98_bolt_close", "k_98_bolt_open", "k_98_clip", "k_98_clip_out",
            "k_98_first_bullet_in",
        )

        // -------------------- gun/m_1897 --------------------
        group(
            "gun/m_1897", "m_1897_bolt_close", "m_1897_bolt_open", "m_1897_fire_1p", "m_1897_fire_3p",
            "m_1897_reload_cloth", "m_1897_reload_empty_start", "m_1897_reload_end_rotate",
            "m_1897_reload_end_shoulder", "m_1897_reload_loop", "m_1897_reload_start", "m_1897_shell_impact",
        )

        // -------------------- gun/m_1911 --------------------
        group(
            "gun/m_1911", "m_1911_fire_1p", "m_1911_fire_3p", "m_1911_fire_1p_s", "m_1911_fire_3p_s", "m_1911_far",
            "m_1911_veryfar",
        )

        // -------------------- gun/m_2_hb --------------------
        group(
            "gun/m_2_hb", "m_2_hb_fire_1p", "m_2_hb_fire_3p", "m_2_hb_far", "m_2_hb_veryfar", "m_2_hb_mag_in",
            "m_2_hb_mag_out", "m_2_hb_open", "m_2_hb_close", "m_2_hb_bullet_in", "m_2_hb_bolt",
        )

        // -------------------- gun/m_4 --------------------
        group(
            "gun/m_4", "m_4_fire_1p", "m_4_fire_1p_s", "m_4_fire_3p", "m_4_fire_3p_s", "m_4_far", "m_4_veryfar",
            "m_4_mag_in", "m_4_mag_out", "m_4_bolt",
        )

        // -------------------- gun/m_60 --------------------
        group(
            "gun/m_60", "m_60_fire_1p", "m_60_fire_3p", "m_60_far", "m_60_veryfar", "m_60_reload_normal",
            "m_60_reload_empty",
        )

        // -------------------- gun/m_79 --------------------
        group(
            "gun/m_79", "m_79_fire_1p", "m_79_fire_3p", "m_79_far", "m_79_veryfar", "m_79_ammo_out", "m_79_ammo_in",
            "m_79_open", "m_79_close",
        )

        // -------------------- gun/m_870 --------------------
        group(
            "gun/m_870", "m_870_fire_1p", "m_870_fire_3p", "m_870_far", "m_870_veryfar", "m_870_fire_1p_s",
            "m_870_fire_3p_s", "m_870_bolt_open", "m_870_bolt_close", "m_870_reload_loop",
        )

        // -------------------- gun/m_98b --------------------
        group(
            "gun/m_98b", "m_98b_fire_1p", "m_98b_fire_3p", "m_98b_fire_1p_s", "m_98b_fire_3p_s", "m_98b_far",
            "m_98b_veryfar", "m_98b_mag_out", "m_98b_mag_in", "m_98b_bolt_open", "m_98b_bolt_close",
        )

        // -------------------- gun/marlin --------------------
        group(
            "gun/marlin", "marlin_fire_1p", "marlin_fire_3p", "marlin_fire_1p_s", "marlin_fire_3p_s", "marlin_far",
            "marlin_veryfar", "marlin_loop", "marlin_prepare", "marlin_bolt",
        )

        // -------------------- gun/minigun --------------------
        group("gun/minigun", "minigun_fire_1p", "minigun_fire_3p", "minigun_far", "minigun_veryfar", "minigun_rotate")

        // -------------------- gun/mk_14 --------------------
        group(
            "gun/mk_14", "mk_14_fire_1p", "mk_14_fire_3p", "mk_14_far", "mk_14_veryfar", "mk_14_fire_1p_s",
            "mk_14_fire_3p_s", "mk_14_mag_in", "mk_14_mag_out", "mk_14_bolt",
        )

        // -------------------- gun/mosin_nagant --------------------
        group(
            "gun/mosin_nagant", "mosin_nagant_fire_1p", "mosin_nagant_fire_3p", "mosin_nagant_fire_1p_s",
            "mosin_nagant_fire_3p_s", "mosin_nagant_far", "mosin_nagant_veryfar", "mosin_nagant_bolt_open",
            "mosin_nagant_bolt_close",
        )

        // -------------------- gun/mp_443 --------------------
        group("gun/mp_443", "mp_443_fire_1p", "mp_443_fire_3p", "mp_443_fire_1p_s", "mp_443_fire_3p_s")

        // -------------------- gun/mp_5 --------------------
        group(
            "gun/mp_5", "mp_5_fire_1p", "mp_5_fire_3p", "mp_5_far", "mp_5_veryfar", "mp_5_fire_1p_s",
            "mp_5_fire_3p_s", "mp_5_mag_in", "mp_5_mag_out", "mp_5_bolt_open", "mp_5_bolt_close",
        )

        // -------------------- gun/ntw_20 --------------------
        group(
            "gun/ntw_20", "ntw_20_fire_1p", "ntw_20_fire_3p", "ntw_20_far", "ntw_20_veryfar", "ntw_20_mag_in",
            "ntw_20_mag_out", "ntw_20_bolt_open", "ntw_20_bolt_close", "ntw_20_action_back",
        )

        // -------------------- gun/qbz_191 --------------------
        group(
            "gun/qbz_191", "qbz_191_fire_1p", "qbz_191_fire_1p_s", "qbz_191_fire_3p", "qbz_191_fire_3p_s",
            "qbz_191_far", "qbz_191_veryfar", "qbz_191_mag_in", "qbz_191_mag_out", "qbz_191_bolt",
        )

        // -------------------- gun/qbz_95 --------------------
        group(
            "gun/qbz_95", "qbz_95_fire_1p", "qbz_95_fire_1p_s", "qbz_95_fire_3p", "qbz_95_fire_3p_s", "qbz_95_far",
            "qbz_95_veryfar", "qbz_95_mag_out", "qbz_95_mag_in", "qbz_95_bolt",
        )

        // -------------------- gun/ql_1031/auto --------------------
        register("ql_1031_fire_1p_auto", "gun/ql_1031/auto/ql_1031_fire_1p")
        register("ql_1031_fire_3p_auto", "gun/ql_1031/auto/ql_1031_fire_3p")
        register("ql_1031_fire_1p_s_auto", "gun/ql_1031/auto/ql_1031_fire_1p_s")
        register("ql_1031_fire_3p_s_auto", "gun/ql_1031/auto/ql_1031_fire_3p_s")
        register("ql_1031_far_auto", "gun/ql_1031/auto/ql_1031_far")
        register("ql_1031_veryfar_auto", "gun/ql_1031/auto/ql_1031_veryfar")

        // -------------------- gun/ql_1031/hold --------------------
        register("ql_1031_fire_1p_hold", "gun/ql_1031/hold/ql_1031_fire_1p")
        register("ql_1031_fire_3p_hold", "gun/ql_1031/hold/ql_1031_fire_3p")
        register("ql_1031_fire_1p_s_hold", "gun/ql_1031/hold/ql_1031_fire_1p_s")
        register("ql_1031_fire_3p_s_hold", "gun/ql_1031/hold/ql_1031_fire_3p_s")
        register("ql_1031_far_hold", "gun/ql_1031/hold/ql_1031_far")
        register("ql_1031_veryfar_hold", "gun/ql_1031/hold/ql_1031_veryfar")
        group("gun/ql_1031/hold", "ql_1031_charge", "ql_1031_discharge")

        // -------------------- gun/ql_1031/semi --------------------
        register("ql_1031_fire_1p_semi", "gun/ql_1031/semi/ql_1031_fire_1p")
        register("ql_1031_fire_3p_semi", "gun/ql_1031/semi/ql_1031_fire_3p")
        register("ql_1031_fire_1p_s_semi", "gun/ql_1031/semi/ql_1031_fire_1p_s")
        register("ql_1031_fire_3p_s_semi", "gun/ql_1031/semi/ql_1031_fire_3p_s")
        register("ql_1031_far_semi", "gun/ql_1031/semi/ql_1031_far")
        register("ql_1031_veryfar_semi", "gun/ql_1031/semi/ql_1031_veryfar")

        // -------------------- gun/raubtier --------------------
        group(
            "gun/raubtier", "raubtier_fire_1p", "raubtier_fire_3p", "raubtier_far", "raubtier_veryfar",
            "raubtier_bolt_close", "raubtier_bolt_open", "raubtier_fall", "raubtier_grab", "raubtier_rise",
            "raubtier_shell_in", "raubtier_shell_in_2",
        )

        // -------------------- gun/repair_tool --------------------
        group("gun/repair_tool", "repair_tool_fire_1p", "repair_tool_fire_3p", "repairing")

        // -------------------- gun/rpg --------------------
        group(
            "gun/rpg", "rpg_fire_1p", "rpg_fire_3p", "rpg_far", "rpg_veryfar", "rpg_in", "rpg_in_2", "rpg_in_3",
            "rpg_in_4",
        )
        // 音频文件名笔误，实际文件是 rpg_delpoy.ogg；动画里引用的也是 superbwarfare:rpg_delpoy
        register("rpg_deploy", "gun/rpg/rpg_delpoy")
        group("gun/rpg", "rpg_trigger")

        // -------------------- gun/rpk --------------------
        group("gun/rpk", "rpk_fire_1p", "rpk_fire_3p", "rpk_fire_1p_s", "rpk_fire_3p_s", "rpk_far", "rpk_veryfar")

        // -------------------- gun/secondary_cataclysm --------------------
        group(
            "gun/secondary_cataclysm", "secondary_cataclysm_fire_1p", "secondary_cataclysm_fire_3p",
            "secondary_cataclysm_far", "secondary_cataclysm_veryfar", "secondary_cataclysm_loop",
        )
        register("secondary_cataclysm_prepare_load", "gun/secondary_cataclysm/secondary_cataclysm_start")
        group("gun/secondary_cataclysm", "secondary_cataclysm_end")
        register("secondary_cataclysm_fire_1p_charge", "gun/secondary_cataclysm/secondary_cataclysm_charge_fire_1p")
        register("secondary_cataclysm_fire_3p_charge", "gun/secondary_cataclysm/secondary_cataclysm_charge_fire_3p")
        register("secondary_cataclysm_far_charge", "gun/secondary_cataclysm/secondary_cataclysm_charge_far")
        register("secondary_cataclysm_veryfar_charge", "gun/secondary_cataclysm/secondary_cataclysm_charge_veryfar")

        // -------------------- gun/sentinel --------------------
        group(
            "gun/sentinel", "sentinel_fire_1p", "sentinel_fire_3p", "sentinel_charge_fire_1p",
            "sentinel_charge_fire_3p", "sentinel_far", "sentinel_veryfar", "sentinel_charge_far",
            "sentinel_charge_veryfar", "sentinel_reload_normal", "sentinel_reload_empty", "sentinel_charge",
            "sentinel_bolt",
        )

        // -------------------- gun/sks --------------------
        group(
            "gun/sks", "sks_fire_1p", "sks_fire_3p", "sks_reload_normal", "sks_reload_empty", "sks_far",
            "sks_veryfar",
        )

        // -------------------- gun/super_star_shooter --------------------
        group(
            "gun/super_star_shooter", "star_shoot_1p", "star_shoot_3p", "star_recover",
            "super_star_shooter_reload_1", "super_star_shooter_reload_2", "super_star_shooter_reload_3",
        )

        // -------------------- gun/svd --------------------
        group(
            "gun/svd", "svd_fire_1p", "svd_fire_3p", "svd_far", "svd_veryfar", "svd_fire_1p_s", "svd_fire_3p_s",
            "svd_mag_in", "svd_mag_out", "svd_bolt",
        )

        // -------------------- gun/taser --------------------
        group("gun/taser", "taser_fire_1p", "taser_fire_3p", "taser_head_out", "taser_head_in")

        // -------------------- gun/trachelium --------------------
        group(
            "gun/trachelium", "trachelium_fire_1p", "trachelium_fire_3p", "trachelium_far", "trachelium_veryfar",
            "trachelium_fire_1p_s", "trachelium_fire_3p_s",
        )
        register("trachelium_reload_empty", "gun/trachelium/trachelium_reload")
        group("gun/trachelium", "trachelium_bolt")

        // -------------------- gun/vector --------------------
        group(
            "gun/vector", "vector_fire_1p", "vector_fire_3p", "vector_far", "vector_veryfar", "vector_fire_1p_s",
            "vector_fire_3p_s", "vector_reload_normal", "vector_reload_empty",
        )

        // ==================== 载具 ====================
        // -------------------- vehicle/common --------------------
        group(
            "vehicle/common", "vehicle_strike", "small_rocket_fire_1p", "small_rocket_fire_3p", "decoy_reload",
            "decoy_release_first", "decoy_release", "coax_fire_1p", "medium_missile_reload", "wheel_vehicle_step",
        )
        register("wheel_vehicle_skip", "vehicle/common/wheel_vehicle_skip") { attenuationDistance(32) }
        register("track_vehicle_skip", "vehicle/common/track_vehicle_skip") { attenuationDistance(32) }
        register("track_vehicle_step", "vehicle/common/track_vehicle_step") { attenuationDistance(32) }
        register("vehicle_swim", "vehicle/common/vehicle_swim") { stream(true).attenuationDistance(24) }
        group("vehicle/common", "missile_locking", "missile_locked")
        register("stuka", "vehicle/common/stuka") { stream(true).attenuationDistance(1024) }
        group("vehicle/common", "turret_burn_start")
        register(
            "turret_burn", "vehicle/common/turret_burn_01", "vehicle/common/turret_burn_02",
            "vehicle/common/turret_burn_03",
        )
        register("heli_crash", "vehicle/common/heli_crash") { stream(true).attenuationDistance(256) }

        // -------------------- vehicle/a_10a --------------------
        register("a_10a_engine", "vehicle/a_10a/a_10a_engine") { stream(true).attenuationDistance(768) }
        register("a_10a_engine_start", "vehicle/a_10a/a_10a_engine_start") { stream(true).attenuationDistance(256) }
        register("a_10a_fire", "vehicle/a_10a/a_10a_fire") { stream(true).attenuationDistance(512) }

        // -------------------- vehicle/ac_130h --------------------
        register("ac_130h_engine", "vehicle/ac_130h/ac_130h_engine") { stream(true).attenuationDistance(768) }
        register("ac_130h_engine_start", "vehicle/ac_130h/ac_130h_engine_start") {
            stream(true).attenuationDistance(128)
        }
        register(
            "bofos_fire_1p", "vehicle/ac_130h/bofos_fire_1p_1", "vehicle/ac_130h/bofos_fire_1p_2",
            "vehicle/ac_130h/bofos_fire_1p_3",
        )
        group(
            "vehicle/ac_130h", "bofos_fire_3p", "bofos_fire_far", "bofos_fire_veryfar", "m102_fire_1p",
            "m102_fire_3p",
        )

        // -------------------- vehicle/ah_6 --------------------
        group("vehicle/ah_6", "ah_6_engine_start")
        register("ah_6_engine", "vehicle/ah_6/ah_6_engine") { stream(true).attenuationDistance(384) }
        group("vehicle/ah_6", "ah_6_cannon_fire_1p", "ah_6_cannon_fire_3p", "ah_6_cannon_far", "ah_6_cannon_veryfar")

        // -------------------- vehicle/bmp_2 --------------------
        group("vehicle/bmp_2", "bmp_2_cannon_fire_1p", "bmp_2_cannon_fire_3p")
        register("bmp_2_engine", "vehicle/bmp_2/bmp_2_engine") { stream(true).attenuationDistance(64) }
        group("vehicle/bmp_2", "bmp_2_missile_fire_1p", "bmp_2_missile_fire_3p")

        // -------------------- vehicle/bradley --------------------
        group(
            "vehicle/bradley", "bradley_cannon_fire_1p", "bradley_cannon_fire_3p", "bradley_cannon_far",
            "bradley_cannon_veryfar",
        )
        register("bradley_engine", "vehicle/bradley/bradley_engine") { stream(true).attenuationDistance(64) }

        // -------------------- vehicle/drone --------------------
        register("drone_engine", "vehicle/drone/drone_engine") { attenuationDistance(48) }

        // -------------------- vehicle/fh_77bw --------------------
        group("vehicle/fh_77bw", "fh_77bw_fire_1p", "fh_77bw_fire_3p")

        // -------------------- vehicle/happiest_ghast --------------------
        group(
            "vehicle/happiest_ghast", "happiest_ghast_kalibr_missile_fire", "happiest_ghast_missile_fire",
            "happiest_ghast_door_open", "happiest_ghast_door_close",
        )
        register("happiest_ghast_engine", "vehicle/happiest_ghast/happiest_ghast_engine") {
            stream(true).attenuationDistance(24)
        }

        // -------------------- vehicle/ju_87 --------------------
        group("vehicle/ju_87", "mg_17_fire_1p", "mg_17_fire_1p_inside", "mg_17_fire_3p", "mg_17_far", "mg_17_veryfar")
        register("ju_87_engine", "vehicle/ju_87/ju_87_engine") { stream(true).attenuationDistance(512) }

        // -------------------- vehicle/kirov --------------------
        register("kirov_engine", "vehicle/kirov/kirov_engine") { stream(true).attenuationDistance(128) }

        // -------------------- vehicle/kv_16 --------------------
        register("kv_16_engine", "vehicle/kv_16/kv_16_engine") { stream(true).attenuationDistance(384) }
        register("kv_16_engine_start", "vehicle/kv_16/kv_16_engine_start") { stream(true).attenuationDistance(64) }

        // -------------------- vehicle/lav_150 --------------------
        group(
            "vehicle/lav_150", "lav_150_cannon_fire_1p", "lav_150_cannon_fire_3p", "lav_150_cannon_far",
            "lav_150_cannon_veryfar",
        )
        register("lav_150_engine", "vehicle/lav_150/lav_150_engine") { stream(true).attenuationDistance(64) }

        // -------------------- vehicle/lav_ad --------------------
        register("lav_ad_fire", "vehicle/lav_ad/lav_ad_fire") { stream(true).attenuationDistance(256) }

        // -------------------- vehicle/m_1a_2 --------------------
        group("vehicle/m_1a_2", "m_1a_2_reload", "m_1a_2_fire_1p", "m_1a_2_fire_3p", "m_1a_2_far", "m_1a_2_veryfar")

        // -------------------- vehicle/mi_28 --------------------
        group("vehicle/mi_28", "mi_28_engine_start")
        register("mi_28_engine", "vehicle/mi_28/mi_28_engine") { stream(true).attenuationDistance(384) }
        group("vehicle/mi_28", "mi_28_cannon_fire_1p", "mi_28_cannon_far", "mi_28_cannon_veryfar")

        // -------------------- vehicle/plz_05 --------------------
        group("vehicle/plz_05", "plz_05_reload", "plz_05_fire_1p", "plz_05_fire_3p", "plz_05_far", "plz_05_veryfar")
        register("plz_05_engine", "vehicle/plz_05/plz_05_engine") { stream(true).attenuationDistance(64) }

        // -------------------- vehicle/prism_tank --------------------
        group(
            "vehicle/prism_tank", "prism_tank_fire_high_1p", "prism_tank_fire_high_3p", "prism_tank_fire_low_1p",
            "prism_tank_fire_low_3p",
        )
        register("prism_tank_engine", "vehicle/prism_tank/prism_tank_engine") { stream(true).attenuationDistance(64) }

        // -------------------- vehicle/sodayo_pick_up --------------------
        register("sodayo_pick_up_engine", "vehicle/sodayo_pick_up/sodayo_pick_up_engine") {
            stream(true).attenuationDistance(48)
        }

        // -------------------- vehicle/speedboat --------------------
        register("speedboat_engine", "vehicle/speedboat/speedboat_engine") { attenuationDistance(48) }

        // -------------------- vehicle/t_90a --------------------
        group("vehicle/t_90a", "t_90a_reload", "t_90a_fire_1p", "t_90a_fire_3p", "t_90a_far", "t_90a_veryfar")
        register("t_90a_engine", "vehicle/t_90a/t_90a_engine") { stream(true).attenuationDistance(64) }

        // -------------------- vehicle/tiny_speedboat --------------------
        register("tiny_speedboat_engine", "vehicle/tiny_speedboat/tiny_speedboat_engine") { attenuationDistance(32) }

        // -------------------- vehicle/tom_6 --------------------
        group("vehicle/tom_6", "tom_6_engine")

        // -------------------- vehicle/truck --------------------
        register("truck_engine", "vehicle/truck/truck_engine") { stream(true).attenuationDistance(64) }
        register("truck_horn", "vehicle/truck/truck_horn") { stream(true).attenuationDistance(128) }

        // -------------------- vehicle/wheel_chair --------------------
        register("wheel_chair_engine", "vehicle/wheel_chair/wheel_chair_engine") { stream(true) }
        group("vehicle/wheel_chair", "wheel_chair_jump")

        // -------------------- vehicle/yx_100 --------------------
        group("vehicle/yx_100", "yx_100_reload", "yx_100_fire_1p", "yx_100_fire_3p", "yx_100_far", "yx_100_veryfar")
        register("yx_100_engine", "vehicle/yx_100/yx_100_engine") { stream(true).attenuationDistance(64) }
        group("vehicle/yx_100", "yx_100_swarm_drone_release")

        // -------------------- vehicle/ztz_99a --------------------
        group(
            "vehicle/ztz_99a", "ztz_99a_reload", "ztz_99a_fire_1p", "ztz_99a_fire_3p", "ztz_99a_far",
            "ztz_99a_veryfar",
        )
        register("ztz_99a_engine", "vehicle/ztz_99a/ztz_99a_engine") { stream(true).attenuationDistance(64) }
    }

    /** 批量注册同一目录下“事件名 == 文件名”的音效 */
    private fun group(dir: String, vararg names: String) = names.forEach { register(it, "$dir/$it") }

    /** 批量注册直接放在 sounds/ 根目录下的“事件名 == 文件名”音效 */
    private fun root(vararg names: String) = names.forEach { register(it) }

    /**
     * 注册一个音效事件。
     *
     * @param name 事件名，同时也是字幕键 [subKey] 和默认的音频路径
     * @param paths 音频路径（相对 assets/superbwarfare/sounds/，不带 .ogg），留空时用 [name]
     * @param subtitle 字幕翻译键，null 表示这个音效没有字幕
     * @param configure 对事件里每个音频文件的额外设置，例如 stream(true)、attenuationDistance(64)
     */
    private fun register(
        name: String,
        vararg paths: String,
        subtitle: String? = null,
        configure: SoundDefinition.Sound.() -> Unit = {}
    ) {
        val files = if (paths.isEmpty()) arrayOf(name) else paths
        val def = definition().with(*files.map { sound(loc(it)).apply(configure) }.toTypedArray())
        if (subtitle != null) def.subtitle(subtitle)
        add(name, def)
    }

    private companion object {
        /** 字幕翻译键：subtitle.superbwarfare.<名字> */
        fun subKey(name: String) = "subtitle.superbwarfare.$name"

        /** 所有 explosion 相关音效共用的字幕键，翻译就是“爆炸” */
        val EXPLOSION_SUBTITLE = subKey("explosion")
    }
}
