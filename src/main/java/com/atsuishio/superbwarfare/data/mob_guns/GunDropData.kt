package com.atsuishio.superbwarfare.data.mob_guns

import com.atsuishio.superbwarfare.config.server.SpawnConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.util.Mth

/**
 * 一条持枪策略（[GunSpawnData]）的掉落规则。
 *
 * 每个字段都可以不写：不写就退回**服务器配置**（[SpawnConfig]）或内置默认值，
 * 因此数据包只需要覆盖它真正关心的那一项。
 *
 * ```jsonc
 * "Drop": {
 *   "Chance": 0.25,           // 掉率，0 表示这条策略永不掉枪
 *   "PlayerKillOnly": false,  // false = 任何死法都掉（默认 true，与原版装备掉落一致）
 *   "ClearAmmo": true,        // 掉落时清空弹匣与溢出备弹，掉出一把空枪
 *   "StripOverride": true     // 掉落时剥掉本策略的 Override（生物平衡参数不外流）
 * }
 * ```
 */
@Serializable
data class GunDropData(
    /** 掉率；不写则用服务器配置 `mob_gun_drop_chance` */
    @JvmField
    @SerialName("Chance")
    val chance: Double? = null,

    /** 是否必须由玩家击杀才掉落；不写则默认 true */
    @JvmField
    @SerialName("PlayerKillOnly")
    val playerKillOnly: Boolean? = null,

    /** 掉落时是否清空弹药（弹匣 + 溢出备弹）；不写则用服务器配置 `mob_gun_drop_clear_ammo` */
    @JvmField
    @SerialName("ClearAmmo")
    val clearAmmo: Boolean? = null,

    /**
     * 掉落时是否剥掉 [GunSpawnData.override]。
     *
     * 那些覆写通常是防止生物一枪秒人（例如 `Damage: 1`），跟着掉落物流到玩家手里
     * 会变成一把"永久被削弱的枪"；需要的话可以在这里剥掉。不写则默认 false（保留）。
     */
    @JvmField
    @SerialName("StripOverride")
    val stripOverride: Boolean? = null,
)

/** 生效掉率：单条策略 > 服务器配置，并夹到 0..1 */
val GunDropData?.effectiveChance: Double
    get() {
        val drop = this ?: return Mth.clamp(SpawnConfig.MOB_GUN_DROP_CHANCE.get(), 0.0, 1.0)
        return Mth.clamp(drop.chance ?: SpawnConfig.MOB_GUN_DROP_CHANCE.get(), 0.0, 1.0)
    }

/** 是否必须玩家击杀才掉落 */
val GunDropData?.effectivePlayerKillOnly: Boolean
    get() {
        val drop = this ?: return true
        return drop.playerKillOnly ?: true
    }

/** 掉落时是否清空弹药 */
val GunDropData?.effectiveClearAmmo: Boolean
    get() {
        val drop = this ?: return SpawnConfig.MOB_GUN_DROP_CLEAR_AMMO.get()
        return drop.clearAmmo ?: SpawnConfig.MOB_GUN_DROP_CLEAR_AMMO.get()
    }

/** 掉落时是否剥掉生物平衡用的属性覆写 */
val GunDropData?.effectiveStripOverride: Boolean
    get() {
        val drop = this ?: return false
        return drop.stripOverride ?: false
    }
