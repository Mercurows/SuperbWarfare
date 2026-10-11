package com.atsuishio.superbwarfare.physics

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType

/** An explicit allowlist; using our namespace does not authorize new IDs. */
object BuiltinVehicles {
    val ids: Set<String> = setOf(
        "superbwarfare:a_10a",
        "superbwarfare:ac_130h",
        "superbwarfare:ah_6",
        "superbwarfare:air_sheep",
        "superbwarfare:annihilator",
        "superbwarfare:bl_132",
        "superbwarfare:bmp_2",
        "superbwarfare:bradley",
        "superbwarfare:drone",
        "superbwarfare:fh_77bw",
        "superbwarfare:happiest_ghast",
        "superbwarfare:hpj_11",
        "superbwarfare:j_16",
        "superbwarfare:ju_87",
        "superbwarfare:kirov",
        "superbwarfare:kv_16",
        "superbwarfare:laser_tower",
        "superbwarfare:lav_150",
        "superbwarfare:lav_25",
        "superbwarfare:lav_ad",
        "superbwarfare:m_1a_2",
        "superbwarfare:mi_28",
        "superbwarfare:mk_42",
        "superbwarfare:mle_1934",
        "superbwarfare:mortar",
        "superbwarfare:plz_05",
        "superbwarfare:prism_tank",
        "superbwarfare:sodayo_pick_up",
        "superbwarfare:sodayo_pick_up_hmg",
        "superbwarfare:sodayo_pick_up_rocket",
        "superbwarfare:sodayo_pick_up_tow",
        "superbwarfare:speedboat",
        "superbwarfare:t_90a",
        "superbwarfare:tiny_speedboat",
        "superbwarfare:tom_6",
        "superbwarfare:tow",
        "superbwarfare:truck",
        "superbwarfare:type_63",
        "superbwarfare:vehicle_assembling_table",
        "superbwarfare:waveforce_tower",
        "superbwarfare:wheel_chair",
        "superbwarfare:yx_100",
        "superbwarfare:ztz_99a",
        "superbwarfare:turret_wreck"
    )

    /**
     * Physics is intentionally limited to registered SuperbWarfare vehicle types.
     * A datapack may replace the data for one of these IDs, but cannot opt a new
     * entity ID into the vehicle physics path merely by using our namespace.
     */
    fun contains(entity: Entity): Boolean =
        EntityType.getKey(entity.type)?.toString()?.let(ids::contains) == true
}
