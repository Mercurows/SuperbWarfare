package com.atsuishio.superbwarfare.data

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.tools.toKxJson
import com.google.gson.JsonObject

// TODO 取代StringPropModifier
class JsonPropertyModifier<DATA : DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA>(
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
            update(DataLoader.GSON.fromJson(string, JsonObject::class.java))
        } catch (exception: Exception) {
            Mod.LOGGER.error("Failed to parse string prop modifier: {}", string, exception)
        }
    }

    override fun computeProperties(data: DATA, rawData: DEFAULT_DATA): DEFAULT_DATA {
        if (obj == null || obj!!.size() == 0) return rawData

        val dataJson = DataLoader.GSON.toJsonTree(rawData).getAsJsonObject()
        for (entry in obj!!.entrySet()) {
            dataJson.add(entry.key, entry.value)
        }

        return DataLoader.GSON.fromJson(dataJson, rawData!!.javaClass)
    }

    private val propsMap by lazy {
        props?.associateBy { it.serializationName } ?: emptyMap()
    }

    override fun modifyProperty(modifier: PMC<DATA, DEFAULT_DATA>) {
        val element = obj?.toKxJson() as? kotlinx.serialization.json.JsonObject ?: return

        for ((key, value) in element) {
            val prop = propsMap[key] ?: continue

            val deserialized = try {
                prop.deserialize(value)!!
            } catch (exception: Exception) {
                Mod.LOGGER.error("Failed to deserialize prop: {}", value, exception)
                continue
            }
            @Suppress("UNCHECKED_CAST")
            modifier[prop as Prop<DATA, DEFAULT_DATA, *, Any, *>] = deserialized
        }
    }
}
