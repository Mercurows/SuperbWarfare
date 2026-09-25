package com.atsuishio.superbwarfare.data.mob_guns

import com.atsuishio.superbwarfare.data.StringInstanceBuilder
import com.atsuishio.superbwarfare.data.StringOrObjectFactory
import com.atsuishio.superbwarfare.item.gun.GunItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation

/**
 * 一只生物使用枪械的配置（`sbw/mob_guns` 的 `Guns` 项）。
 *
 * 原来每个字段都靠 Gson 的 `UPPER_CAMEL_CASE` 策略映射键名，迁移到 kotlinx 之后
 * 必须**逐个显式写 [SerialName]**（kotlinx 没有命名策略）。
 */
@StringOrObjectFactory(GunSpawnData.GunSpawnDataInstanceBuilder::class)
@Serializable
data class GunSpawnData(
    /** 枪械物品的注册 id，必须非空且是 GunItem */
    @JvmField
    @SerialName("ID")
    val id: String = "",

    /**
     * 这条配置的稳定标识，生物读档 / 传送 / `reload` 后靠它还原「抽中的是哪一条」。
     *
     * 同一个 `Guns` 列表里允许出现多条「同一种枪但属性不同」的配置，只凭手上的枪无法区分，
     * 因此需要显式身份。留空时自动生成为 `<枪 id>#<同 id 条目序号>`（从 0 开始）。
     */
    @JvmField
    @SerialName("Key")
    val key: String = "",

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

    /**
     * 生物的备弹数量。
     *
     * 它会被写进**生物身上的弹药池**（见 `MobGunState`），而不是枪的物品 NBT：
     * 生物死亡时池子随生物一起消失，掉落的枪只会剩下弹匣里那点弹药。
     * 池子作为「备弹」参与装填与开火消耗，不需要改枪械本身的弹种声明。
     *
     * **口径与枪的主弹药来源一致**：
     * - 有弹匣的枪（绝大多数）：写的是**发数**，例如 `BackupAmmo: 80` = 80 发；
     * - 没有弹匣的背包型武器：每发直接从池子里扣 `AmmoCostPerShoot`，所以量级要按它来。
     *   例如 `ql_1031` 每发 1000 FE，就要写 `100000` 才有 100 发；写 80 会一发都打不出来
     *   （这种情况启动时会打一条 warn 日志）；
     * - 附带**附加来源**的枪（例如泰瑟枪的 `400 fe`）由池子代付，池子里同样要留出这部分。
     */
    @JvmField
    @SerialName("BackupAmmo")
    val backupAmmo: Int = 0,

    /** 生物生成时是否已上膛（弹匣直接从备弹池里装填） */
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

    /**
     * 这条策略的掉落规则（掉率 / 是否必须玩家击杀 / 是否清空弹药 / 是否剥掉 Override）。
     * 不写则整条策略按服务器配置与内置默认值处理。
     */
    @JvmField
    @SerialName("Drop")
    val drop: GunDropData? = null,

    /** 枪械物品的自定义 NBT 数据覆写 */
    @JvmField
    @SerialName("Data")
    val data: JsonObject? = null,

    /**
     * 枪械属性覆写。生物持枪时会直接写在枪的物品 NBT 上，所以这里的值会跟着掉落物一起
     * 流到玩家手里（例如为了平衡给生物的 `Damage: 1`）。
     */
    @JvmField
    @SerialName("Override")
    val override: JsonObject? = null,
) {
    /** 解析并校验枪械物品；id 非法或不是 [GunItem] 时返回 `null` */
    fun gunItem(): GunItem? {
        val location = ResourceLocation.tryParse(id) ?: return null
        return BuiltInRegistries.ITEM.get(location) as? GunItem
    }

    object GunSpawnDataInstanceBuilder : StringInstanceBuilder<GunSpawnData> {
        override fun fromString(value: String) = GunSpawnData(id = value)
    }
}
