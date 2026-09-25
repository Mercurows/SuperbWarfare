package com.atsuishio.superbwarfare.data.mob_guns

import com.atsuishio.superbwarfare.data.IDBasedData
import com.atsuishio.superbwarfare.data.SingleOrList
import com.atsuishio.superbwarfare.data.StringOrObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 一种生物使用枪械的数据（`sbw/mob_guns/<文件>.json`）。
 *
 * 注意 `Guns` 在原来的 Java 类里没有 `@SerializedName`，是靠 Gson 的 `UPPER_CAMEL_CASE` 策略
 * 把 `guns` 映射成 `"Guns"` 的；kotlinx 没有命名策略，所以这里必须显式写出来。
 */
@Serializable
data class DefaultMobGunData(
    /**
     * 这份数据适用的生物。支持实体 tag（`#minecraft:raiders`）与多个 id：
     * 一个文件可以覆盖一整类生物，不必每种生物抄一份 Guns。
     *
     * 留空时回退为「文件路径推导出的 id」——即 `data/<命名空间>/sbw/mob_guns/<路径>.json`
     * 对应 `<命名空间>:<路径>`。**原版生物请显式写 [entities]**：例如 `minecraft:zombie`
     * 需要把文件放在 `data/minecraft/sbw/mob_guns/` 下才推导得出来，写 Entities 则不受目录限制。
     */
    @JvmField
    @SerialName("Entities")
    val entities: SingleOrList<String> = SingleOrList(),

    /** 生物生成时携带枪械的概率 */
    @JvmField
    @SerialName("Probability")
    val probability: Double = 0.0,

    /**
     * 持枪 goal 的优先级，**数字越小越优先**（0 最高）。
     *
     * 与生物原有的 goal 抢同一个控制位（MOVE/LOOK）时，先跑的赢，所以注册时还会把这个值
     * 自动压到那些冲突 goal 之前，见 `MobGunData.goalPriority`。
     */
    @JvmField
    @SerialName("GoalPriority")
    val goalPriority: Int = 3,

    @JvmField
    @SerialName("Guns")
    val guns: SingleOrList<StringOrObject<GunSpawnData>> = SingleOrList(),
) : IDBasedData<DefaultMobGunData> {

    /** 数据集 id，由加载器按文件路径打戳，不参与序列化 */
    @Transient
    @kotlinx.serialization.Transient
    private var id: String = ""

    override fun getId(): String = this.id

    override fun setId(id: String) {
        this.id = id
    }
}
