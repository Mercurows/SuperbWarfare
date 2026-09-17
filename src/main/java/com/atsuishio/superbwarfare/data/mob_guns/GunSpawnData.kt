package com.atsuishio.superbwarfare.data.mob_guns

import com.atsuishio.superbwarfare.data.STOFactory
import com.atsuishio.superbwarfare.data.StringInstanceBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 一只生物使用枪械的配置（`sbw/mob_guns` 的 `Guns` 项）。
 *
 * 原来每个字段都靠 Gson 的 `UPPER_CAMEL_CASE` 策略映射键名，迁移到 kotlinx 之后
 * 必须**逐个显式写 [SerialName]**（kotlinx 没有命名策略）。
 */
@STOFactory(GunSpawnData.GunSpawnDataInstanceBuilder::class)
@Serializable
data class GunSpawnData(
    /** 枪械物品的注册 id，必须非空且是 GunItem */
    @JvmField
    @SerialName("ID")
    val id: String = "",

    /** 该枪的抽取权重 */
    @JvmField
    @SerialName("Weight")
    val weight: Int = 1,

    /** 开枪前瞄准时间（tick） */
    @JvmField
    @SerialName("AimTime")
    val aimTime: Int = 30,

    /** 丢失目标时是否清空瞄准进度 */
    @JvmField
    @SerialName("ClearAimTimeWhenLostSight")
    val clearAimTimeWhenLostSight: Boolean = true,

    /** 半自动/点射模式下额外冷却（微秒） */
    @JvmField
    @SerialName("SemiFireInterval")
    val semiFireInterval: Long = 500,

    /** 备弹数量 */
    @JvmField
    @SerialName("BackupAmmo")
    val backupAmmo: Int = 0,

    /** 生物生成时是否已上膛 */
    @JvmField
    @SerialName("SpawnWithLoadedAmmo")
    val spawnWithLoadedAmmo: Boolean = true,

    @JvmField
    @SerialName("ShootDistance")
    val shootDistance: Double = 30.0,

    /** 开枪时是否视为正在瞄准 */
    @JvmField
    @SerialName("Zoom")
    val zoom: Boolean = true,

    @JvmField
    @SerialName("Spread")
    val spread: Double = 1.0,

    /** 枪械物品的自定义 NBT 数据覆写 */
    @JvmField
    @SerialName("Data")
    val data: JsonObject? = null,

    /** 枪械属性覆写 */
    @JvmField
    @SerialName("Override")
    val override: JsonObject? = null,
) {
    object GunSpawnDataInstanceBuilder : StringInstanceBuilder<GunSpawnData> {
        override fun fromString(value: String) = GunSpawnData(id = value)
    }
}
