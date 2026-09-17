package com.atsuishio.superbwarfare.data.mob_guns

import com.atsuishio.superbwarfare.data.IDBasedData
import com.atsuishio.superbwarfare.data.ObjectToList
import com.atsuishio.superbwarfare.data.StringToObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 一种生物使用枪械的数据（`sbw/mob_guns/<生物id>.json`）。
 *
 * 注意 `Guns` 在原来的 Java 类里没有 `@SerializedName`，是靠 Gson 的 `UPPER_CAMEL_CASE` 策略
 * 把 `guns` 映射成 `"Guns"` 的；kotlinx 没有命名策略，所以这里必须显式写出来。
 */
@Serializable
data class DefaultMobGunData(
    /** 生物生成时携带枪械的概率 */
    @JvmField
    @SerialName("Probability")
    val probability: Double = 0.0,

    /** 生物使用枪械的行为权重 */
    @JvmField
    @SerialName("GoalWeight")
    val goalWeight: Int = 3,

    @JvmField
    @SerialName("Guns")
    val guns: ObjectToList<StringToObject<GunSpawnData>> = ObjectToList(),
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
