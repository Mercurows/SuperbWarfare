package com.atsuishio.superbwarfare.data.drone_attachment

import com.atsuishio.superbwarfare.data.IDBasedData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.math.max

/**
 * 无人机挂载数据（`sbw/drone_attachments`）。
 *
 * [itemID] 是**序列化字段**（键 `Item`），同时也是 [IDBasedData] 的 id：加载器会就地把文件路径
 * 打戳进去（[IDBasedData.setId]），所以它必须留在类体里当 `var`（和 `ProjectileInfo.itemId` 同理）。
 * 也就是说 `Item` 的 JSON 值实际会被文件 id 覆盖，查找用的是 `CustomData.DRONE_ATTACHMENT[物品id]`。
 *
 * 几个 `xxxData()` / `scale()` 之类的函数保留了原来 Java 版的"回退到备用字段"语义。
 */
@Serializable
data class DroneAttachmentData(
    @SerialName("Entity")
    private val entity: String = "",

    @SerialName("DisplayEntity")
    private val displayEntity: String = "",

    @SerialName("DropEntity")
    private val dropEntity: String = "",

    @SerialName("DropPosition")
    private val dropPosition: FloatArray = floatArrayOf(0f, -0.09f, 0f),

    @SerialName("Data")
    private val data: JsonObject? = null,

    @SerialName("DisplayData")
    private val displayData: JsonObject? = null,

    @SerialName("DropData")
    private val dropData: JsonObject? = null,

    @SerialName("Count")
    private val count: Int = 1,

    @SerialName("IsKamikaze")
    val isKamikaze: Boolean = true,

    @SerialName("HitDamage")
    val hitDamage: Float = 0f,

    @SerialName("ExplosionDamage")
    val explosionDamage: Float = 0f,

    @SerialName("ExplosionRadius")
    val explosionRadius: Float = 0f,

    @SerialName("Scale")
    private val scale: FloatArray = floatArrayOf(1f, 1f, 1f),

    @SerialName("Offset")
    private val offset: FloatArray = floatArrayOf(0f, 0f, 0f),

    @SerialName("Rotation")
    private val rotation: FloatArray = floatArrayOf(0f, 0f, 0f),

    @SerialName("XLength")
    val xLength: Float = 0.1f,

    @SerialName("ZLength")
    val zLength: Float = 0.35f,

    @SerialName("TickCount")
    val tickCount: Int = -1,
) : IDBasedData<DroneAttachmentData> {

    @SerialName("Item")
    var itemID: String = ""

    override fun getId(): String = this.itemID

    override fun setId(id: String) {
        this.itemID = id
    }

    /** 要展示/生成的实体 id，[entity] 优先 */
    fun displayEntity(): String = if (entity.isNotEmpty()) entity else if (displayEntity.isEmpty()) dropEntity else displayEntity

    /** 要生成的实体 id，[entity] 优先 */
    fun dropEntity(): String = if (entity.isNotEmpty()) entity else if (dropEntity.isEmpty()) displayEntity else dropEntity

    fun dropPosition(): FloatArray =
        if (dropPosition.size < 3) floatArrayOf(0f, -0.09f, 0f) else dropPosition

    /** 神风模式下只投一个 */
    fun count(): Int = if (isKamikaze) 1 else max(1, count)

    fun scale(): FloatArray = if (scale.size < 3) floatArrayOf(1f, 1f, 1f) else scale

    fun offset(): FloatArray = if (offset.size < 3) floatArrayOf(0f, 0f, 0f) else offset

    fun rotation(): FloatArray = if (rotation.size < 3) floatArrayOf(0f, 0f, 0f) else rotation

    fun displayData(): JsonObject? =
        if (data != null) data else displayData ?: dropData

    fun dropData(): JsonObject? =
        if (data != null) data else dropData ?: displayData
}
