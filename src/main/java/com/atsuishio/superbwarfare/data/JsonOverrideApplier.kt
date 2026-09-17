package com.atsuishio.superbwarfare.data

import com.atsuishio.superbwarfare.Mod
import kotlinx.serialization.json.JsonObject

/**
 * 属性覆盖（`Override` 字段 / 枪械 NBT 里的属性覆写字符串）的统一入口。
 */
// TODO 取代StringPropModifier
class JsonOverrideApplier<DATA : DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA>(
    // TODO 实现VehicleProp后禁止该项为空
    val props: List<Prop<DATA, DEFAULT_DATA, *, *, *>>? = null
) : OldPropertyModifier<DATA, DEFAULT_DATA>, PropertyModifier<DATA, DEFAULT_DATA> {
    private var obj: JsonObject? = null
    private var str: String? = null

    fun update(`object`: JsonObject?) {
        this.obj = `object`
        this.str = null
    }

    fun update(string: String?) {
        if (string.isNullOrEmpty() || string == this.str) return
        this.str = string

        try {
            update(DataLoader.JSON.parseToJsonElement(string) as? JsonObject)
        } catch (exception: Exception) {
            Mod.LOGGER.error("Failed to parse string prop modifier: {}", string, exception)
        }
    }

    /**
     * 旧版属性覆盖路径：把 override 合并进整份数据对象再重新解析。
     *
     * 枪械已经不再走这里（[com.atsuishio.superbwarfare.data.gun.GunData] 用 PMC），
     * 目前只剩载具在用（[com.atsuishio.superbwarfare.data.vehicle.VehicleData.compute]）。
     */
    override fun computeProperties(data: DATA, rawData: DEFAULT_DATA): DEFAULT_DATA {
        val override = obj ?: return rawData
        if (override.isEmpty()) return rawData

        return DataCodec.mergeOverride(rawData!!, override)
    }

    private val propsMap by lazy {
        props?.associateBy { it.serializationName } ?: emptyMap()
    }

    override fun modifyProperty(modifier: PMC<DATA, DEFAULT_DATA>) {
        val element = obj ?: return

        for ((key, value) in element) {
            val prop = propsMap[key] ?: continue

            val deserialized = try {
                prop.deserialize(modifier.data, value)!!
            } catch (exception: Exception) {
                Mod.LOGGER.error("Failed to deserialize prop: {}", value, exception)
                continue
            }
            @Suppress("UNCHECKED_CAST")
            modifier[prop as Prop<DATA, DEFAULT_DATA, *, Any, *>] = deserialized
        }
    }
}
