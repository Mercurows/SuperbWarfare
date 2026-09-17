package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.IDBasedData
import com.atsuishio.superbwarfare.data.STOFactory
import com.atsuishio.superbwarfare.data.StringInstanceBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 弹射物信息（`Projectile` 字段 / `sbw/launchable` 数据集）。
 *
 * 注意 [itemId] 是**序列化字段**（JSON 键 `Type`），同时也是 [IDBasedData] 的 id：
 * 加载器会就地把数据包文件路径打戳进去（[IDBasedData.setId]），所以它必须留在类体里当 `var`，
 * 不能进主构造参数。这也是它和 `DefaultGunData.itemId`（`@Transient`）的区别。
 *
 * @param data 发射时用于改写弹射物 NBT 的不透明 JSON 数据（`@sbw:xxx` 占位符）
 */
@STOFactory(ProjectileInfo.ProjectileInfoInstanceBuilder::class)
@Serializable
data class ProjectileInfo(
    @JvmField
    @SerialName("Data")
    val data: JsonObject? = null,
) : IDBasedData<ProjectileInfo> {

    @JvmField
    @SerialName("Type")
    var itemId: String = "superbwarfare:projectile"

    override fun getId() = itemId

    override fun setId(id: String) {
        this.itemId = id
    }

    object ProjectileInfoInstanceBuilder : StringInstanceBuilder<ProjectileInfo> {
        override fun fromString(value: String) = ProjectileInfo().apply {
            this.itemId = value
        }
    }
}
